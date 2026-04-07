# Template Node Agent

`apps/node` is the generic agent that:

- registers itself with `template-server`
- executes HTTP probes defined in `template.node.service-probes`
- sends periodic heartbeats with the current probe state
- persists the assigned node id under `TEMPLATE_NODE_STATE_DIR`

## Default Probes

- `template-server -> http://template-server:8080/api/health`
- `template-web -> http://template-web/`

## Main Settings

- `TEMPLATE_NODE_NAME`
- `TEMPLATE_NODE_TYPE`
- `TEMPLATE_NODE_HOST`
- `TEMPLATE_SERVER_BASE_URL`
- `TEMPLATE_SERVER_TOKEN`
- `TEMPLATE_NODE_HEARTBEAT_INTERVAL_MS`
- `TEMPLATE_NODE_HEARTBEAT_INITIAL_DELAY_MS`
- `TEMPLATE_NODE_STATE_DIR`

Add more probe targets in `apps/node/src/main/resources/application.yml` or override them through `SPRING_APPLICATION_JSON`.
