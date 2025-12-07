# Fix for "sbt run" Error

## The Problem

When you run:
```bash
cd backend
sbt run
```

You get:
```
[error] No main class detected.
```

## Why This Happens

The backend uses a **multi-project** sbt build:
- `root` project - aggregates all modules (no executable main class)
- `engine-api` project - contains the actual HTTP server (has main class)

When you run `sbt run`, it tries to run the **root** project by default, which doesn't have a runnable main class.

## Solutions

### ✅ Solution 1: Specify the Project (Recommended)

```bash
cd backend
sbt "project engineApi" run
```

### ✅ Solution 2: Use the Run Script

From the **project root** (not backend directory):
```bash
./scripts/dev-run.sh
```

This script automatically runs the correct command.

### ✅ Solution 3: Use sbt Interactive Shell

```bash
cd backend
sbt
> project engineApi
> run
```

## Quick Reference

| Command | Works? | Notes |
|---------|--------|-------|
| `sbt run` | ❌ No | Tries to run root project |
| `sbt "project engineApi" run` | ✅ Yes | Explicitly runs engineApi |
| `./scripts/dev-run.sh` | ✅ Yes | Script handles it automatically |

## Updated Documentation

The run script has been updated to use the correct command. Use:
- `./scripts/dev-run.sh` from project root, OR
- `sbt "project engineApi" run` from backend directory

