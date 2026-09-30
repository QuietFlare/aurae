#!/usr/bin/env bash
# Generates the local-only secrets for development. Git ignores both output files:
#   api/keycloak/aurae-realm.json    the Keycloak realm with a client secret and test user passwords
#   api/config/application-dev.yaml  the same client secret, for Spring
# Existing files are kept. Pass --force to generate new values, then recreate Keycloak.
set -euo pipefail

cd "$(dirname "$0")/.."
realm=api/keycloak/aurae-realm.json
settings=api/config/application-dev.yaml

if [[ -f $realm && -f $settings && ${1:-} != --force ]]; then
  echo "Secrets already exist. Run with --force to replace them."
  exit 0
fi

secret() { printf 'dev-%s' "$(openssl rand -hex 16)"; }
client_secret=$(secret)

sed -e "s/__CLIENT_SECRET__/$client_secret/" \
    -e "s/__MFA_PASSWORD__/$(secret)/" \
    -e "s/__GP_PASSWORD__/$(secret)/" \
    -e "s/__ADMIN_PASSWORD__/$(secret)/" \
    api/keycloak/aurae-realm.json.template > "$realm"

mkdir -p api/config
printf 'aurae:\n  oidc:\n    client-secret: %s\n' "$client_secret" > "$settings"
chmod 600 "$realm" "$settings"

echo "Written $realm and $settings."
echo "Test user passwords are in $realm. Recreate Keycloak to load them:"
echo "  cd api && docker compose up -d --force-recreate keycloak"
