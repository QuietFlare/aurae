# Aurae with Docker Compose

Runs Aurae for a practice that cannot run k3s. Caddy serves https on one host name. Aurae, Keycloak and both databases sit behind it and publish no ports.

## Install

1. Pick the name staff will type into the browser, for example `aurae.praxis.example`. It must resolve to this server on the practice network. `aurae.localhost` works for a trial on your own machine.
2. Copy `.env.example` to `.env`, set `AURAE_HOST` to that name and fill in the passwords.
3. Create the certificate:

   ```bash
   bash make-cert.sh aurae.localhost
   ```

4. Start the stack:

   ```bash
   docker compose up -d
   ```

5. Install `tls/ca/ca.crt` as a trusted authority on every practice PC. Without it the browser warns on each visit.

## The first user

The realm ships without users. Open `https://<AURAE_HOST>/auth/admin`, sign in as `admin` with `KEYCLOAK_ADMIN_PASSWORD`, switch to the `aurae` realm, create the practice admin and assign the `admin` role. Set a temporary password under Credentials. At first sign-in Keycloak asks that person for a new password and a one-time code app.

Then create a personal admin account in the `master` realm and delete the bootstrap `admin` user.

## Your own certificate

If the practice already has a certificate for the host name, skip `make-cert.sh` and place the files yourself:

- `tls/server/server.crt` with the full chain, and `tls/server/server.key`
- `tls/ca/type` containing the word `ca-certificates`
- `tls/ca/ca.crt` with the issuing authority, if it is a private one. Aurae calls Keycloak over https and must trust the certificate.

## Changing the realm

Edit `deploy/keycloak/aurae-realm.json` and run `docker compose up -d` again. The `keycloak-config` container applies the file on every start and leaves users alone.

## Limits

- The Keycloak admin console is reachable by anyone on the practice network who knows the address. It is protected by the admin password only.
- Nothing here backs up the two database volumes.
