# 🔧 Dashboard LAN Access Fix

**Issue:** Dashboard accessible from another machine but shows no data.

**Root Cause:** API client was hardcoded to `http://localhost:8080`, which doesn't work when accessing from another machine.

---

## ✅ Solution Applied

The API configuration has been updated to automatically detect the host when accessed from another machine. The dashboard will now:

1. **First:** Check for `REACT_APP_API_URL` environment variable
2. **Second:** If accessed from non-localhost, use the same host for API calls
3. **Fallback:** Use `localhost:8080` if accessed locally

---

## 🚀 Quick Fix Steps

### Option 1: Automatic Detection (Already Applied)
The code now automatically detects the host. Just rebuild and restart:

```bash
cd dashboard
npm run build
# Serve the build folder or restart dev server
```

### Option 2: Set Environment Variable (Recommended for Production)

Create a `.env` file in the `dashboard` directory:

```env
REACT_APP_API_URL=http://YOUR_SERVER_IP:8080
```

Replace `YOUR_SERVER_IP` with the actual IP address of the machine running the backend.

Then rebuild:
```bash
cd dashboard
npm run build
```

### Option 3: Manual Configuration

If you need to set it manually, edit `dashboard/src/config/api.ts`:

```typescript
export const API_BASE_URL = 'http://YOUR_SERVER_IP:8080';
```

---

## 🔍 Verification Steps

1. **Check Backend is Accessible:**
   ```bash
   # From another machine, test backend directly
   curl http://YOUR_SERVER_IP:8080/api/v1/health
   ```

2. **Check Browser Console:**
   - Open browser DevTools (F12)
   - Go to Console tab
   - Look for: `API Base URL: http://...`
   - Should show the correct IP, not `localhost:8080`

3. **Check Network Tab:**
   - Open browser DevTools → Network tab
   - Refresh the dashboard
   - Look for API requests
   - Check if they're going to the correct IP address
   - Check response status codes (should be 200, not CORS errors)

---

## 🐛 Troubleshooting

### Issue: Still shows "localhost:8080" in console

**Solution:**
- Clear browser cache
- Hard refresh (Ctrl+Shift+R or Cmd+Shift+R)
- Rebuild the dashboard: `npm run build`

### Issue: CORS errors in browser console

**Check:**
1. Backend CORS is configured (already done - allows all origins)
2. Backend is running and accessible
3. Firewall allows port 8080

**Test backend directly:**
```bash
curl -H "Origin: http://CLIENT_IP:3000" \
     -H "Access-Control-Request-Method: GET" \
     -H "Access-Control-Request-Headers: Content-Type" \
     -X OPTIONS \
     http://SERVER_IP:8080/api/v1/health
```

### Issue: Network errors / Connection refused

**Check:**
1. Backend is running: `cd backend && sbt run`
2. Backend binds to all interfaces (0.0.0.0), not just localhost
3. Firewall allows connections on port 8080

**Verify backend binding:**
Check `HttpServer.scala` - should bind to `0.0.0.0` or check environment variable:
```bash
# Backend should listen on all interfaces
# Check with: netstat -tulpn | grep 8080
```

### Issue: API calls timeout

**Check:**
1. Backend is accessible from client machine
2. Network connectivity
3. Backend is not overloaded

**Test:**
```bash
# From client machine
curl -v http://SERVER_IP:8080/api/v1/health
```

---

## 📝 Environment Variables

### For Development

Create `dashboard/.env`:
```env
REACT_APP_API_URL=http://192.168.1.100:8080
```

### For Production Build

Set before building:
```bash
export REACT_APP_API_URL=http://YOUR_SERVER_IP:8080
npm run build
```

Or use a `.env.production` file:
```env
REACT_APP_API_URL=http://YOUR_SERVER_IP:8080
```

---

## 🔐 Security Note

For production, consider:
1. Using HTTPS instead of HTTP
2. Restricting CORS to specific origins instead of `*`
3. Using a reverse proxy (nginx) for better security

---

## ✅ Expected Behavior After Fix

1. Dashboard loads from any machine on LAN
2. API calls go to correct backend IP
3. Data loads correctly in dashboard
4. No CORS errors in console
5. Network tab shows successful API calls (200 status)

---

## 🧪 Test Checklist

- [ ] Dashboard loads from another machine
- [ ] Browser console shows correct API URL (not localhost)
- [ ] Network tab shows API calls to correct IP
- [ ] API calls return 200 status
- [ ] Data appears in dashboard
- [ ] No CORS errors
- [ ] No network errors

---

**Last Updated:** December 2024

