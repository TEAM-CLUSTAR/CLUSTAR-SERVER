#!/usr/bin/env bash
set -euo pipefail
umask 077

source_dir=$(cd -- "$(dirname -- "$0")" && pwd)
root=/opt/clustar-monitoring
sudo -n install -d -m 0700 -o "$(id -un)" -g "$(id -gn)" "$root"
exec 9>/opt/clustar/deploy.lock
flock -n 9 || { echo 'An application deployment is running' >&2; exit 1; }
if [[ -e /opt/clustar/caddy-recovery-required ]]; then
  echo 'Resolve the Caddy recovery marker before installing monitoring' >&2
  exit 1
fi
role_exists=$(sudo -n docker exec clustar-postgres-1 psql -U postgres -d clustar -Atc \
  "SELECT 1 FROM pg_roles WHERE rolname='clustar_monitor'")
if [[ ! -f $root/monitoring.env ]]; then
  if [[ $role_exists == 1 ]]; then
    echo 'Monitoring role exists but monitoring.env is missing; restore the original credentials first' >&2
    exit 1
  fi
  : "${GRAFANA_DOMAIN:?Set GRAFANA_DOMAIN to the public Grafana hostname}"
  [[ $GRAFANA_DOMAIN =~ ^[a-zA-Z0-9.-]+$ ]] || { echo 'Invalid Grafana hostname' >&2; exit 1; }
  printf 'GRAFANA_DOMAIN=%s\nGRAFANA_ADMIN_PASSWORD=%s\nPOSTGRES_MONITOR_PASSWORD=%s\n' \
    "$GRAFANA_DOMAIN" "$(openssl rand -hex 24)" "$(openssl rand -hex 24)" > "$root/monitoring.env"
fi
source "$root/monitoring.env"
[[ ${GRAFANA_DOMAIN:-} =~ ^[a-zA-Z0-9.-]+$ ]] || { echo 'Invalid or missing GRAFANA_DOMAIN' >&2; exit 1; }
if [[ $role_exists == 1 ]]; then
  echo 'Monitoring DB role already exists; preserving its password'
else
  [[ $POSTGRES_MONITOR_PASSWORD =~ ^[a-f0-9]{48}$ ]]
  printf "BEGIN;\nCREATE ROLE clustar_monitor LOGIN PASSWORD '%s';\nGRANT CONNECT ON DATABASE clustar TO clustar_monitor;\nGRANT pg_monitor TO clustar_monitor;\nCOMMIT;\n" \
    "$POSTGRES_MONITOR_PASSWORD" | sudo -n docker exec -i clustar-postgres-1 psql -U postgres -d clustar -v ON_ERROR_STOP=1 -q
fi
cp -R "$source_dir/grafana" "$root/"
find "$root/grafana" -name '._*' -type f -delete
chmod -R a+rX "$root/grafana"
install -m 0644 "$source_dir/compose.yaml" "$root/compose.yaml"
install -m 0644 "$source_dir/prometheus.yml" "$root/prometheus.yml"
sudo -n docker compose --project-directory "$root" --env-file "$root/monitoring.env" up -d
# Alert provisioning is loaded at Grafana startup, even if Compose did not recreate it.
sudo -n docker compose --project-directory "$root" --env-file "$root/monitoring.env" restart grafana

install -d -m 0755 /opt/clustar/caddy/grafana
config=/opt/clustar/caddy/Caddyfile
backup=$(mktemp /opt/clustar/caddy/Caddyfile.monitoring.XXXXXX)
cp "$config" "$backup"
snippet=/opt/clustar/caddy/grafana/monitoring.caddy
snippet_backup=$(mktemp /opt/clustar/caddy/grafana-backup.XXXXXX)
had_snippet=false
if [[ -f $snippet ]]; then cp "$snippet" "$snippet_backup"; had_snippet=true; fi
printf 'Caddy monitoring configuration requires verification.\n' > /opt/clustar/caddy-recovery-required
printf '%s {\n    reverse_proxy grafana:3000\n}\n' "$GRAFANA_DOMAIN" > "$snippet"
install -m 0644 "$source_dir/../lightsail/Caddyfile" "$config"
if ! sudo -n docker exec clustar-caddy-1 caddy validate --config /etc/caddy/Caddyfile --adapter caddyfile || \
   ! sudo -n docker exec clustar-caddy-1 caddy reload --config /etc/caddy/Caddyfile --adapter caddyfile; then
  cp "$backup" "$config"
  if $had_snippet; then cp "$snippet_backup" "$snippet"; else rm -f "$snippet"; fi
  if sudo -n docker exec clustar-caddy-1 caddy reload --config /etc/caddy/Caddyfile --adapter caddyfile; then
    rm -f /opt/clustar/caddy-recovery-required
  else
    echo 'Caddy rollback failed; inspect its configuration before deploying' >&2
  fi
  rm -f "$backup" "$snippet_backup"
  exit 1
fi
rm -f /opt/clustar/caddy-recovery-required "$backup" "$snippet_backup"
