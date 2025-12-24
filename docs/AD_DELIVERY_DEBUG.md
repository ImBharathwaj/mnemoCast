#  Ad Delivery Debugging Guide

## Issue: Server doesn't send ads to screen when screen requests for ad

### Fixed Issues

1. **Future Chaining Fix**
   - Changed `campaignIdFut.map` to `campaignIdFut.flatMap` to properly chain Futures
   - Ensures `Future[Option[ScreenClientAdResponse]]` is properly returned

2. **Enhanced Logging**
   - Added logging at each step of the ad delivery process
   - Logs when ad is requested, when ad is found, and when response is sent

### Debugging Steps

#### 1. Check Server Logs

When a screen requests an ad, you should see logs like:

```
[timestamp] [GET /api/v1/screens/{screenId}/ads/deliver] Request received from authenticated screen: {screenName}
[timestamp] [GET /api/v1/screens/{screenId}/ads/deliver] Requesting ad delivery...
[timestamp] [AdDelivery] Processing delivery request: {requestId} for screen: {screenId}
[timestamp] [AdDelivery] Total active ads: {count}
[timestamp] [AdDelivery] After targeting filter: {count} ads eligible.
[timestamp] [AdDelivery] After budget filter: {count} ads eligible.
[timestamp] [AdDelivery] After frequency cap filter: {count} ads eligible.
[timestamp] [AdDelivery] Selected ad: {adId}
[timestamp] [GET /api/v1/screens/{screenId}/ads/deliver] AdDeliveryService returned ad: {adId}
[timestamp] [GET /api/v1/screens/{screenId}/ads/deliver] Ad delivered: {adId}
[timestamp] [GET /api/v1/screens/{screenId}/ads/deliver] Successfully returning ad: {adId} ({title})
[timestamp] [GET /api/v1/screens/{screenId}/ads/deliver] Response: id={id}, type={type}, contentUrl={url}, duration={duration}
```

#### 2. Check Response Format

The response should be in this format:

```json
{
  "id": "ad-12345",
  "title": "Summer Sale 2025",
  "type": "image",
  "contentUrl": "http://localhost:9000/api/v1/media/creatives/summer-sale.jpg",
  "duration": 30,
  "startTime": "2025-12-19T10:00:00Z",
  "endTime": "2025-12-19T10:00:30Z",
  "priority": 1,
  "metadata": {
    "campaignId": "campaign-001",
    "targetAudience": "all"
  }
}
```

#### 3. Common Issues and Solutions

**Issue: No ads available**
- **Symptom:** Log shows "No ad available after processing"
- **Solution:** 
  - Check if ads exist in database: `SELECT * FROM ads WHERE is_active = true;`
  - Check if ads match targeting rules for the screen
  - Check if ads have budget remaining
  - Check if frequency caps are blocking ads

**Issue: Ad found but not returned**
- **Symptom:** Log shows "AdDeliveryService returned ad" but no response
- **Solution:**
  - Check if `adStore.getById` is finding the ad
  - Check if campaign lookup is failing
  - Check server logs for exceptions

**Issue: Response format error**
- **Symptom:** Client receives error or malformed JSON
- **Solution:**
  - Verify JSON codec is properly defined
  - Check if `type` field is serializing correctly (should be `"type"` in JSON)
  - Verify ISO 8601 timestamp format

**Issue: Authentication failure**
- **Symptom:** 403 Forbidden error
- **Solution:**
  - Verify screen ID and passkey are correct
  - Check `X-Screen-Id` and `X-Screen-Passkey` headers
  - Check screen authentication logs

### Testing the Endpoint

#### Using curl:

```bash
curl -X GET "http://localhost:8080/api/v1/screens/{screenId}/ads/deliver" \
  -H "X-Screen-Id: {screenId}" \
  -H "X-Screen-Passkey: {passkey}" \
  -v
```

#### Expected Response:

**Success (200 OK):**
```json
{
  "id": "ad-12345",
  "title": "Summer Sale 2025",
  "type": "image",
  "contentUrl": "http://localhost:9000/api/v1/media/creatives/summer-sale.jpg",
  "duration": 30,
  "startTime": "2025-12-19T10:00:00Z",
  "endTime": "2025-12-19T10:00:30Z",
  "priority": 1,
  "metadata": {
    "campaignId": "campaign-001",
    "targetAudience": "all"
  }
}
```

**No Content (204 No Content):**
```json
{
  "message": "No ad available for this screen at this time"
}
```

**Error (500 Internal Server Error):**
```json
{
  "error": "Internal server error: {error message}"
}
```

### Verification Checklist

- [ ] Screen is authenticated (check logs for authentication success)
- [ ] Ad delivery service is called (check logs for "Requesting ad delivery...")
- [ ] Ads exist in database and are active
- [ ] Ads match targeting rules for the screen
- [ ] Ads have budget remaining
- [ ] Frequency caps are not blocking ads
- [ ] Ad is found in store (check logs for "AdDeliveryService returned ad")
- [ ] Response is properly formatted (check logs for "Successfully returning ad")
- [ ] JSON serialization works (check response format)

### Next Steps

If ads are still not being sent:

1. **Check Database:**
   ```sql
   SELECT COUNT(*) FROM ads WHERE is_active = true;
   SELECT * FROM ads WHERE is_active = true LIMIT 5;
   ```

2. **Check Screen:**
   ```sql
   SELECT id, name, passkey FROM screens WHERE id = '{screenId}';
   ```

3. **Check Targeting:**
   ```sql
   SELECT * FROM targeting_rules WHERE ad_id IN (SELECT id FROM ads WHERE is_active = true);
   ```

4. **Enable Debug Logging:**
   - Check `AdDeliveryService` logs for filtering details
   - Check `TargetingService` logs for rule evaluation
   - Check `BudgetService` logs for budget checks
   - Check `FrequencyCapService` logs for frequency cap checks

---

**Status:**  Fixed Future chaining and added enhanced logging  
**Date:** 2025-12-19

