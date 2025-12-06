# Mnemocast Engine Backend

Core ad serving engine for Out-of-Home (OOH) advertising.

## Module Structure

- **engine-domain**: Pure domain models and business logic (no HTTP/DB dependencies)
- **engine-infra**: Persistence stores, configuration, and wiring
- **engine-api**: Pekko HTTP server, routes, and JSON formats
- **shared-kernel**: Shared types and utilities (future)

## Getting Started

See the main [README.md](../README.md) and [docs/](../docs/) for project documentation.

