# MinIO Storage Configuration

This document explains how to configure the Mnemocast engine to use MinIO for storing creative media files instead of the local filesystem.

## Overview

MinIO is an S3-compatible object storage server. The system supports both local filesystem storage and MinIO storage, configurable via environment variables.

## Prerequisites

1. MinIO server running (default: `localhost:9000`)
2. MinIO access credentials (default dev credentials: `minioadmin`/`minioadmin`)

## Configuration

### Environment Variables

The system uses the following environment variables to configure MinIO storage:

| Variable | Description | Default Value |
|----------|-------------|---------------|
| `MEDIA_STORAGE_TYPE` | Storage type: `"minio"` or `"local"` | `"minio"` |
| `MINIO_ENDPOINT` | MinIO server endpoint (host:port) | `"localhost:9000"` |
| `MINIO_ACCESS_KEY` | MinIO access key | `"minioadmin"` |
| `MINIO_SECRET_KEY` | MinIO secret key | `"minioadmin"` |
| `MINIO_BUCKET` | Bucket name for storing creatives | `"mnemocast-creatives"` |
| `MINIO_USE_SSL` | Whether to use SSL/TLS | `"false"` |
| `MINIO_BASE_URL` | Optional: Custom base URL for serving files (for CDN/proxy) | None (uses endpoint) |

### Example Configuration

#### Using MinIO (default)
```bash
export MEDIA_STORAGE_TYPE=minio
export MINIO_ENDPOINT=localhost:9000
export MINIO_ACCESS_KEY=minioadmin
export MINIO_SECRET_KEY=minioadmin
export MINIO_BUCKET=mnemocast-creatives
export MINIO_USE_SSL=false
```

#### Using Local Filesystem
```bash
export MEDIA_STORAGE_TYPE=local
export STORAGE_BASE_PATH=storage/uploads
export STORAGE_BASE_URL=http://localhost:8080/api/v1/media
```

## Bucket Setup

The system automatically creates the bucket if it doesn't exist when the server starts. The bucket is configured with a public read policy to allow direct access to media files.

**Note**: For production deployments, consider using presigned URLs instead of public bucket policies for better security.

## File Organization

Files are organized in MinIO as follows:

```
mnemocast-creatives/
  ├── creatives/
  │   ├── {uuid}.{ext}           # Files without creativeId
  │   └── {creativeId}/
  │       └── {uuid}.{ext}       # Files with creativeId
```

## File Serving

The system currently proxies files through the application server. When a file is requested:

1. The file is downloaded from MinIO to a temporary file
2. The file is served through the `/api/v1/media/creatives/{path}` endpoint
3. Appropriate content type and caching headers are set

**Future Enhancement**: Direct serving via MinIO URLs or presigned URLs for better performance.

## Testing

1. Start MinIO server (if not already running):
   ```bash
   minio server ~/minio-data
   ```

2. Start the Mnemocast engine with MinIO configuration:
   ```bash
   export MEDIA_STORAGE_TYPE=minio
   sbt "project engineApi" run
   ```

3. Upload a creative through the dashboard or API

4. Verify the file appears in the MinIO bucket:
   - Access MinIO console at `http://localhost:9000`
   - Login with your credentials
   - Navigate to the `mnemocast-creatives` bucket
   - Check the `creatives/` folder

## Troubleshooting

### Endpoint Validation Error

**Error**: `invalid hostname 127.0.0.1:9000` or `invalid hostname localhost:9000`

**Cause**: MinIO Java client 8.5.7 has a known bug where the `endpoint()` method validates the entire "hostname:port" string as a hostname, which fails validation.

**Solutions**:

1. **Use hostname instead of IP** (if possible):
   ```bash
   export MINIO_ENDPOINT=your-hostname:9000
   ```

2. **Run MinIO on standard ports** (80 for HTTP, 443 for HTTPS):
   - Configure MinIO to use port 80: `minio server --address :80 /data`
   - Then set: `export MINIO_ENDPOINT=localhost` (or use just hostname)

3. **Use MinIO behind a reverse proxy**:
   - Run MinIO on port 9000 internally
   - Configure nginx/traefik to proxy port 80 → 9000
   - Connect to MinIO via the proxy on port 80

4. **Downgrade MinIO Java client** (if needed):
   - Try using an older version of MinIO Java client that doesn't have this validation issue
   - Update `build.sbt`: `"io.minio" % "minio" % "8.4.2"` (example)

**Note**: The server will start successfully due to lazy initialization, but uploads will fail until this is resolved.

### Connection Issues

If you see connection errors:
- Verify MinIO is running: `curl http://localhost:9000/minio/health/live`
- Check endpoint configuration (no `http://` prefix)
- Verify credentials match your MinIO setup
- Check for the endpoint validation error above

### Bucket Creation Issues

If bucket creation fails:
- Check MinIO permissions
- Verify bucket name doesn't contain invalid characters
- Check MinIO logs for detailed error messages

### File Access Issues

If uploaded files aren't accessible:
- Check bucket policy (should allow public read)
- Verify the file exists in MinIO console
- Check application logs for download errors

## Production Considerations

1. **Security**: Use strong credentials, enable SSL, and consider using presigned URLs instead of public bucket policies

2. **Performance**: Consider using a CDN in front of MinIO or direct MinIO URLs for file serving

3. **Backup**: Implement backup strategies for MinIO data

4. **Monitoring**: Monitor MinIO server health and disk usage

5. **Scalability**: MinIO supports distributed mode for high availability and scalability

