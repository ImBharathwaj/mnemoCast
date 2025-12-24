#  Dashboard Real-Time Data Fix

**Issue:** Frontend doesn't show any real-time data.

**Solution:** Added auto-refresh functionality and improved error handling.

---

##  Changes Made

### 1. **Auto-Refresh Added**
- Dashboard now refreshes every 10 seconds automatically
- Toggle switch to enable/disable auto-refresh
- Manual refresh button available

### 2. **Error Handling Improved**
- Better error messages displayed to user
- Shows API base URL being used
- Link to test backend connection
- Console logging for debugging

### 3. **Last Updated Timestamp**
- Shows when data was last refreshed
- Helps verify auto-refresh is working

---

##  Troubleshooting Steps

### Step 1: Check Browser Console
Open browser DevTools (F12) → Console tab and look for:
- API Request/Response logs
- Error messages
- API Base URL being used

**Expected logs:**
```
API Base URL: http://192.168.1.100:8080
API Request: GET /api/v1/campaigns
API Response: 200 /api/v1/campaigns
```

### Step 2: Verify Backend is Running
```bash
# Test backend health endpoint
curl http://YOUR_SERVER_IP:8080/api/v1/health

# Should return:
# {"status":"UP",...}
```

### Step 3: Check API Base URL
The dashboard automatically detects the API URL:
- If accessed from `http://192.168.1.100:3000`, API URL will be `http://192.168.1.100:8080`
- If accessed from `http://localhost:3000`, API URL will be `http://localhost:8080`

**To override:** Set environment variable:
```bash
# In dashboard directory
export REACT_APP_API_URL=http://YOUR_SERVER_IP:8080
npm start
```

### Step 4: Check Network Tab
Open browser DevTools → Network tab:
- Look for API requests to `/api/v1/campaigns`, `/api/v1/screens`, etc.
- Check response status codes (should be 200)
- Check if requests are being made

### Step 5: Verify CORS
Backend should have CORS enabled (already configured). Check backend logs for CORS errors.

---

##  Common Issues

### Issue: "Failed to load campaigns" error

**Possible Causes:**
1. Backend not running
2. Wrong API URL
3. CORS issues
4. Network connectivity

**Solution:**
```bash
# 1. Check backend is running
cd backend
sbt run

# 2. Test API directly
curl http://YOUR_SERVER_IP:8080/api/v1/campaigns

# 3. Check browser console for detailed error
```

### Issue: Data shows zeros or empty

**Possible Causes:**
1. No data in database
2. API returning empty arrays
3. Data filtering issue

**Solution:**
```bash
# Check if data exists
curl http://YOUR_SERVER_IP:8080/api/v1/campaigns
curl http://YOUR_SERVER_IP:8080/api/v1/screens

# Create test data
# Use dashboard to create a campaign or screen
```

### Issue: Auto-refresh not working

**Check:**
1. Auto-refresh toggle is enabled (checkbox checked)
2. Browser console for errors
3. Network tab shows requests every 10 seconds

**Solution:**
- Click "Refresh" button manually
- Check browser console for errors
- Verify no JavaScript errors blocking execution

---

##  Manual Testing

### Test Dashboard Data Loading

1. **Open Dashboard:**
   ```
   http://YOUR_SERVER_IP:3000
   ```

2. **Check Stats:**
   - Should show Total Campaigns, Active Campaigns, Total Screens, Online Screens
   - If all zeros, create test data

3. **Create Test Data:**
   - Go to Campaigns page → Create a campaign
   - Go to Screens page → Register a screen
   - Return to Dashboard → Should see updated counts

4. **Test Auto-Refresh:**
   - Enable auto-refresh toggle
   - Create/delete a campaign in another tab
   - Dashboard should update within 10 seconds

---

##  Expected Behavior

### Dashboard Should Show:
-  Total Campaigns count
-  Active Campaigns count  
-  Total Screens count
-  Online Screens count
-  Analytics data (if available)
-  Last updated timestamp
-  Auto-refresh indicator

### Auto-Refresh:
-  Refreshes every 10 seconds when enabled
-  Shows "Last updated" timestamp
-  Updates counts automatically
-  Can be toggled on/off

---

##  Quick Fixes

### Fix 1: Rebuild Dashboard
```bash
cd dashboard
npm run build
# Or for dev
npm start
```

### Fix 2: Clear Browser Cache
- Hard refresh: Ctrl+Shift+R (Windows/Linux) or Cmd+Shift+R (Mac)
- Or clear browser cache completely

### Fix 3: Check Backend Logs
```bash
# Backend should show incoming requests
[INFO] ===> GET /api/v1/campaigns
[INFO] <=== GET /api/v1/campaigns -> 200
```

### Fix 4: Verify Environment Variables
```bash
# Check what API URL is being used
# Look in browser console for: "API Base URL: ..."
```

---

##  Verification Checklist

- [ ] Backend is running and accessible
- [ ] Dashboard shows correct API URL in console
- [ ] Browser console shows API requests
- [ ] Network tab shows successful API calls (200 status)
- [ ] Dashboard displays data (not all zeros)
- [ ] Auto-refresh toggle works
- [ ] Manual refresh button works
- [ ] Last updated timestamp updates
- [ ] No CORS errors in console
- [ ] No JavaScript errors in console

---

**Last Updated:** December 2024

