
---

## 🆕 `Epic2.md` — Basic Targeting & Ad Management

Now that Epic 1 is done, Epic 2 should **make the engine feel smarter + easier to demo**, without exploding scope.

I’ll focus Epic 2 on:

- **Real targeting** (not just random)
- **Programmatic ad creation** (no manual seed-only flow)
- **Simple querying for debugging/analytics**

Here’s the next epic.

```markdown
# Mnemocast — Epic 2: Basic Targeting & Ad Management

> Goal: Move from "random ad server" → "smart, filter-based ad server"
> - Use targeting rules (country, platform, etc.)
> - Provide API to create and list ads
> - Add a simple endpoint to inspect recorded events

---

## 1. Targeting Logic in Domain

Introduce pure targeting logic in the domain layer.

**Module:** `engine-domain`  
**Path:** `modules/engine-domain/src/main/scala/mnemocast/engine/domain/services/`

**File:**
- `TargetingService.scala`

Responsibilities:

- Function(s) like:

  ```scala
  def matches(ad: Ad, request: DeliveryRequest): Boolean
