#!/usr/bin/env bash
set -euo pipefail
umask 077

root=${CLUSTAR_DEPLOY_ROOT:-/opt/clustar}
mode=${1:?Usage: deploy.sh release SHA ARCHIVE RUNTIME_ENV | rollback}
compose_file=$(cd -- "$(dirname -- "$0")" && pwd)/compose.yaml
route="$root/caddy/route.caddy"
source "$root/infra.env"
edge_url=${EDGE_READINESS_URL:-https://$SITE_ADDRESS/actuator/health/readiness}
edge_options=()
if [[ -z ${EDGE_READINESS_URL:-} ]]; then
  edge_options=(--resolve "$SITE_ADDRESS:443:127.0.0.1")
fi

exec 9>"$root/deploy.lock"
flock -n 9 || { echo 'Another deployment is running' >&2; exit 1; }
recovery_marker="$root/caddy-recovery-required"
if [[ -e $recovery_marker ]]; then
  echo 'Caddy route requires recovery. Follow the Lightsail recovery instructions before deploying.' >&2
  exit 1
fi

dc() {
  sudo -n env BLUE_IMAGE="${BLUE_IMAGE:-}" GREEN_IMAGE="${GREEN_IMAGE:-}" APP_ENV_FILE="${APP_ENV_FILE:-}" \
    docker compose --project-directory "$root" --env-file "$root/infra.env" -f "$compose_file" "$@"
}

reload_proxy() {
  dc exec -T caddy caddy reload --config /etc/caddy/Caddyfile --adapter caddyfile
}

route_matches() {
  local slot=$1 headers status
  headers=$(mktemp "$root/.route-check.XXXXXXXX") || return 1
  status=$(curl -sS --max-time 5 "${edge_options[@]}" -D "$headers" -o /dev/null \
    -w '%{http_code}' "$edge_url") || { rm -f -- "$headers"; return 1; }
  if [[ $slot == none ]]; then
    [[ $status == 503 ]] && ! tr -d '\r' < "$headers" | grep -qi '^X-Clustar-Slot:'
  else
    [[ $status == 200 ]] && tr -d '\r' < "$headers" | grep -Fix "X-Clustar-Slot: $slot"
  fi
  local result=$?
  rm -f -- "$headers"
  return "$result"
}

stop_slot() {
  local container
  container=$(dc ps -aq "$1")
  if [[ -n $container ]]; then
    sudo -n docker stop --time 65 "$container" >/dev/null
  fi
}

active=$(sed -n '1s/^# active: //p' "$route")
case "$active" in
  app-blue) next=app-green; port=${GREEN_PORT:-18082} ;;
  app-green|none) next=app-blue; port=${BLUE_PORT:-18081} ;;
  *) echo 'Unknown active slot' >&2; exit 1 ;;
esac

previous_route=$(mktemp "$root/.previous-route.XXXXXXXX")
candidate_route=$(mktemp "$root/.candidate-route.XXXXXXXX")
cp "$route" "$previous_route"
started=0
route_changed=0
verified=0

finish() {
  local status=$?
  trap - EXIT
  if (( status != 0 && verified == 0 )); then
    if (( route_changed )); then
      if mv -f -- "$previous_route" "$route" && reload_proxy && route_matches "$active"; then
        if rm -f -- "$recovery_marker"; then
          echo "Restored Caddy route to $active" >&2
        else
          echo 'Caddy was restored but the recovery marker could not be removed.' >&2
          started=0
        fi
      else
        echo 'Caddy rollback failed. Both slots were left available for recovery.' >&2
        if [[ -f $previous_route ]]; then
          echo "Previous route saved at $previous_route" >&2
          previous_route=''
        fi
        started=0
      fi
    fi
    if (( started )); then
      stop_slot "$next" || true
    fi
  fi
  [[ -z $previous_route ]] || rm -f -- "$previous_route"
  rm -f -- "$candidate_route"
  exit "$status"
}
trap finish EXIT
trap 'exit 130' INT
trap 'exit 143' TERM

case "$mode" in
  release)
    revision=${2:?Revision required}
    archive=${3:?Image archive required}
    APP_ENV_FILE=${4:?Runtime env file required}
    [[ $revision =~ ^[0-9a-f]{40}$ && -s $archive && -s $APP_ENV_FILE ]]
    image="clustar:$revision"
    if [[ $next == app-blue ]]; then BLUE_IMAGE=$image; else GREEN_IMAGE=$image; fi
    sudo -n docker load --input "$archive" >/dev/null
    sudo -n docker image inspect "$image" --format '{{.Id}}' >/dev/null
    started=1
    dc up -d --no-deps --pull never "$next"
    ;;
  rollback)
    [[ $active != none ]]
    previous=$(dc ps -aq "$next")
    [[ -n $previous ]] || { echo 'No previous container available' >&2; exit 1; }
    started=1
    sudo -n docker start "$previous" >/dev/null
    ;;
  *) echo "Unknown mode: $mode" >&2; exit 2 ;;
esac

ready=0
for ((attempt=1; attempt<=60; attempt++)); do
  if curl -fsS --max-time 3 "http://127.0.0.1:$port/actuator/health/readiness" >/dev/null 2>&1; then
    ready=1
    break
  fi
  sleep 2
done
(( ready )) || { echo "$next did not become ready" >&2; exit 1; }

printf '# active: %s\nheader /actuator/health/readiness X-Clustar-Slot %s\nreverse_proxy %s:8080 {\n    stream_close_delay 30s\n}\n' \
  "$next" "$next" "$next" > "$candidate_route"
printf 'Caddy route transition requires verification.\n' > "$recovery_marker"
route_changed=1
mv -f -- "$candidate_route" "$route"
reload_proxy

for ((attempt=1; attempt<=5; attempt++)); do
  if curl -fsS --max-time 5 "${edge_options[@]}" \
      -D - -o /dev/null "$edge_url" | tr -d '\r' | grep -Fix "X-Clustar-Slot: $next"; then
    verified=1
    break
  fi
  sleep 1
done
(( verified )) || { echo 'Caddy readiness check failed' >&2; exit 1; }
rm -f -- "$recovery_marker"

if [[ $active != none ]]; then
  sleep 30
  stop_slot "$active"
fi
echo "Serving traffic from $next"
