#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd -P)"
RELEASE_ID="${1:-${RELEASE_ID:-$(date -u +"%Y%m%d-%H%M%S")}}"
RELEASE_DIR="${ROOT}/.release/${RELEASE_ID}"
LOG_FILE="${RELEASE_DIR}/healthcheck.log"
EVIDENCE_FILE="${RELEASE_DIR}/evidence.json"
COMPOSE_FILE="${ROOT}/docker-compose.yml"
ENV_FILE="${ROOT}/.env"

mkdir -p "$RELEASE_DIR"
export PATH="$HOME/.npm-global/bin:/usr/local/bin:/usr/bin:/bin:$PATH"

SERVER_PORT="${LOOM_SERVER_PORT:-8080}"
PUBLIC_PORT="${LOOM_PUBLIC_PORT:-80}"
MAX_ATTEMPTS="${LOOM_HEALTHCHECK_ATTEMPTS:-30}"
SLEEP_SECONDS="${LOOM_HEALTHCHECK_SLEEP_SECONDS:-2}"

wait_for_url() {
  local name="$1"
  local url="$2"
  local attempt=1

  while (( attempt <= MAX_ATTEMPTS )); do
    if curl -fsS "$url" >/dev/null; then
      echo "[healthcheck] ${name}=ok attempt=${attempt}"
      return 0
    fi

    echo "[healthcheck] ${name}=retry attempt=${attempt}/${MAX_ATTEMPTS}"
    sleep "$SLEEP_SECONDS"
    attempt=$((attempt + 1))
  done

  echo "[healthcheck] ${name}=failed"
  return 1
}

content_type_for() {
  curl -fsSI "$1" | awk 'BEGIN{IGNORECASE=1} /^content-type:/ {sub(/\r$/, ""); print $0; exit}'
}

assert_asset_type() {
  local asset="$1"
  local content_type="$2"
  case "$asset" in
    *.js) grep -Eiq 'content-type: *(application|text)/javascript' <<<"$content_type" || return 1 ;;
    *.css) grep -Eiq 'content-type: *text/css' <<<"$content_type" || return 1 ;;
    *) return 0 ;;
  esac
}

image_id() {
  docker image inspect --format '{{.Id}}' "$1"
}

container_image_id() {
  local service="$1"
  local container_id
  container_id="$(docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" ps -q "$service")"
  [[ -n "$container_id" ]] || return 1
  docker inspect --format '{{.Image}}' "$container_id"
}

{
  echo "[healthcheck] repo=$ROOT"
  echo "[healthcheck] release_id=$RELEASE_ID"
  date -u +"[healthcheck] started_at=%Y-%m-%dT%H:%M:%SZ"
  echo "[healthcheck] server_port=$SERVER_PORT"
  echo "[healthcheck] public_port=$PUBLIC_PORT"
  echo "[healthcheck] attempts=$MAX_ATTEMPTS"
  echo "[healthcheck] sleep_seconds=$SLEEP_SECONDS"
  docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" ps
  echo
  docker ps --format '{{.Names}}\t{{.Image}}\t{{.Status}}'
  echo

  wait_for_url "server_actuator_health" "http://127.0.0.1:${SERVER_PORT}/actuator/health"
  wait_for_url "server_api_health" "http://127.0.0.1:${SERVER_PORT}/api/health"
  server_health="$(curl -fsS "http://127.0.0.1:${SERVER_PORT}/api/health")"
  echo "$server_health"
  grep -Fq '"service":"loom-server"' <<<"$server_health" || { echo "[healthcheck] api health does not report loom-server" >&2; exit 1; }
  echo

  wait_for_url "public_root" "http://127.0.0.1:${PUBLIC_PORT}/"
  root_html="$(curl -fsS "http://127.0.0.1:${PUBLIC_PORT}/")"
  grep -Eiq '<title>[[:space:]]*Loom[[:space:]]*</title>' <<<"$root_html" || { echo "[healthcheck] public root title is not Loom" >&2; exit 1; }
  echo "[healthcheck] public_root_title=Loom"

  mapfile -t assets < <(grep -Eo '(src|href)="/assets/[^"]+\.(js|css)"' <<<"$root_html" | sed -E 's/^(src|href)="([^"]+)"$/\2/' | sort -u)
  if [[ ${#assets[@]} -eq 0 ]]; then
    echo "[healthcheck] no built JS/CSS assets found in root HTML" >&2
    exit 1
  fi

  asset_json="[]"
  for asset in "${assets[@]}"; do
    url="http://127.0.0.1:${PUBLIC_PORT}${asset}"
    curl -fsS "$url" >/dev/null
    content_type="$(content_type_for "$url")"
    assert_asset_type "$asset" "$content_type" || { echo "[healthcheck] unexpected content type for $asset: $content_type" >&2; exit 1; }
    echo "[healthcheck] asset=$asset $content_type"
  done

  declare -A expected_images=(
    [loom-server]=loom-loom-server
    [loom-web]=loom-loom-web
    [loom-node]=loom-loom-node
  )
  image_json_entries=()
  for service in loom-server loom-web loom-node; do
    built_id="$(image_id "${expected_images[$service]}")"
    running_id="$(container_image_id "$service")"
    echo "[healthcheck] image service=$service built=$built_id running=$running_id"
    [[ "$built_id" == "$running_id" ]] || { echo "[healthcheck] running image id differs for $service" >&2; exit 1; }
    image_json_entries+=("{\"service\":\"$service\",\"image\":\"${expected_images[$service]}\",\"built_image_id\":\"$built_id\",\"container_image_id\":\"$running_id\"}")
  done

  finished_at="$(date -u +"%Y-%m-%dT%H:%M:%SZ")"
  printf '{\n' > "$EVIDENCE_FILE"
  printf '  "release_id": "%s",\n' "$RELEASE_ID" >> "$EVIDENCE_FILE"
  printf '  "repo_head": "%s",\n' "$(git -C "$ROOT" rev-parse HEAD)" >> "$EVIDENCE_FILE"
  printf '  "finished_at": "%s",\n' "$finished_at" >> "$EVIDENCE_FILE"
  printf '  "server_health": %s,\n' "$server_health" >> "$EVIDENCE_FILE"
  printf '  "public_root": {"url":"http://127.0.0.1:%s/","title":"Loom"},\n' "$PUBLIC_PORT" >> "$EVIDENCE_FILE"
  printf '  "assets": [' >> "$EVIDENCE_FILE"
  first=true
  for asset in "${assets[@]}"; do
    if [[ "$first" == true ]]; then first=false; else printf ',' >> "$EVIDENCE_FILE"; fi
    printf '\n    {"path":"%s"}' "$asset" >> "$EVIDENCE_FILE"
  done
  printf '\n  ],\n' >> "$EVIDENCE_FILE"
  printf '  "container_images": [' >> "$EVIDENCE_FILE"
  first=true
  for entry in "${image_json_entries[@]}"; do
    if [[ "$first" == true ]]; then first=false; else printf ',' >> "$EVIDENCE_FILE"; fi
    printf '\n    %s' "$entry" >> "$EVIDENCE_FILE"
  done
  printf '\n  ]\n}\n' >> "$EVIDENCE_FILE"
  echo "[healthcheck] evidence=$EVIDENCE_FILE"
  date -u +"[healthcheck] finished_at=%Y-%m-%dT%H:%M:%SZ"
} 2>&1 | tee "$LOG_FILE"
