package mnemocast.engine.api.routes

import java.time.Instant
import java.util.UUID

import scala.concurrent.{ExecutionContext, Future}

import org.apache.pekko.http.scaladsl.model.StatusCodes
import org.apache.pekko.http.scaladsl.server.Directives._
import org.apache.pekko.http.scaladsl.server.Route

import mnemocast.engine.api.json.JsonSupport
import mnemocast.engine.api.middleware.ScreenAuthMiddleware
import mnemocast.engine.domain.model.{DeliveryRequest, DeliveryResponse, Screen}
import mnemocast.engine.infra.services.AdDeliveryService
import mnemocast.engine.infra.store.{AdStore, EventStore, ScreenStore}

/**
  * Screen-specific ad delivery routes.
  *
  * These endpoints are optimized for digital screens (OOH displays) and automatically
  * populate screen context from the screen registry.
  *
  * GET /api/v1/screens/{screenId}/ads/deliver - Get a single ad for a screen
  */
class ScreenAdRoutes(
  adDeliveryService: AdDeliveryService,
  screenStore: ScreenStore,
  adStore: AdStore,
  eventStore: EventStore,
  campaignStore: Option[mnemocast.engine.infra.store.CampaignStore] = None
)(implicit ec: ExecutionContext)
    extends JsonSupport {

  /**
    * Screen-specific ad delivery endpoints.
    *
    * Automatically fetches screen details from the registry and populates
    * the delivery request with screen context (location, tags, classification).
    */
  val routes: Route =
    pathPrefix("api" / "v1" / "screens") {
      concat(
        // Single ad delivery (requires screen authentication)
        path(Segment / "ads" / "deliver") { screenId =>
          get {
            ScreenAuthMiddleware.authenticate(screenStore).apply { screen =>
              // Verify the screenId in path matches authenticated screen
              if (screen.id != screenId) {
                complete(StatusCodes.Forbidden, Map("error" -> "Screen ID mismatch"))
              } else {
                parameters("durationSeconds".as[Int].?) { durationSecondsOpt =>
                  extractClientIP { ip =>
                    val timestamp = java.time.LocalDateTime.now()
                    println(s"[$timestamp] [GET /api/v1/screens/$screenId/ads/deliver] Request received from authenticated screen: ${screen.name}")
                    
                    // Use authenticated screen directly (no need to fetch again)
                    println(s"[$timestamp] [GET /api/v1/screens/$screenId/ads/deliver] Screen authenticated: ${screen.name}")
                    
                    // Build delivery request with auto-populated screen context
                    val request = DeliveryRequest(
                      requestId = UUID.randomUUID().toString,
                      deviceId = Some(screenId),
                      userId = None,
                      appId = None,
                      ip = Some(ip.toOption.map(_.getHostAddress).getOrElse(ip.toString())),
                      country = screen.location.country,
                      platform = None, // Screens don't have a platform
                      screenId = Some(screenId),
                      city = screen.location.city,
                      area = screen.location.area,
                      venueType = screen.location.venueType,
                      screenTags = screen.tags,
                      timezone = screen.location.timezone,
                      screenClassification = Some(screen.classification),
                      timestamp = Instant.now()
                    )
                    
                    // Request ad delivery and fetch full ad details for metadata
                    println(s"[$timestamp] [GET /api/v1/screens/$screenId/ads/deliver] Requesting ad delivery...")
                    val futureResponse = adDeliveryService.deliver(request).flatMap {
                      case Some(response) =>
                        println(s"[$timestamp] [GET /api/v1/screens/$screenId/ads/deliver] AdDeliveryService returned ad: ${response.adId}")
                        // Fetch full ad details for metadata
                        adStore.getById(response.adId).flatMap {
                          case Some(ad) =>
                            println(s"[$timestamp] [GET /api/v1/screens/$screenId/ads/deliver] Ad delivered: ${response.adId}")
                            // Determine creative type from URL
                            val creativeType = getCreativeTypeFromUrl(response.creativeUrl)
                            val duration = ad.durationSeconds.orElse(durationSecondsOpt)
                            
                            // Try to find campaign ID by matching advertiser ID
                            val campaignIdFut = campaignStore match {
                              case Some(store) =>
                                store.listActive().map { campaigns =>
                                  // Find first active campaign with matching advertiserId
                                  campaigns.find { campaign =>
                                    campaign.advertiserId == ad.advertiserId && campaign.status == "active"
                                  }.map(_.id).getOrElse("unknown")
                                }
                              case None =>
                                Future.successful("unknown")
                            }
                            
                            // Use flatMap to properly chain the Future
                            campaignIdFut.flatMap { campaignId =>
                              // Generate title from ad ID or use advertiser
                              val title = generateAdTitle(ad.id, ad.advertiserId)
                              
                              // Calculate start and end times
                              val now = java.time.Instant.now()
                              val startTime = now
                              val endTime = duration match {
                                case Some(dur) => now.plusSeconds(dur)
                                case None => now.plusSeconds(30) // Default 30 seconds
                              }
                              
                              // Determine target audience from targeting rules
                              val targetAudience = if (ad.targetingRules.isEmpty) {
                                "all"
                              } else {
                                // Summarize targeting rules
                                val locations = ad.targetingRules.filter(_.key == "city").map(_.value).mkString(", ")
                                if (locations.nonEmpty) locations else "targeted"
                              }
                              
                              // Return screen client compatible format wrapped in Future
                              Future.successful(Some(ScreenClientAdResponse(
                                id = ad.id,
                                title = title,
                                `type` = creativeType,
                                contentUrl = response.creativeUrl,
                                duration = duration,
                                startTime = startTime.toString, // ISO 8601 format
                                endTime = endTime.toString,     // ISO 8601 format
                                priority = ad.weight,
                                metadata = ScreenClientAdMetadata(
                                  campaignId = campaignId,
                                  targetAudience = targetAudience
                                )
                              )))
                            }
                          case None =>
                            println(s"[$timestamp] [GET /api/v1/screens/$screenId/ads/deliver] Ad delivered but not found in store: ${response.adId}")
                            // Fallback response
                            val now = java.time.Instant.now()
                            val duration = durationSecondsOpt.getOrElse(30)
                            Future.successful(Some(ScreenClientAdResponse(
                              id = response.adId,
                              title = generateAdTitle(response.adId, "unknown"),
                              `type` = getCreativeTypeFromUrl(response.creativeUrl),
                              contentUrl = response.creativeUrl,
                              duration = durationSecondsOpt,
                              startTime = now.toString,
                              endTime = now.plusSeconds(duration).toString,
                              priority = 1,
                              metadata = ScreenClientAdMetadata(
                                campaignId = "unknown",
                                targetAudience = "all"
                              )
                            )))
                        }
                      case None =>
                        println(s"[$timestamp] [GET /api/v1/screens/$screenId/ads/deliver] No ad available")
                        Future.successful(None)
                    }
                
                    onComplete(futureResponse) {
                      case scala.util.Success(Some(response)) =>
                        println(s"[$timestamp] [GET /api/v1/screens/$screenId/ads/deliver] Successfully returning ad: ${response.id} (${response.title})")
                        println(s"[$timestamp] [GET /api/v1/screens/$screenId/ads/deliver] Response: id=${response.id}, type=${response.`type`}, contentUrl=${response.contentUrl}, duration=${response.duration}")
                        complete(response)
                      case scala.util.Success(None) =>
                        println(s"[$timestamp] [GET /api/v1/screens/$screenId/ads/deliver] No ad available after processing")
                        complete(StatusCodes.NoContent, Map("message" -> "No ad available for this screen at this time"))
                      case scala.util.Failure(ex) =>
                        println(s"[$timestamp] [GET /api/v1/screens/$screenId/ads/deliver] Internal error: ${ex.getMessage}")
                        ex.printStackTrace()
                        complete(StatusCodes.InternalServerError, Map("error" -> s"Internal server error: ${ex.getMessage}"))
                    }
                  }
                }
              }
            }
          }
        },
        // Batch ad delivery (requires screen authentication)
        path(Segment / "ads" / "batch") { screenId =>
          get {
            ScreenAuthMiddleware.authenticate(screenStore).apply { screen =>
              // Verify the screenId in path matches authenticated screen
              if (screen.id != screenId) {
                complete(StatusCodes.Forbidden, Map("error" -> "Screen ID mismatch"))
              } else {
                parameters(
                  "count".as[Int].?(10),
                  "durationMinutes".as[Int].?
                ) { (count, durationMinutesOpt) =>
                  extractClientIP { ip =>
                    val timestamp = java.time.LocalDateTime.now()
                    println(s"[$timestamp] [GET /api/v1/screens/$screenId/ads/batch] Request received from authenticated screen: ${screen.name}, count=$count")
                    
                    // Validate count
                    if (count <= 0 || count > 50) {
                      complete(StatusCodes.BadRequest, Map("error" -> "Count must be between 1 and 50"))
                    } else {
                      // Use authenticated screen directly
                      println(s"[$timestamp] [GET /api/v1/screens/$screenId/ads/batch] Screen authenticated: ${screen.name}")
                      
                      // Build base delivery request with auto-populated screen context
                      val baseRequest = DeliveryRequest(
                        requestId = UUID.randomUUID().toString,
                        deviceId = Some(screenId),
                        userId = None,
                        appId = None,
                        ip = Some(ip.toOption.map(_.getHostAddress).getOrElse(ip.toString())),
                        country = screen.location.country,
                        platform = None,
                        screenId = Some(screenId),
                        city = screen.location.city,
                        area = screen.location.area,
                        venueType = screen.location.venueType,
                        screenTags = screen.tags,
                        timezone = screen.location.timezone,
                        screenClassification = Some(screen.classification),
                        timestamp = Instant.now()
                      )
                      
                      // Fetch multiple ads with deduplication
                      println(s"[$timestamp] [GET /api/v1/screens/$screenId/ads/batch] Fetching $count ads...")
                      val futureBatch = fetchBatchAdsForClient(baseRequest, count, Set.empty, campaignStore).map { ads =>
                        val now = java.time.Instant.now()
                        println(s"[$timestamp] [GET /api/v1/screens/$screenId/ads/batch] Fetched ${ads.length} ads")
                        BatchAdResponse(
                          ads = ads,
                          updatedAt = now.toString // ISO 8601 format
                        )
                      }
                      
                      onComplete(futureBatch) {
                        case scala.util.Success(batch) =>
                          println(s"[$timestamp] [GET /api/v1/screens/$screenId/ads/batch] Returning ${batch.ads.length} ads")
                          if (batch.ads.isEmpty) {
                            println(s"[$timestamp] [GET /api/v1/screens/$screenId/ads/batch] No ads available, returning empty list")
                            // Return empty list instead of NoContent
                            complete(BatchAdResponse(ads = List.empty, updatedAt = java.time.Instant.now().toString))
                          } else {
                            complete(batch)
                          }
                        case scala.util.Failure(ex) =>
                          println(s"[$timestamp] [GET /api/v1/screens/$screenId/ads/batch] Internal error: ${ex.getMessage}")
                          ex.printStackTrace()
                          complete(StatusCodes.InternalServerError, Map("error" -> s"Internal server error: ${ex.getMessage}"))
                      }
                    }
                  }
                }
              }
            }
          }
        },
        // Screen ad preferences endpoint (requires screen authentication)
        path(Segment / "ads" / "preferences") { screenId =>
          get {
            ScreenAuthMiddleware.authenticate(screenStore).apply { screen =>
              // Verify the screenId in path matches authenticated screen
              if (screen.id != screenId) {
                complete(StatusCodes.Forbidden, Map("error" -> "Screen ID mismatch"))
              } else {
                val timestamp = java.time.LocalDateTime.now()
                println(s"[$timestamp] [GET /api/v1/screens/$screenId/ads/preferences] Request received from authenticated screen: ${screen.name}")
                
                // Use authenticated screen directly
                // Get all active ads to determine supported formats
                val futurePreferences = adStore.listActive().map { ads =>
                  // Extract supported formats from creative URLs
                  val supportedFormats = ads
                    .map(_.creativeUrl.toLowerCase)
                    .flatMap { url =>
                      if (url.contains(".mp4") || url.contains(".webm") || url.contains(".mov")) Some("video")
                      else if (url.contains(".jpg") || url.contains(".jpeg") || url.contains(".png") || url.contains(".gif") || url.contains(".webp")) Some("image")
                      else None
                    }
                    .distinct
                  
                  // Extract duration constraints from ads
                  val durations = ads.flatMap(_.durationSeconds).distinct.sorted
                  val maxDuration = durations.maxOption.getOrElse(60)
                  val minDuration = durations.minOption.getOrElse(5)
                  
                  // Extract targeting rules that match this screen
                  val matchingTargetingRules = ads.flatMap { ad =>
                    ad.targetingRules.filter { rule =>
                      rule.key match {
                        case "city" => screen.location.city.contains(rule.value)
                        case "area" => screen.location.area.contains(rule.value)
                        case "venueType" => screen.location.venueType.contains(rule.value)
                        case "country" => screen.location.country.contains(rule.value)
                        case "tags" => rule.value.split(",").exists(tag => screen.tags.contains(tag.trim))
                        case _ => false
                      }
                    }
                  }.distinct
                  
                  ScreenAdPreferencesResponse(
                    screenId = screen.id,
                    screenName = screen.name,
                    supportedFormats = supportedFormats,
                    maxDurationSeconds = maxDuration,
                    minDurationSeconds = minDuration,
                    isAudible = screen.isAudible,
                    screenWidth = screen.width,
                    screenHeight = screen.height,
                    preferredCategories = screen.tags, // Use screen tags as preferred categories
                    blockedCategories = List.empty, // Could be enhanced with screen metadata
                    targetingRules = ScreenTargetingRules(
                      city = screen.location.city,
                      area = screen.location.area,
                      venueType = screen.location.venueType,
                      country = screen.location.country,
                      tags = screen.tags,
                      timezone = screen.location.timezone
                    ),
                    classification = screen.classification
                  )
                }
                
                onComplete(futurePreferences) {
                  case scala.util.Success(preferences) =>
                    println(s"[$timestamp] [GET /api/v1/screens/$screenId/ads/preferences] Success")
                    complete(preferences)
                  case scala.util.Failure(ex) =>
                    println(s"[$timestamp] [GET /api/v1/screens/$screenId/ads/preferences] Error: ${ex.getMessage}")
                    ex.printStackTrace()
                    complete(StatusCodes.InternalServerError, Map("error" -> s"Internal server error: ${ex.getMessage}"))
                }
              }
            }
          }
        },
        // Screen ad history endpoint (requires screen authentication)
        path(Segment / "ads" / "history") { screenId =>
          get {
            ScreenAuthMiddleware.authenticate(screenStore).apply { screen =>
              // Verify the screenId in path matches authenticated screen
              if (screen.id != screenId) {
                complete(StatusCodes.Forbidden, Map("error" -> "Screen ID mismatch"))
              } else {
                parameters(
                  "limit".as[Int].?(50),
                  "since".?
                ) { (limit, sinceOpt) =>
                  val timestamp = java.time.LocalDateTime.now()
                  println(s"[$timestamp] [GET /api/v1/screens/$screenId/ads/history] Request received from authenticated screen: ${screen.name}, limit=$limit")
                  
                  // Validate limit
                  if (limit <= 0 || limit > 500) {
                    complete(StatusCodes.BadRequest, Map("error" -> "Limit must be between 1 and 500"))
                  } else {
                    // Use authenticated screen directly
                    println(s"[$timestamp] [GET /api/v1/screens/$screenId/ads/history] Screen authenticated: ${screen.name}")
                    
                    // Parse since timestamp if provided
                    val sinceInstant = sinceOpt.flatMap { sinceStr =>
                      try {
                        Some(java.time.Instant.parse(sinceStr))
                      } catch {
                        case _: Exception => None
                      }
                    }
                    
                    // Get all active ads to find events
                    val futureHistory = adStore.listActive().flatMap { ads =>
                      // For each ad, get recent events and filter by screenId (deviceId)
                      val eventFutures = ads.map { ad =>
                        eventStore.findByAdId(ad.id, limit * 2) // Get more to filter
                          .map(_.filter { event =>
                            // Filter events that match this screen (deviceId = screenId)
                            event.metadata.get("deviceId").contains(screenId) ||
                            event.metadata.get("screenId").contains(screenId)
                          })
                      }
                      
                      Future.sequence(eventFutures).map { eventLists =>
                        val allEvents = eventLists.flatten
                          .filter { event =>
                            // Additional filtering by since timestamp if provided
                            sinceInstant match {
                              case Some(since) => event.occurredAt.isAfter(since) || event.occurredAt.equals(since)
                              case None => true
                            }
                          }
                          .sortBy(_.occurredAt)(Ordering[java.time.Instant].reverse) // Most recent first
                          .take(limit)
                        
                        ScreenAdHistoryResponse(
                          screenId = screen.id,
                          screenName = screen.name,
                          ads = allEvents.map { event =>
                            ScreenAdHistoryItem(
                              adId = event.adId,
                              requestId = event.requestId,
                              eventType = event.eventType,
                              servedAt = event.occurredAt,
                              impressionTracked = event.eventType == "impression"
                            )
                          },
                          total = allEvents.length,
                          limit = limit,
                          since = sinceInstant
                        )
                      }
                    }
                    
                    onComplete(futureHistory) {
                      case scala.util.Success(history) =>
                        println(s"[$timestamp] [GET /api/v1/screens/$screenId/ads/history] Success: ${history.ads.length} events")
                        complete(history)
                      case scala.util.Failure(ex) =>
                        println(s"[$timestamp] [GET /api/v1/screens/$screenId/ads/history] Error: ${ex.getMessage}")
                        ex.printStackTrace()
                        complete(StatusCodes.InternalServerError, Map("error" -> s"Internal server error: ${ex.getMessage}"))
                    }
                  }
                }
              }
            }
          }
        }
      )
    }
  
  /**
    * Recursively fetch multiple ads with deduplication for screen client format.
    * Returns ads in ScreenClientAdResponse format.
    */
  private def fetchBatchAdsForClient(
    baseRequest: DeliveryRequest,
    remaining: Int,
    seenAdIds: Set[String],
    campaignStoreOpt: Option[mnemocast.engine.infra.store.CampaignStore],
    maxAttempts: Int = 100
  ): Future[List[ScreenClientAdResponse]] = {
    if (remaining <= 0 || maxAttempts <= 0) {
      Future.successful(List.empty)
    } else {
      val request = baseRequest.copy(requestId = UUID.randomUUID().toString)
      
      adDeliveryService.deliver(request).flatMap {
        case Some(response) if !seenAdIds.contains(response.adId) =>
          adStore.getById(response.adId).flatMap {
            case Some(ad) =>
              val creativeType = getCreativeTypeFromUrl(response.creativeUrl)
              val duration = ad.durationSeconds
              
              val campaignIdFut = campaignStoreOpt match {
                case Some(store) =>
                  store.listActive().map { campaigns =>
                    campaigns.find { campaign =>
                      campaign.advertiserId == ad.advertiserId && campaign.status == "active"
                    }.map(_.id).getOrElse("unknown")
                  }
                case None =>
                  Future.successful("unknown")
              }
              
              campaignIdFut.flatMap { campaignId =>
                val title = generateAdTitle(ad.id, ad.advertiserId)
                val now = java.time.Instant.now()
                val endTime = duration match {
                  case Some(dur) => now.plusSeconds(dur)
                  case None => now.plusSeconds(30)
                }
                
                val targetAudience = if (ad.targetingRules.isEmpty) {
                  "all"
                } else {
                  val locations = ad.targetingRules.filter(_.key == "city").map(_.value).mkString(", ")
                  if (locations.nonEmpty) locations else "targeted"
                }
                
                Future.successful(ScreenClientAdResponse(
                  id = ad.id,
                  title = title,
                  `type` = creativeType,
                  contentUrl = response.creativeUrl,
                  duration = duration,
                  startTime = now.toString,
                  endTime = endTime.toString,
                  priority = ad.weight,
                  metadata = ScreenClientAdMetadata(
                    campaignId = campaignId,
                    targetAudience = targetAudience
                  )
                ))
              }
            case None =>
              val creativeType = getCreativeTypeFromUrl(response.creativeUrl)
              val now = java.time.Instant.now()
              val duration = 30
              Future.successful(ScreenClientAdResponse(
                id = response.adId,
                title = generateAdTitle(response.adId, "unknown"),
                `type` = creativeType,
                contentUrl = response.creativeUrl,
                duration = Some(duration),
                startTime = now.toString,
                endTime = now.plusSeconds(duration).toString,
                priority = 1,
                metadata = ScreenClientAdMetadata(
                  campaignId = "unknown",
                  targetAudience = "all"
                )
              ))
          }.flatMap { item =>
            fetchBatchAdsForClient(baseRequest, remaining - 1, seenAdIds + response.adId, campaignStoreOpt, maxAttempts - 1)
              .map(item :: _)
          }
        case Some(_) =>
          fetchBatchAdsForClient(baseRequest, remaining, seenAdIds, campaignStoreOpt, maxAttempts - 1)
        case None =>
          Future.successful(List.empty)
      }
    }
  }
  
  /**
    * Recursively fetch multiple ads with deduplication (legacy format).
    * Stops when we have enough ads or no more ads are available.
    */
  private def fetchBatchAds(
    baseRequest: DeliveryRequest,
    remaining: Int,
    seenAdIds: Set[String],
    maxAttempts: Int = 100
  ): Future[List[BatchAdItem]] = {
    if (remaining <= 0 || maxAttempts <= 0) {
      Future.successful(List.empty)
    } else {
      // Create a new request with unique requestId for each attempt
      val request = baseRequest.copy(requestId = UUID.randomUUID().toString)
      
      adDeliveryService.deliver(request).flatMap {
        case Some(response) if !seenAdIds.contains(response.adId) =>
          // Fetch full ad details for metadata
          adStore.getById(response.adId).flatMap {
            case Some(ad) =>
              val creativeType = getCreativeTypeFromUrl(response.creativeUrl)
              val item = BatchAdItem(
                adId = response.adId,
                advertiserId = ad.advertiserId,
                creativeUrl = response.creativeUrl,
                creativeType = creativeType,
                targetUrl = response.targetUrl,
                impressionTrackingUrl = response.impressionTrackingUrl,
                durationSeconds = ad.durationSeconds,
                weight = ad.weight,
                metadata = Map(
                  "maxPlays" -> ad.maxPlays.map(_.toString).getOrElse("unlimited"),
                  "dailyLimit" -> ad.dailyLimit.map(_.toString).getOrElse("unlimited"),
                  "hourlyLimit" -> ad.hourlyLimit.map(_.toString).getOrElse("unlimited"),
                  "maxImpressionsPerDevice" -> ad.maxImpressionsPerDevice.map(_.toString).getOrElse("unlimited"),
                  "frequencyCapWindowHours" -> ad.frequencyCapWindowHours.map(_.toString).getOrElse("none")
                ),
                order = seenAdIds.size
              )
              fetchBatchAds(baseRequest, remaining - 1, seenAdIds + response.adId, maxAttempts - 1)
                .map(item :: _)
            case None =>
              // Fallback if ad not found
              val creativeType = getCreativeTypeFromUrl(response.creativeUrl)
              val item = BatchAdItem(
                adId = response.adId,
                advertiserId = "unknown",
                creativeUrl = response.creativeUrl,
                creativeType = creativeType,
                targetUrl = response.targetUrl,
                impressionTrackingUrl = response.impressionTrackingUrl,
                durationSeconds = None,
                weight = 1,
                metadata = Map.empty,
                order = seenAdIds.size
              )
              fetchBatchAds(baseRequest, remaining - 1, seenAdIds + response.adId, maxAttempts - 1)
                .map(item :: _)
          }
        case Some(_) =>
          // Duplicate ad, try again
          fetchBatchAds(baseRequest, remaining, seenAdIds, maxAttempts - 1)
        case None =>
          // No more ads available
          Future.successful(List.empty)
      }
    }
  }
  
  /**
    * Enhanced response model for screen-specific ad delivery.
    * Includes screen metadata for better client-side handling.
    */
  /**
    * Screen client compatible ad response format.
    * Matches the exact JSON structure expected by screen clients.
    */
  case class ScreenClientAdResponse(
    id: String,                        // Ad ID
    title: String,                     // Ad title/name
    `type`: String,                    // Creative type: "image" or "video"
    contentUrl: String,                // URL to download creative
    duration: Option[Int],             // Duration in seconds
    startTime: String,                 // ISO 8601 timestamp when ad should start
    endTime: String,                   // ISO 8601 timestamp when ad should end
    priority: Int,                     // Ad priority/weight
    metadata: ScreenClientAdMetadata   // Ad metadata
  )
  
  case class ScreenClientAdMetadata(
    campaignId: String,                // Campaign ID (or "unknown" if not linked)
    targetAudience: String              // Target audience description
  )
  
  implicit val screenClientAdMetadataCodec: io.circe.Codec[ScreenClientAdMetadata] =
    io.circe.generic.semiauto.deriveCodec[ScreenClientAdMetadata]
  
  implicit val screenClientAdResponseCodec: io.circe.Codec[ScreenClientAdResponse] =
    io.circe.generic.semiauto.deriveCodec[ScreenClientAdResponse]
  
  /**
    * Enhanced ad response for screen clients (legacy format - kept for backward compatibility).
    * Includes full ad metadata and creative download information.
    */
  case class EnhancedScreenAdResponse(
    // Request tracking
    requestId: String,
    
    // Ad identification
    adId: String,
    advertiserId: String,
    
    // Creative download information
    creativeUrl: String,              // Direct URL to download creative
    creativeType: String,              // "image", "video", or "unknown"
    
    // Ad metadata
    targetUrl: Option[String],        // Click-through URL
    impressionTrackingUrl: Option[String], // URL to call when ad is displayed
    durationSeconds: Option[Int],      // Ad duration in seconds
    weight: Int,                       // Ad weight/priority
    metadata: Map[String, String],     // Additional ad metadata (budget, frequency cap, etc.)
    
    // Screen information
    screenId: String,
    screenName: String,
    screenClassification: Int,
    screenWidth: Option[Int],
    screenHeight: Option[Int],
    isAudible: Boolean
  )
  
  /**
    * Helper to determine creative type from URL.
    */
  private def getCreativeTypeFromUrl(url: String): String = {
    val lowerUrl = url.toLowerCase
    if (lowerUrl.endsWith(".mp4") || lowerUrl.endsWith(".webm") || lowerUrl.endsWith(".mov") || lowerUrl.endsWith(".avi")) {
      "video"
    } else if (lowerUrl.endsWith(".jpg") || lowerUrl.endsWith(".jpeg") || lowerUrl.endsWith(".png") || lowerUrl.endsWith(".gif") || lowerUrl.endsWith(".webp")) {
      "image"
    } else {
      "unknown"
    }
  }
  
  /**
    * Generate ad title from ad ID or advertiser.
    */
  private def generateAdTitle(adId: String, advertiserId: String): String = {
    // Try to extract meaningful name from ad ID
    // e.g., "ad-summer-sale-001" -> "Summer Sale"
    val parts = adId.split("-")
    if (parts.length > 1) {
      // Capitalize and join parts (skip "ad" prefix)
      parts.drop(1).take(parts.length - 2).map(_.capitalize).mkString(" ")
    } else {
      // Fallback to advertiser ID
      advertiserId.replace("advertiser-", "").capitalize + " Ad"
    }
  }
  
  // JSON codec for enhanced response
  implicit val enhancedScreenAdResponseCodec: io.circe.Codec[EnhancedScreenAdResponse] =
    io.circe.generic.semiauto.deriveCodec[EnhancedScreenAdResponse]
  
  /**
    * Batch ad delivery response model for screen clients.
    * Matches the expected format: { "ads": [...], "updatedAt": "..." }
    */
  case class BatchAdResponse(
    ads: List[ScreenClientAdResponse],  // List of ads in screen client format
    updatedAt: String                   // ISO 8601 timestamp
  )
  
  /**
    * Legacy batch ad delivery response model (kept for backward compatibility).
    */
  case class LegacyBatchAdResponse(
    screenId: String,
    screenName: String,
    ads: List[ScreenClientAdResponse],
    totalDurationSeconds: Int,
    requestedCount: Int,
    actualCount: Int,
    requestedDurationMinutes: Option[Int]
  )
  
  implicit val batchAdResponseCodec: io.circe.Codec[BatchAdResponse] =
    io.circe.generic.semiauto.deriveCodec[BatchAdResponse]
  
  implicit val legacyBatchAdResponseCodec: io.circe.Codec[LegacyBatchAdResponse] =
    io.circe.generic.semiauto.deriveCodec[LegacyBatchAdResponse]
  
  /**
    * Single ad item in batch response.
    * Includes full ad metadata and creative download information.
    */
  case class BatchAdItem(
    // Ad identification
    adId: String,
    advertiserId: String,
    
    // Creative download information
    creativeUrl: String,              // Direct URL to download creative
    creativeType: String,              // "image", "video", or "unknown"
    
    // Ad metadata
    targetUrl: Option[String],        // Click-through URL
    impressionTrackingUrl: Option[String], // URL to call when ad is displayed
    durationSeconds: Option[Int],      // Ad duration in seconds
    weight: Int,                       // Ad weight/priority
    metadata: Map[String, String],     // Additional ad metadata (budget, frequency cap, etc.)
    
    // Playlist ordering
    order: Int                         // Order in playlist (0-based)
  )
  
  implicit val batchAdItemCodec: io.circe.Codec[BatchAdItem] =
    io.circe.generic.semiauto.deriveCodec[BatchAdItem]
  
  /**
    * Screen ad preferences response model.
    */
  case class ScreenAdPreferencesResponse(
    screenId: String,
    screenName: String,
    supportedFormats: List[String],
    maxDurationSeconds: Int,
    minDurationSeconds: Int,
    isAudible: Boolean,
    screenWidth: Option[Int],
    screenHeight: Option[Int],
    preferredCategories: List[String],
    blockedCategories: List[String],
    targetingRules: ScreenTargetingRules,
    classification: Int
  )
  
  implicit val screenAdPreferencesResponseCodec: io.circe.Codec[ScreenAdPreferencesResponse] =
    io.circe.generic.semiauto.deriveCodec[ScreenAdPreferencesResponse]
  
  case class ScreenTargetingRules(
    city: Option[String],
    area: Option[String],
    venueType: Option[String],
    country: Option[String],
    tags: List[String],
    timezone: Option[String]
  )
  
  implicit val screenTargetingRulesCodec: io.circe.Codec[ScreenTargetingRules] =
    io.circe.generic.semiauto.deriveCodec[ScreenTargetingRules]
  
  /**
    * Screen ad history response model.
    */
  case class ScreenAdHistoryResponse(
    screenId: String,
    screenName: String,
    ads: List[ScreenAdHistoryItem],
    total: Int,
    limit: Int,
    since: Option[java.time.Instant]
  )
  
  implicit val screenAdHistoryResponseCodec: io.circe.Codec[ScreenAdHistoryResponse] =
    io.circe.generic.semiauto.deriveCodec[ScreenAdHistoryResponse]
  
  case class ScreenAdHistoryItem(
    adId: String,
    requestId: String,
    eventType: String,
    servedAt: java.time.Instant,
    impressionTracked: Boolean
  )
  
  implicit val screenAdHistoryItemCodec: io.circe.Codec[ScreenAdHistoryItem] =
    io.circe.generic.semiauto.deriveCodec[ScreenAdHistoryItem]
}

