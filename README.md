# Expense Tracker

A cloud-native expense management platform built as Spring Boot microservices.

**Current state: Slice 2 — expense persistence.** Four services and PostgreSQL start and report health;
`expense-service` persists expenses to its own schema via Flyway and exposes CRUD. Authentication and
gateway routing are not built yet.

---

## Architecture

```text
                         ┌──────────────────┐
                         │   API Gateway    │
                         │      :8083       │
                         │  WebFlux/Netty   │
                         └────────┬─────────┘
                                  │
                 ┌────────────────┼────────────────┐
                 ↓                ↓                ↓
        ┌────────────────┐ ┌───────────────┐ ┌──────────────────┐
        │  auth-service  │ │expense-service│ │analytics-service │
        │     :8082      │ │     :8081     │ │      :8084       │
        │   MVC/Tomcat   │ │  MVC/Tomcat   │ │   MVC/Tomcat     │
        └───────┬────────┘ └───────┬───────┘ └────────┬─────────┘
                └──────────────────┼──────────────────┘
                                   ↓
                          ┌─────────────────┐
                          │  PostgreSQL 16  │
                          │      :5432      │
                          └─────────────────┘
```

| Service | Port | Web stack | Module |
|---|---|---|---|
| `expense-service` | 8081 | Spring MVC (Tomcat) | [expense-service/](expense-service) |
| `auth-service` | 8082 | Spring MVC (Tomcat) | [auth-service/](auth-service) |
| `api-gateway` | 8083 | Spring Cloud Gateway on WebFlux (Netty) | [api-gateway/](api-gateway) |
| `analytics-service` | 8084 | Spring MVC (Tomcat) | [analytics-service/](analytics-service) |
| PostgreSQL 16 | 5432 | — | `docker-compose.yml` |

## Stack

Java 21 · Spring Boot 3.5.15 · Spring Cloud 2025.0.3 · Maven (multi-module) · PostgreSQL 16 · Docker Compose

## Build

```bash
./mvnw clean verify
```

A single service builds independently:

```bash
./mvnw -pl expense-service -am clean package
```

## Run

```bash
docker compose up --build -d
```

Wait for every container to report `healthy` — not merely `Up`:

```bash
docker compose ps
```

Then:

```bash
curl http://localhost:8081/health && curl http://localhost:8082/health && curl http://localhost:8083/health && curl http://localhost:8084/health
```

Tear down (`-v` also drops the database volume):

```bash
docker compose down -v
```

## API

`expense-service` (`:8081`) — unauthenticated for now; user scoping arrives with gateway JWT validation.

| Method | Path | Result |
|---|---|---|
| `POST` | `/expenses` | 201 + `Location`; 400 with per-field errors on invalid input |
| `GET` | `/expenses` | all expenses, most recent `spentOn` first |
| `GET` | `/expenses/{id}` | one expense, or 404 |
| `PUT` | `/expenses/{id}` | replaces the editable fields; `createdAt` is preserved |
| `DELETE` | `/expenses/{id}` | 204, or 404 if unknown |

```bash
curl -X POST http://localhost:8081/expenses -H 'Content-Type: application/json' -d '{"description":"Weekly groceries","amount":42.50,"category":"FOOD","spentOn":"2026-08-20"}'
```

## Configuration

Compose reads database credentials via `${VAR:-default}` interpolation, so it runs with no `.env`
file present. To override, copy [`.env.example`](.env.example) to `.env` (gitignored). No credentials
are hard-coded in application source.

## Key decisions

- **One PostgreSQL instance, one schema per service.** Ownership is separated (each service owns and
  migrates only its own schema, with its own Flyway history table) without separating infrastructure.
  Three database instances would buy physical isolation this project does not need, at three times the
  operational and hosting cost. Schemas are the seam, so promoting one to its own instance later is a
  connection-string change rather than a redesign.
- **Flyway, never `ddl-auto: update`.** Schema changes will be versioned, checksummed SQL files
  reviewed in a PR and shipped with the artifact — not DDL inferred at runtime from whatever entities
  happen to be on the classpath. JPA runs with `ddl-auto: validate`, so entity/table drift is a
  startup failure rather than a production surprise — verified by deliberately renaming a column and
  watching the application refuse to boot.
- **The gateway is reactive; the other three are not.** Spring Cloud Gateway is built on WebFlux, and
  a gateway's work is almost pure I/O — Netty's event loop handles many in-flight proxied requests on
  few threads. The other services are blocking CRUD over blocking JDBC, where the servlet model is
  simpler to write and debug. `api-gateway` must never see `spring-boot-starter-web`: Boot would
  auto-configure a servlet container and the gateway would refuse to start. A `maven-enforcer` rule in
  [api-gateway/pom.xml](api-gateway/pom.xml) fails the build if that ever happens.
- **Health via Actuator, not a custom controller.** `management.endpoints.web.base-path: /` serves the
  endpoint at `/health`, and `exposure.include: health` keeps it the only HTTP-exposed endpoint. Now
  that `expense-service` has a `DataSource`, Actuator's database health indicator registers itself and
  `/health` returns 503 `DOWN` when PostgreSQL is unreachable — a hand-written controller returning
  `"OK"` would have kept reporting healthy the whole time.
- **No shared library module.** A `common`/`shared-dto` module re-couples services at compile time and
  forces lockstep redeploys. One will be introduced only if duplication genuinely hurts.

## Not yet implemented

Budgets, users, registration/login, BCrypt, JWT, Spring Security, gateway routes and filters,
per-user expense scoping, pagination, analytics, LLM insights, GitHub Actions, AWS deployment.
