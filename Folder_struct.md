# Mnemocast Backend — Project Structure

Root repo focused on **one main service now** (Engine), but ready for:
- more services later (analytics, dashboard API, etc.)
- shared domain models
- infra & deployment

```text
mnemocast/
├── docs/
│   ├── 01-domain-model.md
│   ├── 02-api-spec.md
│   ├── 03-playlist-logic.md
│   ├── 04-events-and-budget.md
│   └── demo-script.md
│
├── backend/
│   ├── project/                # sbt project files (build properties, plugins)
│   ├── build.sbt
│   │
│   ├── modules/
│   │   ├── engine-domain/      # pure domain models & logic (no HTTP/DB)
│   │   │   ├── src/main/scala/mnemocast/engine/domain/
│   │   │   │   ├── model/      # Screen, Campaign, Creative, Targeting, Playlist, PlayEvent
│   │   │   │   └── services/   # pure services: PlaylistEngine, TargetingLogic, BudgetingLogic
│   │   │   └── src/test/scala/...
│   │   │
│   │   ├── engine-infra/       # persistence, config, external integrations
│   │   │   ├── src/main/scala/mnemocast/engine/infra/
│   │   │   │   ├── store/      # InMemoryScreenStore, EventStore, later DB impl
│   │   │   │   ├── config/     # loading app config (Typesafe config)
│   │   │   │   └── wiring/     # wire domain + infra (manual DI or simple modules)
│   │   │   └── src/test/scala/...
│   │   │
│   │   ├── engine-api/         # Pekko HTTP, routing, JSON formats
│   │   │   ├── src/main/scala/mnemocast/engine/api/
│   │   │   │   ├── json/       # spray-json or circe formats
│   │   │   │   ├── routes/     # ScreenRoutes, PlaylistRoutes, EventRoutes
│   │   │   │   └── HttpServer.scala  # boot Pekko HTTP server
│   │   │   └── src/test/scala/...
│   │   │
│   │   └── shared-kernel/      # (optional, future) shared types/util across services
│   │       ├── src/main/scala/mnemocast/shared/
│   │       └── src/test/scala/...
│   │
│   └── README.md
│
├── infra/
│   ├── docker/                 # Dockerfiles for engine-api (later)
│   ├── k8s/                    # Kubernetes manifests (future)
│   └── local-dev/              # docker-compose for DB, etc.
│
├── scripts/
│   ├── dev-run.sh              # run engine locally
│   ├── seed-demo-data.sh       # optional: calls APIs to seed screens/campaigns
│   └── health-check.sh         # simple health check pings
│
└── README.md
