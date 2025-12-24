#  Dashboard Creative URL Fix

**Issue:** Clicking on creative links in the dashboard redirects to `localhost`, which doesn't work when accessing from another machine.

**Root Cause:** Creative URLs stored in the database contain `localhost` (from MinIO or local storage), and the dashboard was displaying them directly without transformation.

---

##  Solution Applied

The dashboard now automatically transforms creative URLs to replace `localhost` with the current hostname when accessed from another machine.

### How It Works

1. **URL Transformation Utility:** Created `dashboard/src/utils/urlTransform.ts`
   - Detects when dashboard is accessed from a non-localhost IP
   - Automatically replaces `localhost` in URLs with the current hostname
   - Works for both MinIO URLs and local storage URLs

2. **Updated Components:**
   - `Creatives.tsx` - Creative list page
   - `CampaignDetail.tsx` - Campaign detail page with creatives
   - `Playlist.tsx` - Playlist view with creative links

---

##  Technical Details

### URL Transformation Function

```typescript
export const transformMediaUrl = (url: string): string => {
  if (!url) return url;
  
  // If running in browser, get current hostname
  if (typeof window !== 'undefined') {
    const currentHost = window.location.hostname;
    
    // Only transform if not already on localhost
    if (currentHost !== 'localhost' && currentHost !== '127.0.0.1') {
      // Replace localhost with current host
      let transformedUrl = url.replace(/http:\/\/localhost:/g, `http://${currentHost}:`);
      transformedUrl = transformedUrl.replace(/http:\/\/127\.0\.0\.1:/g, `http://${currentHost}:`);
      
      return transformedUrl;
    }
  }
  
  return url;
};
```

### Usage

All creative links now use:
```tsx
<a href={transformMediaUrl(creative.creativeUrl)}>
  View Creative →
</a>
```

---

##  Testing

### Test from Another Machine

1. **Access Dashboard:**
   ```
   http://SERVER_IP:3000
   ```

2. **Navigate to Creatives Page:**
   - Go to Creatives section
   - Click on any "View Creative →" link
   - Should open media in new tab with correct IP (not localhost)

3. **Check URL:**
   - Hover over link - should show `http://SERVER_IP:9000/...` (not localhost)
   - Click link - should load media correctly

### Expected Behavior

-  Links show correct IP address (not localhost)
-  Media files load correctly when clicked
-  Works for both MinIO and local storage URLs
-  No redirect to localhost

---

##  How URLs Are Transformed

### Example Transformations

**Before (from API):**
```
http://localhost:9000/mnemocast-creatives/creatives/file.jpg
http://localhost:8080/api/v1/media/creatives/file.jpg
```

**After (displayed in dashboard from another machine):**
```
http://192.168.1.100:9000/mnemocast-creatives/creatives/file.jpg
http://192.168.1.100:8080/api/v1/media/creatives/file.jpg
```

---

##  Troubleshooting

### Issue: Links still redirect to localhost

**Check:**
1. Dashboard was rebuilt after changes
2. Browser cache cleared (hard refresh: Ctrl+Shift+R)
3. Check browser console for errors

**Solution:**
```bash
cd dashboard
npm run build
# Or for dev
npm start
```

### Issue: Media doesn't load after clicking link

**Check:**
1. Backend/MinIO is accessible from client machine
2. Firewall allows ports (8080 for API, 9000 for MinIO)
3. URLs are being transformed correctly (check browser DevTools → Network)

**Test:**
```bash
# From client machine
curl http://SERVER_IP:9000/mnemocast-creatives/creatives/file.jpg
curl http://SERVER_IP:8080/api/v1/media/creatives/file.jpg
```

### Issue: Transformation not working

**Check:**
1. `urlTransform.ts` file exists in `dashboard/src/utils/`
2. Import statement is correct in component files
3. Browser console for JavaScript errors

---

##  Files Changed

1. **Created:**
   - `dashboard/src/utils/urlTransform.ts` - URL transformation utility

2. **Updated:**
   - `dashboard/src/pages/Creatives.tsx` - Added URL transformation
   - `dashboard/src/pages/CampaignDetail.tsx` - Added URL transformation
   - `dashboard/src/pages/Playlist.tsx` - Added URL transformation

---

##  Related Fixes

This fix works together with:
- **Backend Media URL Fix:** `docs/MEDIA_URL_LAN_FIX.md`
- **MinIO LAN Access Fix:** `docs/MINIO_LAN_ACCESS_FIX.md`
- **Dashboard LAN Access Fix:** `docs/DASHBOARD_LAN_ACCESS_FIX.md`

For complete LAN access, ensure:
1.  Backend uses `SERVER_HOST` environment variable
2.  MinIO is accessible from other machines
3.  Dashboard transforms URLs (this fix)

---

##  Expected Behavior After Fix

1.  Creative links show correct IP address (not localhost)
2.  Clicking links opens media with correct URL
3.  Media loads correctly from any machine on LAN
4.  No redirects to localhost
5.  Works for both existing and new creatives

---

**Last Updated:** December 2024

