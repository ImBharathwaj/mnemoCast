# 🪣 MinIO Media URL LAN Access Fix

**Issue:** Media URLs point to MinIO at `localhost:9000`, which is not accessible from other machines on the LAN.

**Root Cause:** MinIO base URLs are constructed from the endpoint, which defaults to `localhost:9000`.

---

## ✅ Solution

The backend now automatically uses the `SERVER_HOST` environment variable to construct MinIO URLs that are accessible from other machines.

### Quick Fix

Set the `SERVER_HOST` environment variable to your server's IP address before starting the backend:

```bash
# Find your server's IP address
ip addr show | grep "inet " | grep -v 127.0.0.1
# Or use: hostname -I

# Set environment variable (replace with your actual IP)
export SERVER_HOST=192.168.1.100

# Start backend
cd backend
sbt run
```

### What This Does

When `SERVER_HOST` is set, MinIO URLs will be constructed as:
- `http://192.168.1.100:9000/mnemocast-creatives/...` (instead of `http://localhost:9000/...`)

This makes media files accessible from any machine on your LAN.

---

## 🔧 Configuration Options

### Option 1: SERVER_HOST (Recommended - Automatic)
Sets the host for both MinIO URLs and local storage URLs.

```bash
export SERVER_HOST=192.168.1.100
```

**Benefits:**
- ✅ Works for both MinIO and local storage
- ✅ Single configuration point
- ✅ Automatically constructs correct URLs

### Option 2: MINIO_BASE_URL (Full Control)
Set the complete base URL for MinIO media serving.

```bash
export MINIO_BASE_URL=http://192.168.1.100:9000/mnemocast-creatives
```

**Benefits:**
- ✅ Full control over the URL format
- ✅ Can use different host/port than endpoint
- ✅ Can use CDN or proxy URLs

### Option 3: Both Set
`MINIO_BASE_URL` takes precedence if both are set.

---

## 🧪 Verification

### 1. Check Backend Startup Logs
When you start the backend, you should see:

```
📦 Media Storage: MinIO
   Endpoint: localhost:9000
   Bucket: mnemocast-creatives
   Use SSL: false
   Base URL: http://192.168.1.100:9000/mnemocast-creatives
```

If you see `localhost` in the Base URL, the environment variable isn't set correctly.

### 2. Test MinIO URL
After uploading a creative, check the returned URL:

```bash
# Upload a creative and check the response
curl -X POST http://192.168.1.100:8080/api/v1/creatives/upload \
  -F "file=@test.jpg"

# Response should contain:
# "url": "http://192.168.1.100:9000/mnemocast-creatives/creatives/..."
```

### 3. Test from Another Machine
```bash
# From another machine, try to access a creative URL
curl -I http://192.168.1.100:9000/mnemocast-creatives/creatives/some-file.jpg

# Should return 200 OK, not connection refused
```

### 4. Verify MinIO is Accessible
Make sure MinIO is accessible from other machines:

```bash
# Test MinIO endpoint from another machine
curl -I http://192.168.1.100:9000

# Should return MinIO response (not connection refused)
```

---

## 🔒 MinIO Server Configuration

For MinIO to be accessible from other machines, ensure:

### 1. MinIO Binds to All Interfaces
MinIO should bind to `0.0.0.0`, not just `localhost`:

```bash
# Start MinIO binding to all interfaces
minio server /data --address "0.0.0.0:9000"
```

Or in Docker:
```yaml
ports:
  - "9000:9000"
  - "9001:9001"
```

### 2. Firewall Configuration
Allow port 9000 through firewall:

```bash
# Linux (ufw)
sudo ufw allow 9000/tcp

# Linux (firewalld)
sudo firewall-cmd --add-port=9000/tcp --permanent
sudo firewall-cmd --reload

# Check if port is listening
sudo netstat -tulpn | grep 9000
```

### 3. Bucket Policy
Ensure the bucket has public read access (already configured by the code):

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {"AWS": ["*"]},
      "Action": ["s3:GetObject"],
      "Resource": ["arn:aws:s3:::mnemocast-creatives/*"]
    }
  ]
}
```

---

## 🔄 Updating Existing Creatives

**Important:** Existing creatives in the database already have URLs with `localhost`. You have two options:

### Option A: Re-upload Creatives (Recommended)
1. Set `SERVER_HOST` environment variable
2. Restart backend
3. Re-upload all creatives
4. New URLs will use the correct host

### Option B: Update Database URLs (Advanced)
If you have many creatives, you can update URLs in the database:

```sql
-- For PostgreSQL
UPDATE creatives 
SET creative_url = REPLACE(creative_url, 'localhost', '192.168.1.100')
WHERE creative_url LIKE '%localhost:9000%';
```

For Redis, you'll need to update each creative manually or use a script.

---

## 🐛 Troubleshooting

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

### Issue: MinIO connection refused from other machines

**Check:**
1. MinIO is running and accessible: `curl http://SERVER_IP:9000`
2. MinIO binds to `0.0.0.0`, not `127.0.0.1`
3. Firewall allows port 9000
4. MinIO endpoint in backend config matches server IP

**Test:**
```bash
# From server machine
curl http://localhost:9000

# From another machine
curl http://SERVER_IP:9000
```

### Issue: CORS errors when loading media

MinIO should handle CORS, but verify:
1. MinIO CORS is configured (if needed)
2. Browser console shows CORS errors (not network errors)
3. Media URLs use correct host (not localhost)

### Issue: Different MinIO host than backend server

If MinIO runs on a different machine:

```bash
# Set MINIO_BASE_URL explicitly
export MINIO_BASE_URL=http://MINIO_SERVER_IP:9000/mnemocast-creatives

# MinIO endpoint can still point to localhost if using port forwarding
export MINIO_ENDPOINT=localhost:9000
```

---

## 📝 Example: Complete Setup

```bash
# 1. Find your server IP
ip addr show | grep "inet " | grep -v 127.0.0.1
# Example output: inet 192.168.1.100/24

# 2. Start MinIO binding to all interfaces
minio server /data --address "0.0.0.0:9000"

# 3. Set environment variable
export SERVER_HOST=192.168.1.100

# 4. Verify MinIO is accessible
curl http://192.168.1.100:9000

# 5. Start backend
cd backend
sbt run

# 6. Check logs - should show:
# Base URL: http://192.168.1.100:9000/mnemocast-creatives

# 7. Upload a creative and verify URL
curl -X POST http://192.168.1.100:8080/api/v1/creatives/upload \
  -F "file=@test.jpg" | jq .url

# Should return: "http://192.168.1.100:9000/mnemocast-creatives/creatives/..."
```

---

## 🔐 Production Considerations

For production deployments:

1. **Use HTTPS:**
   ```bash
   export MINIO_USE_SSL=true
   export MINIO_BASE_URL=https://minio.yourdomain.com/mnemocast-creatives
   ```

2. **Use a CDN/Proxy:**
   ```bash
   export MINIO_BASE_URL=https://cdn.yourdomain.com/media
   ```

3. **Use Presigned URLs:**
   For better security, consider using presigned URLs instead of public bucket access.

4. **Environment-Specific Configs:**
   - Development: `localhost`
   - Staging: `staging-minio.yourdomain.com`
   - Production: `cdn.yourdomain.com`

---

## ✅ Expected Behavior After Fix

1. ✅ Backend startup shows correct MinIO Base URL (not localhost)
2. ✅ Uploaded creatives have URLs with server IP
3. ✅ Media files load from any machine on LAN
4. ✅ Dashboard displays images/videos correctly
5. ✅ No "connection refused" or "localhost" errors
6. ✅ MinIO is accessible from other machines

---

## 🔗 Related Documentation

- **Local Storage Fix:** `docs/MEDIA_URL_LAN_FIX.md`
- **Dashboard LAN Access:** `docs/DASHBOARD_LAN_ACCESS_FIX.md`
- **MinIO Setup:** `docs/MINIO_CONFIGURATION.md`

---

**Last Updated:** December 2024

