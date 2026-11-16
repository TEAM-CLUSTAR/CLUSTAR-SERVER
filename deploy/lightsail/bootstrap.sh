#!/usr/bin/env bash
set -euo pipefail
umask 077

root=${CLUSTAR_DEPLOY_ROOT:-/opt/clustar}
source_dir=$(cd -- "$(dirname -- "$0")" && pwd)
site=${1:?Usage: bootstrap.sh DOMAIN}

if [[ ! -d $root ]]; then
  sudo -n install -d -m 0700 -o "$(id -un)" -g "$(id -gn)" "$root"
fi
mkdir -p "$root/caddy" "$root/postgres-init" "$root/incoming"
chmod 0755 "$root/caddy" "$root/postgres-init"
exec 9>"$root/deploy.lock"
flock -n 9
if [[ -e $root/caddy-recovery-required ]]; then
  echo 'Caddy route requires recovery. Follow the Lightsail recovery instructions before bootstrapping.' >&2
  exit 1
fi

if [[ ! -e $root/infra.env ]]; then
  [[ $site =~ ^[a-zA-Z0-9.-]+$ ]] || exit 2
  printf 'SITE_ADDRESS=%s\nPOSTGRES_ADMIN_PASSWORD=%s\nDB_PASSWORD=%s\n' \
    "$site" "$(openssl rand -hex 32)" "$(openssl rand -hex 32)" > "$root/infra.env"
fi

install -m 0644 "$source_dir/Caddyfile" "$root/caddy/Caddyfile"
install -m 0755 "$source_dir/init-db.sh" "$root/postgres-init/10-clustar.sh"
install -m 0644 "$source_dir/compose.yaml" "$root/compose.yaml"
install -m 0700 "$source_dir/deploy.sh" "$root/deploy.sh"
if [[ ! -e $root/caddy/route.caddy ]]; then
  printf '# active: none\nrespond "Application not deployed yet" 503\n' > "$root/caddy/route.caddy"
fi

sudo -n docker compose --project-directory "$root" --env-file "$root/infra.env" \
  up -d --wait --wait-timeout 150 postgres redis caddy
