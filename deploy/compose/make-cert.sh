#!/usr/bin/env bash
# Creates a private certificate authority and a server certificate for one host name, for a
# practice network where no public authority can issue one. Git ignores the tls folder.
#   tls/ca/ca.crt       install on every practice PC so browsers trust Aurae. Aurae trusts it too.
#   tls/server/         certificate and key that Caddy serves
#   tls/ca.key          signs new server certificates. Keep it off the network.
# Existing files are kept. Pass --force as second argument to replace them.
set -euo pipefail

cd "$(dirname "$0")"
host=${1:?usage: make-cert.sh <host name> [--force]}

if [[ -f tls/server/server.key && ${2:-} != --force ]]; then
  echo "Certificates already exist. Run with --force to replace them."
  exit 0
fi

mkdir -p tls/ca tls/server
# Aurae's image reads this folder as a set of extra trusted certificates.
printf 'ca-certificates' > tls/ca/type

ca_config=$(mktemp)
extensions=$(mktemp)
trap 'rm -f "$ca_config" "$extensions" tls/server/server.csr' EXIT

# Spelled out in a file, because OpenSSL and the LibreSSL on macOS differ in their defaults.
cat > "$ca_config" <<CONFIG
[req]
distinguished_name = dn
x509_extensions = ca
prompt = no
[dn]
CN = Aurae CA for $host
[ca]
basicConstraints = critical,CA:TRUE
keyUsage = critical,keyCertSign,cRLSign
CONFIG
openssl ecparam -name prime256v1 -genkey -noout -out tls/ca.key
openssl req -x509 -new -key tls/ca.key -sha256 -days 3650 -config "$ca_config" -out tls/ca/ca.crt

# Browsers check the alternative name, not the subject. An IP address needs its own kind of entry.
if [[ $host =~ ^[0-9.]+$ ]]; then san="IP:$host"; else san="DNS:$host"; fi
printf 'subjectAltName=%s\nextendedKeyUsage=serverAuth\nbasicConstraints=CA:FALSE\n' "$san" > "$extensions"

openssl ecparam -name prime256v1 -genkey -noout -out tls/server/server.key
openssl req -new -key tls/server/server.key -subj "/CN=$host" -out tls/server/server.csr
# Apple devices reject server certificates valid for more than 825 days.
openssl x509 -req -in tls/server/server.csr -CA tls/ca/ca.crt -CAkey tls/ca.key -CAcreateserial \
  -sha256 -days 825 -extfile "$extensions" -out tls/server/server.crt
rm -f tls/ca/ca.srl

chmod 600 tls/ca.key tls/server/server.key
echo "Written tls/ca/ca.crt and tls/server/ for $host."
