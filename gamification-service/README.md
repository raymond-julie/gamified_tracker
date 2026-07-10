# Gamification Service

**Tracks XP, levels, and level thresholds per user + activity.** · Port **8082**

The gamification engine. When a user logs an activity, the Activity Service forwards the earned XP here; this service accumulates it, recomputes the user's level for that activity against configurable thresholds, and keeps an append-only history of every change. It is a **leaf** service — it calls no other service (no Feign clients).

## Role in the system

```
  activity-service ──POST /level──►  gamification-service (8082)
     (8081, Feign)                        │
                                          ▼
                                   PostgreSQL (5433)
                                   level_tracker
                                   level_tracker_archive
                                   activity_level_threshold
```

Called by: **activity-service** (after each activity log) and, indirectly, the **api-gateway** is *not* wired to it for reads today. Registers with **Eureka** (8761); persists to **PostgreSQL**.

## Responsibilities

- Accumulate `totalXp` per `(userId, activityId)` pair, safely under concurrency.
- Recompute `level` and `currentLevelXp` from the highest reached `ActivityLevelThreshold`.
- Append a **previous-state** snapshot to an archive table on every update (audit/history).
- Manage the per-activity level thresholds (the XP curve).

## Tech stack

- Java 17, Spring Boot 3.5, Spring Cloud 2025 (Eureka client)
- Spring Data JPA + PostgreSQL; **pessimistic row locking** for the XP accumulation
- Java 17 **sealed interface** (`LevelOutcome`) with pattern matching for the level decision
- Entry point: `GamificationServiceApplication` (`@SpringBootApplication`; no `@EnableFeignClients` — leaf service)

## API reference

Base paths: `/level` and `/threshold`. No auth at this layer (enforced only at the gateway). All bodies are JSON.

### Level Tracker — `/level`

#### `GET /level`
List every level-tracker row (all users, all activities). → `200 OK`, array of `LevelTrackerDto`.

#### `GET /level/{id}`
Fetch one row by its numeric surrogate id.
- `200 OK` → `LevelTrackerDto`
- `404 Not Found` → `ProblemDetail` (`"LevelTracker with id: {id} not found"`)

#### `POST /level`
Create-or-update XP for a `(userId, activityId)` pair and recompute level. This is the endpoint the Activity Service calls over Feign after each activity log.

Request — `LevelTrackerRequestDTO`:
| Field | Type | Notes |
|-------|------|-------|
| `userId` | Long | |
| `activityId` | Long | |
| `xp` | double | XP to **add**. Must be `>= 0` — a compact-constructor guard rejects negatives |

Response `200 OK` — `LevelTrackerDto`:
| Field | Type | Notes |
|-------|------|-------|
| `userId` | Long | |
| `activityId` | Long | |
| `level` | Integer | recomputed |
| `totalXp` | double | accumulated total after this call |
| `currentLevelXp` | double | XP within the current level (`totalXp − threshold.xpRequired`) |

- `400 Bad Request` → `ProblemDetail` (`"Invalid request body"`) if `xp` is negative.

#### `GET /level/user/{userId}`
All level-tracker rows for a user (one per activity they've logged). → `200 OK`, array.

#### `GET /level/activity/{activityId}`
All level-tracker rows for an activity (one per user). → `200 OK`, array.

### Activity Level Threshold — `/threshold`

Defines the XP required to reach each level, per activity. **Thresholds must be seeded** (there is no default curve yet — see Troubleshooting).

#### `GET /threshold`
List all thresholds. → `200 OK`, array of `ActivityLevelThresholdDto`.

#### `POST /threshold`
Create (or overwrite) a threshold.

Request / Response — `ActivityLevelThresholdDto`:
| Field | Type |
|-------|------|
| `activityId` | Long |
| `level` | Integer |
| `xpRequired` | double |

#### `POST /threshold/activity`
Look up a single threshold by composite key — despite the `POST`, this is a **read** (the body is used only as a key; `xpRequired` is ignored).
- `200 OK` → the matching `ActivityLevelThresholdDto`
- `404 Not Found` → `ProblemDetail` (`"ActivityLevelThreshold not found"`)

## Data model

**`level_tracker`** — current state per user+activity:
| Column | Type | Notes |
|--------|------|-------|
| `id` | bigint | PK, identity |
| `user_id` | bigint | part of unique key |
| `activity_id` | bigint | part of unique key |
| `level` | int | |
| `total_xp` | double | |
| `current_level_xp` | double | |

Unique constraint **`uk_level_tracker_user_activity`** on `(user_id, activity_id)` — one row per pair; also the conflict target for the atomic insert.

**`level_tracker_archive`** — append-only history (surrogate PK so rows accumulate, not overwrite):
| Column | Type | Notes |
|--------|------|-------|
| `id` | bigint | PK, identity |
| `user_id`, `activity_id` | bigint | plain columns (not a key) |
| `level`, `total_xp`, `current_level_xp` | — | snapshot of the **previous** state |
| `archived_at` | timestamp | when the snapshot was taken |

**`activity_level_threshold`** — the XP curve. `@EmbeddedId` composite key `ActivityLevelThresholdId(activityId, level)` + `xpRequired` (double).

## Key internal flow — `LevelTrackerServiceImpl.save()`

Concurrency-safe XP accumulation (fixes the lost-update race — [issue #5](https://github.com/prashant-singh-2001/gamified_tracker/issues/5), merged in [PR #29](https://github.com/prashant-singh-2001/gamified_tracker/pull/29)):

1. **`insertIfAbsent(userId, activityId)`** — native `INSERT … ON CONFLICT (user_id, activity_id) DO NOTHING`. Atomically creates the zero-state row if absent; the affected-row count tells us whether this call **created** the row.
2. **`findByUserIdAndActivityIdForUpdate(...)`** — `@Lock(PESSIMISTIC_WRITE)` → `SELECT … FOR UPDATE`, locking the row for the rest of the transaction so concurrent updates serialize instead of racing.
3. If the row already existed, **archive the previous state** (snapshot taken *before* any mutation).
4. Add `xp` to `totalXp`, then **`applyLevel(...)`**: query the highest reached threshold and set `level` / `currentLevelXp` via the sealed `LevelOutcome` (`LeveledUp` vs `InProgress`) with `instanceof` pattern matching.
5. Persist and return the DTO.

All in one `@Transactional` method — no retry loop.

## Configuration

Reads standard env vars (see root [`.env.example`](../.env.example)):

| Var / key | Default | Purpose |
|-----------|---------|---------|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://postgres:5432/tracker_db` | DB connection |
| `SPRING_DATASOURCE_USERNAME` / `_PASSWORD` | `postgres` / `postgres` | DB creds |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | `update` | schema management (dev only) |
| `server.port` | `8082` | HTTP port |
| `eureka.client.service-url.defaultZone` | `http://eureka-server:8761/eureka` | registry |

## Inter-service dependencies

- **Calls:** nothing (leaf service — no Feign clients).
- **Called by:** activity-service (`POST /level`).
- **Infra:** Eureka (registration), PostgreSQL (persistence).

## Running

**Whole stack (recommended):**
```bash
docker-compose up --build          # from repo root
```

**Standalone** (needs Postgres + Eureka reachable):
```bash
cd gamification-service
mvn spring-boot:run
```

## Testing

```bash
mvn test          # from gamification-service/
```

Covered by `@WebMvcTest` controller tests, `@DataJpaTest` repository tests, and Mockito service tests — including the `save()` concurrency flow and archive-on-update behavior (`LevelTrackerServiceImplTest`).

> The concurrency fix itself was additionally verified with a live 20-request concurrent burst — exact `totalXp`, a single row, and a coherent archive sequence.

## Troubleshooting

- **Everyone stays at level 1** — no thresholds are seeded by default. `POST /threshold` a curve (e.g. level 2 at `xpRequired: 100`) for the activity first. (A default curve is proposed in [issue #8](https://github.com/prashant-singh-2001/gamified_tracker/issues/8).)
- **`POST /level` returns 400** — `xp` was negative; the DTO rejects it before persistence.
- **`/actuator/health` 404s** — actuator not yet wired ([issue #28](https://github.com/prashant-singh-2001/gamified_tracker/issues/28)).
- **Schema/constraint errors on startup after a model change** — `ddl-auto: update` won't retrofit new constraints onto existing data; recreate the dev volume with `docker compose down -v`.

## Related docs

- [Root README](../README.md) · [API.md](../API.md)
