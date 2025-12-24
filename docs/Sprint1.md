#  Mnemocast Engine — Sprint Plan (6 Weeks)

##  Objective
Build the **Core Ad Serving Engine** for Mnemocast:
- Register screens
- Manage campaigns & creatives
- Generate dynamic playlists
- Log play events & enforce budgets
- Demonstrate smart ad delivery (no client UI yet)

Output: Fully demo-ready backend with Postman/terminal

---

##  Sprint Breakdown

###  Sprint 1 — Domain Model + Screen APIs
**Duration:** 1 week

**User Stories**
- Define domain entities (Screen, Campaign, Creative, Targeting, Playlist, PlayEvent)
- Register and fetch screens via API

**Tasks**
- Write `docs/01-domain-model.md`
- Write `docs/02-api-spec.md` (Screen APIs section)
- Create ScreenStore (in-memory)
- Implement:
  - `POST /api/v1/screens/register`
  - `GET /api/v1/screens/{screenId}`
- Seed minimum 3 screens with varied tags & locations

**Acceptance Criteria**
- Able to register a screen → returns screenId
- Able to fetch screen details

---

###  Sprint 2 — Campaign & Creative Handling
**Duration:** 1 week

**User Stories**
- Engine can store campaign & creative metadata
- Engine can identify active campaigns at a given time

**Tasks**
- Extend `docs/01-domain-model.md` with Campaign + Creative details
- Add `docs/03-playlist-logic.md` (Targeting rules section)
- Implement CampaignStore + CreativeStore (in-memory)
- Seed 4–5 campaigns with:
  - Different tags
  - Different time/day constraints
  - Different priorities

**Acceptance Criteria**
- Query active campaigns for a screen at a given time in dev logs

---

###  Sprint 3 — Playlist Generation Engine (v1)
**Duration:** 2 weeks

**User Stories**
- Engine generates playlist based on:
  - screen context
  - campaign targeting & active window
  - campaign priority
  - requested duration

**Tasks**
- Complete playlist logic definition in `docs/03-playlist-logic.md`
- Implement playlist selection algorithm
- Add API:
  - `GET /api/v1/screens/{screenId}/playlist?durationMinutes=X`
- Prioritize campaigns by weight (priority multiplier)
- Duration-based playlist fill
- Add `validForSeconds` field in response

**Acceptance Criteria**
- Different screens return different playlists
- Playlist approx. matches requested duration

---

###  Sprint 4 — Play Event Logging + Budget Enforcement
**Duration:** 1 week

**User Stories**
- Engine tracks ad plays
- Budget limits influence future playlists

**Tasks**
- Write `docs/04-events-and-budget.md`
- Implement EventStore (in-memory)
- API:
  - `POST /api/v1/events/play`
- Add debug endpoint:
  - `GET /api/v1/debug/events`
- Enforce: `if plays >= maxPlays` → exclude campaign

**Acceptance Criteria**
- Play events visible through debug API
- Campaign drops out after hitting maxPlays

---

###  Sprint 5 — Demo Story + Stability Hardening
**Duration:** 1 week

**User Stories**
- Engine reliably demonstrates intelligence during live pitch

**Tasks**
- Clean logging & error messages
- Add retry + safety checks in playlist serve
- Create repeatable test/demo data seeding
- Create Postman collection for all endpoints
- Write **Demo Script**:
  1) Register screens  
  2) Fetch playlists  
  3) Trigger play events  
  4) Show campaign removal after budget reached  
  5) Confirm context-based changes live

**Acceptance Criteria**
- 10-minute **flawless** live demo possible
- Stakeholders understand value immediately

---

##  Timeline Summary

| Sprint | Duration | Focus | Demo Output |
|--------|:-------:|------|-------------|
| 1 | 1 week | Screens | Register + query screens |
| 2 | 1 week | Campaigns | Eligible campaigns logged |
| 3 | 2 weeks | Playlist Engine | Smart playlist decisions |
| 4 | 1 week | Events + Budget | Dynamic campaign pacing |
| 5 | 1 week | Demo polish | Investor-ready engine |

⏱ Total: **6 Weeks** (Core Engine MVP)

---

##  Principle for Phase 1
> **If it doesn’t show up in the demo → it’s not in Phase 1.**

---

##  Next Step
Start with:
- `docs/01-domain-model.md`
- `docs/02-api-spec.md`

Then give Cursor those docs and let it generate the first data structures + API skeleton.