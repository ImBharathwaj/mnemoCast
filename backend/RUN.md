# How to Run the Backend

## The Issue

Running `sbt run` from the backend directory tries to run the **root** project, which doesn't have a main class configured for direct execution.

## Solution: Use the Correct Command

### Option 1: Run the engineApi Project Directly (Recommended)

```bash
cd backend
sbt "project engineApi" run
```

### Option 2: Use the Run Script

From the project root:
```bash
./scripts/dev-run.sh
```

### Option 3: Use sbt Shell

```bash
cd backend
sbt
> project engineApi
> run
```

## Quick Test

1. Start Redis: `redis-server`
2. Run: `sbt "project engineApi" run`
3. Test: `curl http://localhost:8080/ads/deliver`

## Why This Happens

The backend uses a **multi-project** sbt build:
- `root` - aggregates all modules (no main class)
- `engine-domain` - domain models (library)
- `engine-infra` - infrastructure (library)
- `engine-api` - HTTP server (has main class: `HttpServer`)

The main class is only in the `engine-api` project, so you need to specify which project to run.

## Alternative: Run from Root Directory

You can also run from the project root using the script:

```bash
# From project root (not backend/)
./scripts/dev-run.sh
```

This script automatically runs the correct command.

