# 0013. Build the application image with a multi-stage Dockerfile

Date: 2026-10-11
Recorded retrospectively; decided 2026-10-06.
Status: Accepted
Amended 2026-10-11: corrected what layer caching saves on a code change, and described the configuration options of buildpacks fairly.

## Context

A reviewer should be able to run the whole system with one command, without
installing Java 25 or Maven. That needs a container image for the application,
next to the PostgreSQL container that Docker Compose already starts. The image
should be small enough to pull, run without root, and rebuild quickly when
only the code changes.

## Decision

A multi-stage `Dockerfile` in the repository root:

- **Build stage** on `eclipse-temurin:25-jdk`. It runs the project's own Maven
  wrapper, so the Maven version is the same as in CI. A cache mount keeps
  Maven's downloads between builds without putting them in the image. Tests are
  skipped here (`-DskipTests`): the integration tests need Docker for
  Testcontainers, which isn't available inside an image build, and CI already
  runs every test on every push.
- **Layered jar.** The jar is copied to a fixed name, `application.jar`, and
  split with Spring Boot's `-Djarmode=tools ... extract --layers` into
  dependencies, loader, snapshot dependencies and application. When only my code
  changes, the build stage still recompiles and re-extracts the jar, but the
  runtime image reuses every layer whose content is unchanged. Only the small
  application layer is new, so pushing or pulling the new image moves little
  data. The fixed name keeps the entry point independent of the version in
  `pom.xml`.
- **Runtime stage** on `eclipse-temurin:25-jre`: no JDK, no Maven, no source
  code. It runs as a non-root `app` user, with `-XX:MaxRAMPercentage=75.0` so the
  heap is sized from the container's memory limit and leaves room for non-heap
  memory.

`compose.yaml` adds the app as a second service behind the `app` profile, and
it waits for a PostgreSQL healthcheck before starting. Running the app from
IntelliJ starts only PostgreSQL, so the two never compete for port 8080. The
whole system starts with:

    docker compose --profile app up --build

### Alternatives considered

- **Spring Boot buildpacks** (`spring-boot:build-image`). No Dockerfile to
  maintain, and it can be configured: the builder and run image, the JVM
  version, and JVM options through environment variables. But that
  configuration lives in the build plugin and the buildpack's own conventions,
  and writing the Dockerfile myself teaches more about how an image is built.
- **Jib.** Fast, doesn't need Docker to build, and layers automatically, but it
  only works for JVM projects and moves the image definition into plugin
  configuration.
- **Alpine or distroless runtime images.** Smaller, but Alpine uses a different
  C library (musl), and distroless has no shell for debugging. The Ubuntu-based
  Temurin JRE is the safest default while the setup is new.

A Dockerfile is also the skill that transfers to any language and to
Kubernetes later.

## Consequences

- Anyone with Docker can run the full system without a local JDK.
- After a code change, the build stage runs again, but the runtime image only
  gets a new application layer; the dependency layers are reused.
- The image is about 600 MB, more than I first expected. Most of it is the JRE
  and the Ubuntu base. A distroless or `jlink` runtime is on the post-MVP
  hardening list.
- The compose credentials are plain values. That's acceptable for a local demo;
  real secrets handling belongs to the Phase 3 security work.