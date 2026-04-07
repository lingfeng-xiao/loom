# loom-node

`loom-node` is the read-only runtime agent for Loom.

## What it does

- Registers with the Loom server
- Sends periodic heartbeats
- Collects real CPU, memory, disk, and service probe data
- Keeps the local `nodeId` on disk so restarts do not trigger duplicate registration

## Configuration

Key environment variables:

- `LOOM_SERVER_BASE_URL`
- `LOOM_SERVER_TOKEN`
- `LOOM_NODE_NAME`
- `LOOM_NODE_TYPE`
- `LOOM_NODE_HOST`
- `LOOM_NODE_STATE_DIR`
- `LOOM_NODE_HEARTBEAT_INTERVAL_MS`
- `LOOM_NODE_HEARTBEAT_INITIAL_DELAY_MS`
- `LOOM_SERVER_HEALTH_URL`
- `LOOM_WEB_HEALTH_URL`

The default configuration uses structured probes:

- `loom-server` over HTTP
- `loom-web` over HTTP

If you run the node outside the compose network, override the probe URLs to point at reachable hosts.

## Runtime State

The node persists a small properties file under the configured state directory. It stores:

- last registered node id
- last registration time
- last heartbeat time
- last error message

This file is what prevents duplicate registration after a restart.

## Phase 1 Rules

- Read-only only
- No shell execution
- No deployment automation
- No remote command dispatch
