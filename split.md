# 🖥️ Multi-System Work Split for Mnemocast (Solo Dev + Cursor)

Goal:
- Use **two systems in parallel**
- Each system runs its own workstream
- No hard dependency (you don’t NEED both on to make progress)
- They only connect via **HTTP APIs + shared docs**

---

## 🧱 High-Level Split

- **System A (Engine Machine)** → Core Ad Serving Engine
- **System B (Tooling & Simulation Machine)** → Test client, simulators, docs, future player experiments

Think of it like:
- System A = **Brain**
- System B = **Observer + Tester + Future Eyes**

---

## 🖥 System A — Core Engine Development (Backend)

**Repository:** `mnemocast-engine`  
**Primary Tech:** Scala + Pekko HTTP

### Responsibilities

1. **Domain & Business Logic**
   - Define all core models:
     - Screen, Campaign, Creative, Targeting, Playlist, PlayEvent
   - Implement:
     - Targeting logic
     - Playlist selection
     - Budget enforcement

2. **Engine APIs**
   - Implement HTTP endpoints:
     - `POST /api/v1/screens/register`
     - `GET /api/v1/screens/{screenId}`
     - `GET /api/v1/screens/{screenId}/playlist?durationMinutes=X`
     - `POST /api/v1/events/play`
     - `GET /api/v1/debug/events` (for debugging)

3. **Demo Data Seeding**
   - Seed screens (mall, office, transit)
   - Seed campaigns (morning, weekend, brand, etc.)

4. **Engine-Level Tests**
   - Local unit/integration checks:
     - Targeting works per rules
     - Playlist changes when campaigns inactive or out of budget

### System A Can Work Completely Alone

- You can:
  - Start it with `sbt run` (or script)
  - Hit APIs using curl/Postman **from the same machine**
- You do **not** need System B to develop the engine.

---

## 💻 System B — Tools, Simulators & Docs

**Repository:** `mnemocast-tools` (or `mnemocast-lab`)  
**Primary Tech:** whatever is fastest (Node, Python, or even just Postman collections + Markdown)

### Responsibilities

1. **HTTP Client & Simulator**
   - Create a **simple script/app** that:
     - Registers sample screens
     - Seeds sample campaigns via pre-defined JSON (if you add admin API later)
     - Calls playlist API periodically (like a fake display)
     - Calls play event API to simulate playback

2. **Load & Scenario Testing**
   - Write small scripts to:
     - Request playlists for multiple screenIds
     - Simulate 100+ play events to test budget logic
   - Goal: stress test your engine’s logic, not performance yet.

3. **Demo Automation**
   - Prepare a **demo runner script** (optional):
     - A sequence of calls that reproduce your investor demo scenario.
   - Or:
     - A small CLI that prints:
       - “Playlist for Screen: office-1”
       - “Playlist for Screen: mall-1”
       - “Budget exhausted for Campaign XYZ”

4. **Documentation Hub**
   - Store all docs here too (or sync via Git submodule):
     - `01-domain-model.md`
     - `02-api-spec.md`
     - `03-playlist-logic.md`
     - `04-events-and-budget.md`
     - `demo-script.md`
   - This machine can be your **doc + research station**, while System A is focused on coding.

5. **Future: Player R&D**
   - Later, you can use this system to:
     - Develop the **web player** (HTML/JS)
     - Test it against the running engine
     - Control Raspberry Pi / SSH / deploy

### System B Can Work Completely Alone

- You can:
  - Mock engine responses (local JSON files) if System A is off.
  - Work on:
    - docs
    - simulation logic
    - demo scripts
  - Even design frontend or future monitoring tools **before** the real engine is online.

---

## 🔗 Integration Between System A & System B

The only shared contracts:

1. **Docs (copied or in shared repo)**
   - API spec
   - Domain model
   - Playlist & event formats

2. **HTTP APIs**
   - System B sends HTTP to System A when both are on same network:
     - `http://<SystemA-IP>:8080/api/v1/...`

3. **Git Repos**
   - `mnemocast-engine` (System A primary, System B can clone read-only for testing)
   - `mnemocast-tools` (System B primary, System A can clone if needed)

No direct filesystem or process dependency.

---

## 🔁 Example Daily Flow

### On System A (Engine)
- Open Cursor
- Work on:
  - new targeting rule
  - playlist logic fix
  - endpoint implementation
- Run engine locally.

### On System B (Tools)
- Run simulation script:
  - Register screens
  - Fetch playlists
  - Send play events
- Update docs when you discover changes.
- Prepare demo outputs.

You can do this **independently**:
- If System A is off, System B can still:
  - Edit docs
  - Improve simulator (using mock data)
- If System B is off, System A can still:
  - Improve core engine

---

## ✅ Benefits of This Split

- No machine is “blocked” by the other.
- You separate:
  - **Building the brain** (System A)
  - **Observing & proving the brain** (System B)
- You train yourself to think in **API contracts**, which is founder-level engineering.