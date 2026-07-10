# AGENTS.md — ResenhaBET Backend

## Commands

```bash
./mvnw test                                                    # all tests
./mvnw test -Dtest=TournamentServiceImplTest                   # single class
./mvnw test -Dtest="TournamentServiceImplTest#create*"         # single method
./mvnw compile                                                 # compile + annotation processors (required after mapper changes)
./mvnw spring-boot:run                                         # start app (requires PostgreSQL on localhost:5432)
```

## Stack

- **Spring Boot 4.0.4 / Java 17** (per pom.xml — docs/ files reference 3.4/21, those are stale).
- **PostgreSQL** at `localhost:5432/resenhaapi`, schema `resenha` (not `public`).
- **Flyway** owns the schema (`ddl-auto=validate`). Migrations: `src/main/resources/db/migration/V1–V18`.
- **Lombok + MapStruct** annotation processors. Lombok MUST appear first in `annotationProcessorPaths` (already correct).
- **No Spring Security.** Custom auth via `SessionFilter` + `CurrentUserContext` (ThreadLocal).

## Auth Model (no JWT, no Spring Security)

- `SessionFilter` reads `Authorization: Bearer <UUID>` from the `Session` table.
- `CurrentUserContext` (ThreadLocal) holds the authenticated `User`.
- In services: call `currentUserContext.getRequiredUser()` or `.requireAdmin()`.
- Public routes: `POST /api/v1/auth/login`, `POST|GET /api/v1/users`, `GET /api/v1/teams/**`, `/ws/**`, Swagger (`/swagger-ui/**`, `/v3/api-docs/**`). All other routes require `Authorization: Bearer <UUID>`.
- Admin seeded on first startup; name from `resenhabet.admin.name` property.

## Package Layout

```
com.franciscomaath.resenhaapi
├── controller/          # REST controllers + dto/{request,response}/
├── controller/exception/GlobalExceptionHandler
├── service/             # interfaces (e.g. TournamentService)
├── service/Impl/        # implementations (capital "Impl" — not "impl")
├── domain/
│   ├── entity/          # JPA entities (Lombok @Builder, @Getter, @Setter)
│   ├── repository/      # Spring Data JPA repos
│   ├── enums/
│   └── exception/       # BusinessException, ValidationException, etc.
├── mapper/              # MapStruct interfaces (@Mapper(componentModel="spring"))
├── config/              # SessionFilter, CurrentUserContext, CorsConfig, OddsProperties, PlayerInitializer
└── service/dto/         # Value objects: H2HRecord, OddsResult
```

## Domain Model — Key Facts

- **Player ≠ User.** A Player (plays FIFA) can exist without a User account. A User (bettor) can exist without a Player. Linked optionally via `player.user_id` (unique FK).
- **Player** starts with `currentElo = 1000`. Updated after each COMPLETED event.
- **Event** stores `homeEloBefore` / `awayEloBefore` snapshots.
- **Market** lifecycle: OPEN → SUSPENDED (by scheduler when gameDatetime passes) → CLOSED (on finishEvent).
- **Outcome** names: "Vitória Casa", "Empate", "Vitória Fora".
- **Bet.oddSnapshot** locked at bet time, never changes.
- **Wallet** is global per User (not per tournament). Auto-created on user registration.
- **Transaction** types: `DEPOSIT`, `BET`, `PRIZE`. Has nullable `bet_id` FK for traceability.

## Odds Calculation

- Static odds — calculated once at event creation, never updated.
- All configurable params in `application.properties` under `resenhabet.odds.*`, bound by `OddsProperties`:
  - `elo-scale=600`, `draw-factor=0.28`, `max-h2h-weight=0.20`, `min-odd=1.05`, `h2h-match-limit=10`
- `OddsCalculatorService` is a pure service — no repositories, no side effects.
- Full spec with formulas and test scenarios: `docs/ODDS_CALCULATION.md`.

## Error Handling

All errors return `ErrorResponseDTO`: `{ status, message, error, path, timestamp, fieldErrors?, additionalInfo? }`.

| Exception | HTTP |
|---|---|
| `ResourceNotFoundException` | 404 |
| `BusinessException` | 400 |
| `ValidationException` | 400 |
| `InvalidStateException` | 409 |
| `DuplicateResourceException` | 409 |
| `InsufficientFundsException` | 402 |
| `UnauthorizedException` | 401 |

Error messages mix English (exception throws) and Brazilian Portuguese (SessionFilter, some business messages).

## Testing Conventions

- **Pure unit tests only** — no `@SpringBootTest`, no Spring context, no test DB.
- Service tests: `@ExtendWith(MockitoExtension.class)` + `@Mock` deps + `@InjectMocks` on `*ServiceImpl`.
- Controller tests: standalone `MockMvc` built in `@BeforeEach`:
  ```java
  MockMvcBuilders.standaloneSetup(controller)
      .setControllerAdvice(new GlobalExceptionHandler())
      .setValidator(validator)
      .build();
  ```
- `CurrentUserContext` is `@Mock`ed in service tests (it's a `@Component`, not auto-wired in tests).
- No `src/test/resources/` — tests never load `application.properties`.

## Gotchas

- **New DB column?** Create a Flyway migration (`V<N>__*.sql` in `resenha` schema). Never rely on `ddl-auto`.
- **Changed a MapStruct mapper?** Run `./mvnw compile` before tests — implementations are generated at compile time.
- **`@Transactional`** uses `jakarta.transaction.Transactional` (not `org.springframework`).
- **Service impls live in `service/Impl/`** (capital I). Imports use this exact casing.
- **Phase 2 is partially complete.** Identity (Groups 1–2) and wallet are done. Betting (Group 4), stats (5), WebSocket (6), and frontend work remain. See `docs/RESENHABET_MASTER_BRIEFING.md` for the full task list.
- **Odds docs vs code:** `docs/ODDS_CALCULATION.md` worked examples use `elo-scale=400` for manual calculations, but the code defaults to `600`. The `draw-factor` default in docs text says 0.12 in one place but the code and properties use 0.28.
