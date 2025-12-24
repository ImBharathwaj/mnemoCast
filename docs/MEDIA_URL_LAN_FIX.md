#  Media/Creative URL LAN Access Fix

**Issue:** Images and videos (creatives) cannot be viewed from another machine on the LAN.

**Root Cause:** Media URLs are generated with `localhost:8080`, which doesn't work when accessing from another machine.

---

##  Solution

The backend now supports configuring the server host for media URLs via environment variables.

### Quick Fix

Set the `SERVER_HOST` environment variable to your server's IP address before starting the backend:

```bash
# Find your server's IP address
ip addr show  # Linux
# or
ifconfig     # macOS/Linux

# Set environment variable (replace with your actual IP)
export SERVER_HOST=192.168.1.100

# Start backend
cd backend
sbt run
```

### Permanent Fix

Add to your shell profile (`~/.bashrc`, `~/.zshrc`, etc.):

```bash
export SERVER_HOST=192.168.1.100
```

Or create a `.env` file in the backend directory (if using a process manager that supports it).

---

##  Configuration Options

### Option 1: SERVER_HOST (Recommended)
Sets the host part of media URLs. Port defaults to 8080.

```bash
export SERVER_HOST=192.168.1.100
# Media URLs will be: http://192.168.1.100:8080/api/v1/media/...
```

### Option 2: STORAGE_BASE_URL (Full Control)
Set the complete base URL for media storage.

```bash
export STORAGE_BASE_URL=http://192.168.1.100:8080/api/v1/media
```

### Option 3: Both Set
`STORAGE_BASE_URL` takes precedence if both are set.

---

##  Verification

### 1. Check Backend Startup Logs
When you start the backend, you should see:

```
 Media Storage: Local Filesystem
   Base Path: storage/uploads
   Base URL: http://192.168.1.100:8080/api/v1/media
```

If you see `localhost` instead, the environment variable isn't set correctly.

### 2. Test Media URL
After uploading a creative, check the returned URL:

```bash
# Upload a creative and check the response
curl -X POST http://192.168.1.100:8080/api/v1/creatives/upload \
  -F "file=@test.jpg"

# Response should contain:
# "url": "http://192.168.1.100:8080/api/v1/media/creatives/..."
```

### 3. Test from Another Machine
```bash
# From another machine, try to access a creative URL
curl -I http://192.168.1.100:8080/api/v1/media/creatives/some-file.jpg

# Should return 200 OK, not connection refused
```

---

##  Updating Existing Creatives

**Important:** Existing creatives in the database already have URLs with `localhost`. You have two options:

### Option A: Re-upload Creatives (Recommended)
1. Export existing creatives (if possible)
2. Set `SERVER_HOST` environment variable
3. Re-upload all creatives
4. New URLs will use the correct host

### Option B: Update Database URLs (Advanced)
If you have many creatives, you can update URLs in the database:

```sql
-- For PostgreSQL
UPDATE creatives 
SET creative_url = REPLACE(creative_url, 'localhost', '192.168.1.100')
WHERE creative_url LIKE '%localhost%';

-- For Redis, you'll need to update each creative manually or use a script
```

---

##  Troubleshooting

### Issue: Still seeing localhost URLs after setting SERVER_HOST

**Check:**
1. Environment variable is set before starting backend
2. Backend was restarted after setting the variable
3. Check startup logs for the actual Base URL being used

**Solution:**
```bash
# Verify environment variable
echo $SERVER_HOST

# Restart backend
cd backend
sbt run
```

### Issue: Media files load locally but not from other machines

**Check:**
1. Backend is accessible from other machines: `curl http://SERVER_IP:8080/api/v1/health`
2. Firewall allows port 8080
3. Media URLs in API responses use the correct IP (not localhost)

**Test:**
```bash
# From another machine
curl http://SERVER_IP:8080/api/v1/media/creatives/some-file.jpg
```

### Issue: CORS errors when loading media

Media serving already includes CORS headers, but verify:
1. Backend CORS is configured (already done)
2. Browser console shows CORS errors (not network errors)

---

##  Example: Complete Setup

```bash
# 1. Find your server IP
ip addr show | grep "inet " | grep -v 127.0.0.1
# Example output: inet 192.168.1.100/24

# 2. Set environment variable
export SERVER_HOST=192.168.1.100

# 3. Verify
echo $SERVER_HOST

# 4. Start backend
cd backend
sbt run

# 5. Check logs - should show:
# Base URL: http://192.168.1.100:8080/api/v1/media

# 6. Upload a creative and verify URL
curl -X POST http://192.168.1.100:8080/api/v1/creatives/upload \
  -F "file=@test.jpg" | jq .url

# Should return: "http://192.168.1.100:8080/api/v1/media/creatives/..."
```

---

##  Production Considerations

For production deployments:

1. **Use HTTPS:**
   ```bash
   export STORAGE_BASE_URL=https://yourdomain.com/api/v1/media
   ```

2. **Use a CDN:**
   ```bash
   export STORAGE_BASE_URL=https://cdn.yourdomain.com/media
   ```

3. **Use Environment-Specific Configs:**
   - Development: `localhost`
   - Staging: `staging.yourdomain.com`
   - Production: `cdn.yourdomain.com`

---

##  Expected Behavior After Fix

1.  Backend startup shows correct Base URL (not localhost)
2.  Uploaded creatives have URLs with server IP
3.  Media files load from any machine on LAN
4.  Dashboard displays images/videos correctly
5.  No "connection refused" or "localhost" errors

---

**Last Updated:** December 2024

