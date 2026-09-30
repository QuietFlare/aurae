plugins {
    kotlin("jvm") version "2.3.21"
    kotlin("plugin.spring") version "2.3.21"
    id("org.springframework.boot") version "4.1.1"
    id("io.spring.dependency-management") version "1.1.7"
    kotlin("plugin.jpa") version "2.3.21"
}

group = "net.quietflare"
version = "0.0.1-SNAPSHOT"
description = "aurae"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-security-oauth2-client")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("tools.jackson.module:jackson-module-kotlin")
    developmentOnly("org.springframework.boot:spring-boot-docker-compose")
    runtimeOnly("org.postgresql:postgresql")
    testImplementation("org.springframework.boot:spring-boot-starter-actuator-test")
    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
    testImplementation("org.springframework.boot:spring-boot-starter-flyway-test")
    testImplementation("org.springframework.boot:spring-boot-starter-security-test")
    testImplementation("org.springframework.boot:spring-boot-starter-validation-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:testcontainers-postgresql")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
    }
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

// ./gradlew test runs the fast tests and needs nothing else running.
tasks.test {
    useJUnitPlatform {
        excludeTags("integration")
    }
}

// ./gradlew integrationTest runs the tests marked @IntegrationTest. They start the whole
// application against a real Postgres in a container, so Docker must be running.
tasks.register<Test>("integrationTest") {
    group = "verification"
    description = "Runs the tests that need Docker."
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    useJUnitPlatform {
        includeTags("integration")
    }
    shouldRunAfter(tasks.test)
}

// The React frontend is built by Vite and served by Spring from classpath:/static, so one image
// holds the whole application. Gradle rebuilds it only when files in web/ have changed.
val webDir = layout.projectDirectory.dir("../web")

val buildWeb by tasks.registering(Exec::class) {
    workingDir = webDir.asFile
    commandLine("npm", "run", "build")
    inputs.dir(webDir.dir("src"))
    inputs.files(webDir.file("package.json"), webDir.file("index.html"), webDir.file("vite.config.ts"))
    outputs.dir(webDir.dir("dist"))
}

tasks.processResources {
    from(buildWeb) {
        into("static")
    }
}

// Local runs use the dev profile: Keycloak on localhost:8180 with the imported aurae realm.
tasks.bootRun {
    systemProperty("spring.profiles.active", "dev")
}

// ./gradlew bootBuildImage builds the image locally. Add --publishImage to push it to GitHub's
// registry as well, with credentials from the environment:
//   GHCR_USER=<github user> GHCR_TOKEN=$(gh auth token) ./gradlew bootBuildImage --publishImage
tasks.bootBuildImage {
    imageName = "ghcr.io/quietflare/aurae:${project.version}"
    docker {
        // The registry comes from the image name. The credentials are only read when set, so a
        // plain local build needs none.
        publishRegistry {
            username = providers.environmentVariable("GHCR_USER")
            password = providers.environmentVariable("GHCR_TOKEN")
        }
    }
}
