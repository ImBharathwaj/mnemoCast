#  Heartbeat 403 Error - Fix & Troubleshooting

##  Changes Made

### 1. Enhanced Authentication Logging

**File:** `backend/modules/engine-api/src/main/scala/mnemocast/engine/api/middleware/ScreenAuthMiddleware.scala`

**Improvements:**
-  Added detailed logging for authentication attempts
-  Logs screen ID being authenticated
-  Logs passkey mismatch details (length comparison)
-  Logs missing headers with available headers list
-  Logs screen not found errors
-  Case-insensitive header matching (both `.is()` and `.lowercaseName()`)

**Log Output Examples:**
```
[2025-12-19 00:36:00] [ScreenAuth] Attempting authentication for screen: d31f2fe7-16f3-4842-8db7-4b67868ecdc6
[2025-12-19 00:36:00] [ScreenAuth] Authentication successful for screen: d31f2fe7-16f3-4842-8db7-4b67868ecdc6 (Screen Name)
```

Or on failure:
```
[2025-12-19 00:36:00] [ScreenAuth] Authentication failed: Missing X-Screen-Id header
[2025-12-19 00:36:00] [ScreenAuth] Available headers: Content-Type, User-Agent, ...
```

### 2. Route-Level Logging

**File:** `backend/modules/engine-api/src/main/scala/mnemocast/engine/api/routes/ScreenRoutes.scala`

**Improvements:**
-  Logs when heartbeat route is matched
-  Logs all request headers for debugging
-  Helps identify if request reaches the endpoint

**Log Output:**
```
[2025-12-19 00:36:00] [PUT /api/v1/screens/d31f2fe7-16f3-4842-8db7-4b67868ecdc6/heartbeat] Route matched - checking authentication
[2025-12-19 00:36:00] [PUT /api/v1/screens/d31f2fe7-16f3-4842-8db7-4b67868ecdc6/heartbeat] Request headers: X-Screen-Id=d31f2fe7-..., X-Screen-Passkey=..., ...
```

### 3. Better Error Response

**File:** `backend/modules/engine-api/src/main/scala/mnemocast/engine/api/HttpServer.scala`

**Improvements:**
-  Specific handler for `AuthorizationFailedRejection`
-  Returns proper JSON error message
-  Includes CORS headers in error response

**Error Response:**
```json
{
  "error": "Authentication failed. Please check your credentials."
}
```

---

##  Troubleshooting Steps

### Step 1: Check Backend Logs

When the screen client sends a heartbeat, check the backend logs for:

1. **Route Matching:**
   ```
   [PUT /api/v1/screens/{screenId}/heartbeat] Route matched - checking authentication
   ```
   If you don't see this, the route isn't being matched.

2. **Headers Received:**
   ```
   Request headers: X-Screen-Id=..., X-Screen-Passkey=..., ...
   ```
   Verify the headers are present and correctly named.

3. **Authentication Attempt:**
   ```
   [ScreenAuth] Attempting authentication for screen: {screenId}
   ```
   This confirms the middleware is being called.

4. **Authentication Result:**
   - Success: `[ScreenAuth] Authentication successful for screen: ...`
   - Failure: `[ScreenAuth] Authentication failed: ...` (with reason)

### Step 2: Verify Screen Exists

**Check if screen is registered:**
```sql
SELECT id, name, passkey FROM screens WHERE id = 'd31f2fe7-16f3-4842-8db7-4b67868ecdc6';
```

**If using Redis:**
```bash
redis-cli GET "screens:d31f2fe7-16f3-4842-8db7-4b67868ecdc6"
```

### Step 3: Verify Passkey

**Get the correct passkey:**
```sql
SELECT passkey FROM screens WHERE id = 'd31f2fe7-16f3-4842-8db7-4b67868ecdc6';
```

**Compare with client:**
- Ensure the passkey in the client matches exactly
- Check for whitespace or encoding issues
- Verify the passkey wasn't truncated

### Step 4: Check Header Names

**Verify header names are correct:**
- `X-Screen-Id` (case-insensitive, but use this exact format)
- `X-Screen-Passkey` (case-insensitive, but use this exact format)

**Common Issues:**
-  `x-screen-id` (lowercase) - Should work but prefer `X-Screen-Id`
-  `X-SCREEN-ID` (uppercase) - Should work but prefer `X-Screen-Id`
-  `screen-id` (missing X- prefix) - Won't work
-  `X-ScreenId` (camelCase) - Won't work

### Step 5: Test with curl

**Test heartbeat endpoint:**
```bash
# Replace with actual screen ID and passkey
SCREEN_ID="d31f2fe7-16f3-4842-8db7-4b67868ecdc6"
PASSKEY="your-actual-passkey-here"

curl -X PUT "http://localhost:8080/api/v1/screens/$SCREEN_ID/heartbeat" \
  -H "X-Screen-Id: $SCREEN_ID" \
  -H "X-Screen-Passkey: $PASSKEY" \
  -v
```

**Expected Response (200 OK):**
```json
{
  "id": "d31f2fe7-16f3-4842-8db7-4b67868ecdc6",
  "name": "Screen Name",
  "passkey": "***REDACTED***",
  ...
}
```

**If 403 Forbidden:**
- Check backend logs for authentication failure reason
- Verify screen ID and passkey are correct
- Ensure headers are sent correctly

---

##  Common Issues & Solutions

### Issue 1: Screen Not Found

**Error in logs:**
```
[ScreenAuth] Authentication failed: Screen not found: {screenId}
```

**Solution:**
1. Register the screen first using `POST /api/v1/screens/register`
2. Save the returned passkey
3. Use the correct screen ID and passkey

### Issue 2: Passkey Mismatch

**Error in logs:**
```
[ScreenAuth] Authentication failed: Passkey mismatch for screen: {screenId}
Expected passkey length: 44, Provided passkey length: 43
```

**Solution:**
1. Get the correct passkey from database
2. Ensure no whitespace in passkey
3. Check for encoding issues (URL encoding, etc.)
4. Verify passkey wasn't truncated

### Issue 3: Missing Headers

**Error in logs:**
```
[ScreenAuth] Authentication failed: Missing X-Screen-Id header
Available headers: Content-Type, User-Agent, ...
```

**Solution:**
1. Ensure client sends both headers:
   - `X-Screen-Id: {screen-id}`
   - `X-Screen-Passkey: {passkey}`
2. Check header names are exact (case-insensitive but prefer `X-Screen-Id`)
3. Verify headers aren't being stripped by proxy/load balancer

### Issue 4: Route Not Matched

**No logs for route matching:**
- Request might be matching a different route
- Check route ordering in `ScreenRoutes.scala`
- Verify HTTP method is `PUT` (not `POST` or `GET`)

---

##  Debugging Checklist

- [ ] Backend logs show route matched
- [ ] Backend logs show headers received
- [ ] Backend logs show authentication attempt
- [ ] Screen exists in database/Redis
- [ ] Passkey matches exactly
- [ ] Headers are named correctly (`X-Screen-Id`, `X-Screen-Passkey`)
- [ ] HTTP method is `PUT`
- [ ] URL path is correct (`/api/v1/screens/{screenId}/heartbeat`)
- [ ] No proxy/load balancer stripping headers
- [ ] CORS preflight (OPTIONS) succeeds

---

##  Quick Fixes

### If Screen Doesn't Exist

1. **Register screen via API:**
   ```bash
   curl -X POST "http://localhost:8080/api/v1/screens/register" \
     -H "Authorization: Bearer <admin-token>" \
     -H "Content-Type: application/json" \
     -d '{
       "name": "Test Screen",
       "location": {"country": "IN", "city": "Mumbai"},
       ...
     }'
   ```

2. **Save the returned passkey**

3. **Use the screen ID and passkey in client**

### If Passkey Doesn't Match

1. **Query database for correct passkey:**
   ```sql
   SELECT id, name, passkey FROM screens WHERE id = '{screen-id}';
   ```

2. **Update client configuration with correct passkey**

3. **Restart screen client**

---

##  Expected Log Flow (Success)

```
[2025-12-19 00:36:00] [PUT /api/v1/screens/d31f2fe7-.../heartbeat] Route matched - checking authentication
[2025-12-19 00:36:00] [PUT /api/v1/screens/d31f2fe7-.../heartbeat] Request headers: X-Screen-Id=d31f2fe7-..., X-Screen-Passkey=..., ...
[2025-12-19 00:36:00] [ScreenAuth] Attempting authentication for screen: d31f2fe7-...
[2025-12-19 00:36:00] [ScreenAuth] Authentication successful for screen: d31f2fe7-... (Screen Name)
[2025-12-19 00:36:00] [PUT /api/v1/screens/d31f2fe7-.../heartbeat] Request received from authenticated screen: Screen Name
[2025-12-19 00:36:00] [PUT /api/v1/screens/d31f2fe7-.../heartbeat] Success: Heartbeat recorded
```

---

##  Expected Log Flow (Failure)

```
[2025-12-19 00:36:00] [PUT /api/v1/screens/d31f2fe7-.../heartbeat] Route matched - checking authentication
[2025-12-19 00:36:00] [PUT /api/v1/screens/d31f2fe7-.../heartbeat] Request headers: ...
[2025-12-19 00:36:00] [ScreenAuth] Attempting authentication for screen: d31f2fe7-...
[2025-12-19 00:36:00] [ScreenAuth] Authentication failed: Passkey mismatch for screen: d31f2fe7-...
[2025-12-19 00:36:00] [ScreenAuth] Expected passkey length: 44, Provided passkey length: 43
[2025-12-19 00:36:00] REJECTION: AuthorizationFailedRejection - Authentication failed
```

---

##  Next Steps

1. **Restart backend** to get new logging
2. **Check backend logs** when screen client sends heartbeat
3. **Identify the specific failure reason** from logs
4. **Fix the issue** based on the error message
5. **Verify** heartbeat succeeds

The enhanced logging will help identify exactly why authentication is failing!

---

**Status:**  Enhanced Logging Added  
**Date:** 2025-12-19  
**Version:** 4.2.1

