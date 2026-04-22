# DoThatTask — Backend

REST API for the **DoThatTask** task-management app, written in **Kotlin** on top of the **Ktor** framework. Exposes authenticated endpoints for user registration/login and full CRUD on tasks, backed by PostgreSQL through the Exposed ORM.

## Learning goal

Hands-on practice with **Kotlin** and the **Ktor** web framework — routing DSL, content negotiation, status pages, authentication, and integration with a relational database via Exposed.

## Tech stack

- **Language:** Kotlin
- **Framework:** Ktor (Netty engine)
- **Persistence:** PostgreSQL + Exposed
- **Auth:** JWT
- **Serialization:** kotlinx.serialization
- **Build / Deploy:** Gradle (Kotlin DSL), Docker, docker-compose

## Project structure

```
src/main/kotlin/
├── Application.kt             # Ktor entry point & feature installation
├── Routing.kt                 # Route configuration
├── Serialization.kt           # Content negotiation setup
├── AppModule.kt               # Composition root / DI wiring
├── Controller/                # TaskController, UserController
├── Model/                     # Task, User, UserPrincipal
└── data/
    ├── DBSchema/              # Exposed table definitions + seed
    └── repository/            # TaskRepository, UserRepository
```

## Build & run

| Task | Command |
| --- | --- |
| Run tests | `./gradlew test` |
| Run locally | `./gradlew run` |
| Build fat JAR | `./gradlew buildFatJar` |
| Build Docker image | `./gradlew buildImage` |
| Run via docker-compose (API + DB) | `docker compose up` |

The API listens on port `8080` by default.

## Companion repository

- Multiplatform client: [DoThatTask_fe](https://github.com/Valer09/DoThatTask_fe)
