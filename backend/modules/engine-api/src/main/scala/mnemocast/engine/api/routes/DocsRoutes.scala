package mnemocast.engine.api.routes

import org.apache.pekko.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import org.apache.pekko.http.scaladsl.server.Directives._
import org.apache.pekko.http.scaladsl.server.Route

import scala.io.Source

/**
  * API Documentation routes (Swagger UI).
  *
  * GET /api/docs - Swagger UI
  * GET /api/docs/openapi.yaml - OpenAPI specification
  */
class DocsRoutes {

  val routes: Route =
    pathPrefix("api" / "docs") {
      pathEndOrSingleSlash {
        get {
          complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, swaggerUIHtml))
        }
      } ~
      path("openapi.yaml") {
        get {
          val openApiSpec = loadOpenAPISpec()
          complete(HttpEntity(ContentTypes.`text/plain(UTF-8)`, openApiSpec))
        }
      } ~
      path("swagger-ui") {
        get {
          complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, swaggerUIHtml))
        }
      }
    }

  private def loadOpenAPISpec(): String = {
    try {
      val stream = getClass.getResourceAsStream("/openapi.yaml")
      if (stream != null) {
        Source.fromInputStream(stream).mkString
      } else {
        "# OpenAPI spec not found"
      }
    } catch {
      case e: Exception =>
        s"# Error loading OpenAPI spec: ${e.getMessage}"
    }
  }

  private val swaggerUIHtml: String =
    """
      |<!DOCTYPE html>
      |<html lang="en">
      |<head>
      |  <meta charset="UTF-8">
      |  <title>Mnemocast API Documentation</title>
      |  <link rel="stylesheet" type="text/css" href="https://unpkg.com/swagger-ui-dist@5.9.0/swagger-ui.css" />
      |  <style>
      |    html {
      |      box-sizing: border-box;
      |      overflow: -moz-scrollbars-vertical;
      |      overflow-y: scroll;
      |    }
      |    *, *:before, *:after {
      |      box-sizing: inherit;
      |    }
      |    body {
      |      margin:0;
      |      background: #fafafa;
      |    }
      |  </style>
      |</head>
      |<body>
      |  <div id="swagger-ui"></div>
      |  <script src="https://unpkg.com/swagger-ui-dist@5.9.0/swagger-ui-bundle.js"></script>
      |  <script src="https://unpkg.com/swagger-ui-dist@5.9.0/swagger-ui-standalone-preset.js"></script>
      |  <script>
      |    window.onload = function() {
      |      const ui = SwaggerUIBundle({
      |        url: '/api/docs/openapi.yaml',
      |        dom_id: '#swagger-ui',
      |        deepLinking: true,
      |        presets: [
      |          SwaggerUIBundle.presets.apis,
      |          SwaggerUIStandalonePreset
      |        ],
      |        plugins: [
      |          SwaggerUIBundle.plugins.DownloadUrl
      |        ],
      |        layout: "StandaloneLayout",
      |        validatorUrl: null
      |      });
      |    };
      |  </script>
      |</body>
      |</html>
    """.stripMargin
}

