package mnemocast.engine.api.json

import com.github.pjfanning.pekkohttpcirce.FailFastCirceSupport

/**
  * JSON marshalling/unmarshalling support using Circe.
  *
  * We already have Circe Codecs in the domain model companions,
  * so this just mixes in the Pekko HTTP <-> Circe integration.
  */
trait JsonSupport extends FailFastCirceSupport
