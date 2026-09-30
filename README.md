# Aurae

Self-hosted lab results platform for GP practices. Aurae reads lab reports the practice did not order, from hospitals, specialists and labs abroad, arriving on paper, by fax or as PDFs. It turns them into structured values with LOINC codes, units and reference ranges and writes them into the practice software as LDT, so they sit next to the results the practice ordered. All data stays on the practice's own server.

## Status

Early development. Sign-in and access control exist. Reading reports, the LDT write-back and the k3s deployment are not built yet. Aurae is not ready for use with real patient data.

## What Aurae does not do

Aurae organises and displays lab data. It shows whether a value lies inside or outside the reference range of the lab that measured it. It does not interpret results, diagnose or recommend treatment. Those decisions stay with the clinician.

## Stack

React with TypeScript on Vite, Kotlin, Spring Boot, PostgreSQL, Docker, Keycloak and Kubernetes (k3s).

The application builds to one container image, made to run unchanged under Docker Compose and under k3s. All configuration comes in through environment variables, and Spring Actuator provides the health endpoints k3s uses as probes. The two environments differ only on the platform side: secrets, TLS and ingress, and volumes.

The Compose deployment exists today and is the fallback for a practice that cannot run k3s. The k3s deployment is the main target and is not built yet.

## Running it

- [docs/development.md](docs/development.md) for local development.
- [deploy/compose/README.md](deploy/compose/README.md) for a full installation with Docker Compose.

## Identity and access

Aurae signs users in with OpenID Connect on OAuth 2.0, using the authorization code flow with PKCE that current OAuth security guidance (RFC 9700) recommends. It works with any OpenID Connect provider. Keycloak ships as the default, and the same build has signed users in through Amazon Cognito in development with no code changes.

- The backend performs the sign-in and keeps all tokens on the server. The browser only holds an HttpOnly session cookie that page scripts cannot read.
- Aurae never sees a password. Users sign in on the provider's page, so two-factor login and a clinic's existing Microsoft or Active Directory accounts keep working.
- Endpoints check named permissions, never role names. Which role holds which permission, and where the provider puts roles in its token, is configuration.
- Every sign-in asks for the password, and an idle session ends after 15 minutes, because practice PCs are shared.
- There is no self-registration. For now an admin creates each account in the Keycloak console. An invite screen inside Aurae is planned.
- The bundled Keycloak asks every new user to set up a one-time code app and requires passwords of 12 characters or more. A clinic that brings its own provider sets these rules there.
- An installation serves https only. A reverse proxy holds the certificate, and Keycloak runs in production mode behind it with its own database.
- The files in the repository hold no secrets, only templates. Each installation generates its own. The first commit still contains development values, which have been replaced and only ever worked on a developer's machine.

Known limits, planned for later:

- The permission check is in place and tested, but the only endpoint so far returns the signed-in user's own details.
- The Cognito setup has not been tested behind the https proxy of the Compose deployment.
- Roles are read at sign-in. A user who is disabled at the provider keeps an open session until it goes idle.
- Sessions live in the server's memory. That fits one Aurae instance per practice. Running several instances behind a load balancer needs the sessions in PostgreSQL first.
- Aurae checks the provider's configuration at startup and refuses to start while the provider is unreachable. In Kubernetes it must wait for Keycloak.

## License

Aurae is licensed under the GNU AGPL v3.0. See [LICENSE](LICENSE).
