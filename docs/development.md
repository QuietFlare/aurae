# Development

## Running locally

Once, after cloning, generate the local secrets:

```bash
bash scripts/dev-setup.sh
```

It writes `api/keycloak/aurae-realm.json` and `api/config/application-dev.yaml` from templates, with random values. Git ignores both. Test users and their passwords are in the realm file.

Then start everything:

```bash
cd api && ./gradlew bootRun
```

This starts Postgres and Keycloak through `api/compose.yaml`, activates the `dev` profile, builds the frontend if it changed, and serves everything on `http://localhost:8080`.

For quick frontend work, also run `npm run dev` in `web/` and open `http://localhost:5173`. Vite updates the page on every save and forwards API and sign-in calls to Spring.

## Gotchas

### Two Compose files with different jobs

`api/compose.yaml` starts only the services the app needs during development. The app itself runs outside Docker and reads its settings from `application-dev.yaml`.

`deploy/compose/compose.yaml` runs the whole stack from the built image, the way a practice without Kubernetes would. The app reads its settings from environment variables and a `.env` file that is never committed.

### The dev profile comes only from Gradle

`bootRun` sets `spring.profiles.active=dev`, and Spring then loads `application-dev.yaml` by its name alone. Starting `main` from the IDE skips Gradle, so the app fails to find Keycloak. Set the active profile to `dev` in the IDE's run configuration.

### Spring skips Compose by default if something is already running

By default, when any service from `api/compose.yaml` is already up, Spring assumes the whole stack is running and skips `docker compose up`. A stopped Keycloak then fails startup with an `UnsatisfiedDependencyException` on `securityFilterChain`, and a stopped Postgres with `Failed to configure a DataSource: 'url' attribute is not specified`. The dev profile sets `spring.docker.compose.start.skip: never`, so `bootRun` always runs `docker compose up`, which starts only what is missing. Starting the app another way still needs the services up.

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

`api/keycloak/aurae-realm.json` and `api/config/application-dev.yaml` must hold the same secret. `scripts/dev-setup.sh` writes both at once. If they differ, sign-in fails with `invalid_client`. After `dev-setup.sh --force`, recreate Keycloak so it imports the new values. The deploy Compose stack does not use this file. Its realm is `deploy/keycloak/aurae-realm.json`, without users, and its secret comes from `.env`.

### Roles are read at sign-in

Aurae maps the provider's roles once, when the user signs in. After changing a user's roles in Keycloak, sign out and in again.

### The two stacks use different addresses

Development serves plain http on `localhost:8080`, with Keycloak on port 8180. The deploy stack publishes only ports 80 and 443 and serves https on the name in `AURAE_HOST`, with Keycloak under `/auth`. See `deploy/compose/README.md`.

### Test users skip two-factor sign-in

The development realm enforces the password policy but does not ask test users for a one-time code, so sign-in stays quick. The deploy realm asks every new user to set one up.

### Only the built frontend is in the JAR

`vite build` compiles all `.tsx` files into one JavaScript file and one CSS file under `web/dist`. Gradle copies that folder into `static/` on the classpath. Your source files never reach the image.
