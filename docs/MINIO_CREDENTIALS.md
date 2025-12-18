# MinIO Credentials Storage

## Current Implementation

MinIO credentials are **not stored in any file**. They are read from **environment variables** at runtime when the server starts.

### Location in Code

The credentials are read in `backend/modules/engine-api/src/main/scala/mnemocast/engine/api/HttpServer.scala`:

```scala
val minioAccessKey = sys.env.getOrElse("MINIO_ACCESS_KEY", "minioadmin")
val minioSecretKey = sys.env.getOrElse("MINIO_SECRET_KEY", "minioadmin")
```

### Default Values

If environment variables are not set, the system uses these **hardcoded defaults**:
- Access Key: `"minioadmin"`
- Secret Key: `"minioadmin"`

**⚠️ WARNING**: These are insecure default credentials meant only for development. Never use these in production!

## How to Set Credentials

### Option 1: Environment Variables (Current Method)

Set environment variables before running the server:

```bash
export MINIO_ACCESS_KEY=your-access-key
export MINIO_SECRET_KEY=your-secret-key
export MINIO_ENDPOINT=localhost:9000
export MINIO_BUCKET=mnemocast-creatives

# Then run the server
sbt "project engineApi" run
```

### Option 2: Create a `.env` File (Recommended for Development)

Create a `.env` file in the project root:

```bash
# .env
MINIO_ENDPOINT=localhost:9000
MINIO_ACCESS_KEY=your-access-key
MINIO_SECRET_KEY=your-secret-key
MINIO_BUCKET=mnemocast-creatives
MINIO_USE_SSL=false
MEDIA_STORAGE_TYPE=minio
```

**Note**: The current implementation doesn't automatically load `.env` files. You'll need to:
1. Use a tool like `direnv` or `dotenv` to load the file, OR
2. Source it manually: `source .env && sbt "project engineApi" run`

### Option 3: Shell Script Wrapper

Create a script to load environment variables:

```bash
#!/bin/bash
# scripts/run-with-minio.sh

export MINIO_ENDPOINT=localhost:9000
export MINIO_ACCESS_KEY=your-access-key
export MINIO_SECRET_KEY=your-secret-key
export MINIO_BUCKET=mnemocast-creatives
export MEDIA_STORAGE_TYPE=minio

cd backend
sbt "project engineApi" run
```

## Production Recommendations

For production environments, consider:

1. **Secret Management Services**:
   - AWS Secrets Manager
   - HashiCorp Vault
   - Kubernetes Secrets
   - Docker Secrets

2. **Configuration Management**:
   - Use a proper configuration library (e.g., Typesafe Config)
   - Store credentials outside the codebase
   - Use separate config files per environment

3. **Environment-Specific Files**:
   - `application.conf` for development
   - `application.prod.conf` for production (not in git)
   - Load from secure vaults in production

## Security Best Practices

1. **Never commit credentials to git** - Add `.env` to `.gitignore`
2. **Use strong passwords** - Don't use default `minioadmin` credentials
3. **Rotate credentials regularly** - Especially if exposed
4. **Use IAM policies** - Restrict MinIO access to minimal required permissions
5. **Use SSL/TLS** - Set `MINIO_USE_SSL=true` for encrypted connections
6. **Use presigned URLs** - Instead of public bucket policies for file access

## Current Status

✅ **Working**: Credentials read from environment variables  
⚠️ **Insecure Defaults**: Hardcoded defaults are for development only  
📝 **No Persistence**: Credentials are not saved anywhere - only in environment  
🔐 **Production Ready**: No - needs proper secret management for production

