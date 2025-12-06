# 🧱 Mnemocast Engine — Epics, User Stories & Tasks

> Scope: Backend Engine Only (Phase 1 — Core Ad Serving Engine MVP)

---

## EPIC E1 — Domain Model & Core Data Structures

### 🎯 Epic Goal
Define the fundamental objects and data relationships that the Mnemocast Engine will operate on.

### 📌 User Stories
- **S1.1** — As the engine, I need a model for **Screens** with context details.
- **S1.2** — As the engine, I need a model for **Campaigns** and **Creatives**.
- **S1.3** — As the engine, I must understand **Targeting rules**.
- **S1.4** — As the engine, I must represent **Playlists** and **PlayEvents**.

### 📝 Tasks
- **T1.1** — Create `docs/01-domain-model.md`
- **T1.2** — Define Screen schema (id, geo, tags, active)
- **T1.3** — Define Campaign + Creative schemas (priority, duration, type…)
- **T1.4** — Define Targeting fields (cities, tags, time bands)
- **T1.5** — Define Playlist structures (validity + sequence of creatives)
- **T1.6** — Define PlayEvent structure for tracking
- **T1.7** — Review & finalize domain documentation

🟢 **Done when:** All entities documented & approved

---

## EPIC E2 — Screen Registration & Lookup

### 🎯 Epic Goal
Engine must store and identify screens with contextual metadata.

### 📌 User Stories
- **S2.1** — As an operator, I can register new screens.
- **S2.2** — As the engine, I can retrieve screen details by ID.

### 📝 Tasks
- **T2.1** — Define API spec in `docs/02-api-spec.md`
- **T2.2** — Implement ScreenStore (in-memory for MVP)
- **T2.3** — Implement:
  - `POST /api/v1/screens/register`
  - `GET /api/v1/screens/{screenId}`
- **T2.4** — Add validation for required fields
- **T2.5** — Seed 3 demo screens with varied contexts
- **T2.6** — Local testing & Postman setup

🟢 **Done when:** Screens can be registered & fetched successfully

---

## EPIC E3 — Campaigns, Creatives & Targeting Logic

### 🎯 Epic Goal
Engine must determine which campaigns are relevant for a screen at any time.

### 📌 User Stories
- **S3.1** — As the engine, I can load active campaigns.
- **S3.2** — As the engine, I filter campaigns by targeting rules.
- **S3.3** — As the system, campaigns hold multiple creatives.

### 📝 Tasks
- **T3.1** — Add Campaign + Creative details to `docs/01-domain-model.md`
- **T3.2** — Write targeting rule definitions in `docs/03-playlist-logic.md`
- **T3.3** — Implement CampaignStore + CreativeStore (in-memory)
- **T3.4** — Implement “active window” logic (start/end dates)
- **T3.5** — Matching logic for geo + tags + time
- **T3.6** — Seed 4–5 demo campaigns with different targeting
- **T3.7** — Testing targeting scenarios

🟢 **Done when:** Eligible campaigns can be listed for a given screen and time

---

## EPIC E4 — Playlist Generation Engine

### 🎯 Epic Goal
Return a dynamic playlist that maximizes priority while honoring duration & targeting.

### 📌 User Stories
- **S4.1** — As a screen, I request a playlist for N minutes.
- **S4.2** — As the engine, I auto-generate playlist filling requested duration.
- **S4.3** — As the engine, I consider campaign weight/priority.

### 📝 Tasks
- **T4.1** — Playlist API spec in `docs/02-api-spec.md`
- **T4.2** — Implement weighted random creative selection
- **T4.3** — Implement duration-based playlist fill strategy
- **T4.4** — Add `validForSeconds` field
- **T4.5** — Playlist logging for internal debugging
- **T4.6** — Document example playlist behaviors in `docs/03-playlist-logic.md`

🟢 **Done when:** Different screens receive different playlists under same time conditions

---

## EPIC E5 — Play Event Logging & Budget Enforcement

### 🎯 Epic Goal
Close the feedback loop — ads served affect future decisions.

### 📌 User Stories
- **S5.1** — As a screen, I send play events to the engine.
- **S5.2** — As the engine, I update campaign eligibility based on usage.
- **S5.3** — As operator, I can inspect events (debug).

### 📝 Tasks
- **T5.1** — Define event + budget logic in `docs/04-events-and-budget.md`
- **T5.2** — Implement EventStore + `/events/play`
- **T5.3** — Implement budget check (`maxPlays` exhaustion)
- **T5.4** — Implement `/debug/events` for demo/testing
- **T5.5** — Test scenario:
  1) Request playlist  
  2) Log events  
  3) Confirm campaign stops showing

🟢 **Done when:** Play events visibly influence future playlists

---

## EPIC E6 — Demo Script + Reliability Pass

### 🎯 Epic Goal
Deliver a stable, repeatable demonstration of the engine.

### 📌 User Stories
- **S6.1** — As a founder, I can run a 10-minute live demo confidently.

### 📝 Tasks
- **T6.1** — Write “Engine Demo Script” in `/docs/demo-script.md`
- **T6.2** — Create Postman collection for all endpoints
- **T6.3** — Add logging for debugging
- **T6.4** — Stability testing with seeded data
- **T6.5** — Clean startup + success messages

🟢 **Done when:** Engine pitch can run **without manual hacks** or crash

---

## ✔ Phase Completion Checklist

- [ ] All API endpoints functional
- [ ] Screening logic + playlist logic validated
- [ ] Budget & events visibly working
- [ ] Demo script tested 3+ times
- [ ] Stakeholder-ready showcase environment

---

## 🎯 Final Output for Phase 1
A **fully operational backend** that *proves* Mnemocast’s intelligence —  
even without UI or Raspberry Pi player yet.