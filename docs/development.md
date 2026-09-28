# Development

## Running locally

```bash
cd api && ./gradlew bootRun
```

This starts Postgres and Keycloak through `api/compose.yaml`, activates the `dev` profile, builds the frontend if it changed, and serves everything on `http://localhost:8080`. Test users and their passwords are in `api/keycloak/aurae-realm.json`.

For quick frontend work, also run `npm run dev` in `web/` and open `http://localhost:5173`. Vite updates the page on every save and forwards API and sign-in calls to Spring.

## Gotchas

### Two Compose files with different jobs

`api/compose.yaml` starts only the services the app needs during development. The app itself runs outside Docker and reads its settings from `application-dev.yaml`.

`deploy/compose/compose.yaml` runs the whole stack from the built image, the way a practice without Kubernetes would. The app reads its settings from environment variables and a `.env` file that is never committed.

### The dev profile comes only from Gradle

`bootRun` sets `spring.profiles.active=dev`, and Spring then loads `application-dev.yaml` by its name alone. Starting `main` from the IDE skips Gradle, so the app fails to find Keycloak. Set the active profile to `dev` in the IDE's run configuration.

### Spring does not start Compose if something is already running

When any service from `api/compose.yaml` is already up, Spring assumes the whole stack is running and skips `docker compose up`. If Keycloak is down while Postgres is up, the app fails at startup with an `UnsatisfiedDependencyException` on `securityFilterChain`. Start the missing service yourself.

### The app needs Keycloak at startup

Spring fetches Keycloak's configuration when it starts. If Keycloak is unreachable, the application context fails. The real cause sits at the bottom of the stack trace, below several lines about Spring MVC.

### Keycloak imports every JSON file in its import folder

`api/keycloak/` is mounted into the container, and `--import-realm` imports every `.json` file there as a realm, whatever its name. Keep only realm files in that folder.

### Realm changes need a new container

Keycloak skips a realm that already exists. After editing `aurae-realm.json`, recreate the container:

```bash
cd api && docker compose up -d --force-recreate keycloak
```

### The client secret lives in two files

`api/keycloak/aurae-realm.json` and `api/src/main/resources/application-dev.yaml` must hold the same secret. If they differ, sign-in fails with `invalid_client`. Both values work only against the Keycloak on your own machine.

### Roles are read at sign-in

Aurae maps the provider's roles once, when the user signs in. After changing a user's roles in Keycloak, sign out and in again.

### Ports are shared between Compose projects

Both Compose files publish Keycloak on host port 8180. Stop one stack before starting the other, or Docker reports `port is already allocated` and leaves containers half-created.

### Only the built frontend is in the JAR

`vite build` compiles all `.tsx` files into one JavaScript file and one CSS file under `web/dist`. Gradle copies that folder into `static/` on the classpath. Your source files never reach the image.
