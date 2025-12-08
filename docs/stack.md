
---

### What goes where (short version)

- **`engine-domain`**
  - Pure Scala: case classes & pure functions.
  - No Pekko, no DB, no HTTP here.
  - Examples:
    - `Screen`, `Campaign`, `Creative`, `Targeting`, `Playlist`, `PlayEvent`
    - `PlaylistEngine`, `TargetingLogic`, `BudgetService`

- **`engine-infra`**
  - How you **store & wire** stuff.
  - Examples:
    - `ScreenStore` in-memory / DB impl
    - `CampaignStore`, `EventStore`
    - App config, logging, wiring classes

- **`engine-api`**
  - Pekko HTTP server.
  - Examples:
    - Routes for `/screens`, `/playlist`, `/events`
    - JSON marshalling
    - `HttpServer` main entrypoint

- **`docs/`**
  - All the things we’re already planning:
    - Domain model, API spec, playlist logic, events & budget, demo script.

- **`infra/`**
  - Future scaling: docker, k8s, infra-as-code.
  - You don’t need to fill this now; just having the folder keeps you future-proof.

---

### Why this scales well

1. **Domain isolated**  
   You can later:
   - reuse `engine-domain` in another service (e.g., analytics, simulation)
   - test logic without HTTP/DB

2. **Infra & API separable**  
   You can:
   - swap in Postgres instead of in-memory stores without touching HTTP or domain
   - expose the same domain via gRPC or other protocols in future

3. **Multi-service ready**  
   In a year, you can add:
   - `analytics-service/`
   - `dashboard-service/`
   - `player-telemetry-service/`
   under `backend/modules/` without breaking the current structure.

---

### How to start practically (for Sprint 1)

For now, you can keep it **very light** inside this structure:

- Create the folders exactly as shown.
- In Sprint 1:
  - Most work will be in:
    - `engine-domain/model`
    - `engine-infra/store` (in-memory)
    - `engine-api/routes` + `HttpServer`
- No need to touch `shared-kernel`, `infra/docker`, `k8s` yet.

---

If you want, next I can:

- Give you a **minimal `README.md`** for `/backend` describing each module, or  
- Draft **`docs/01-domain-model.md`** in markdown so you can start implementing right away.
