# API Documentation — Gamified Tracker

This document covers every REST endpoint exposed by the three services: **API Gateway** (public entry point, port `8080`), **Activity Service** (internal, port `8081`), and **Gamification Service** (internal, port `8082`).

In normal use, clients talk **only to the API Gateway**. The Activity Service and Gamification Service endpoints are documented separately below because they're directly reachable in this dev setup (no network isolation yet — see `CLAUDE.md`) and are useful for debugging service-to-service calls.

---

## Authentication

All API Gateway endpoints require a JWT **except** `/auth/**`. Obtain a token via register or login, then send it on every subsequent request:

```
Authorization: Bearer <token>
```

Tokens are signed HS256 JWTs. The signing secret comes from `jwt.secret` (see `.env` / `JWT_SECRET`). Token expiry is **hardcoded to 24 hours** in `JwtUtil.generateToken()` — the `jwt.expiration` / `JWT_EXPIRATION` config value is not currently read by the token-generation code, only kept as unused config.

⚠️ **Known issue:** `JwtFilter` currently authenticates every valid token with an **empty authorities list** (`List.of()`). Endpoints annotated `@PreAuthorize("hasRole('ADMIN')")` (e.g. `POST /api/activity`) will reject every request, including from users registered with `role: ADMIN`, until this is fixed.

---

## API Gateway (port 8080) — public surface

### Auth

#### `POST /auth/register`
Creates a user and returns a JWT. Public (no token required).

**Request body:**
| Field | Type | Notes |
|---|---|---|
| `firstName` | String | |
| `lastName` | String | |
| `email` | String | must be unique |
| `password` | String | hashed with BCrypt before storage |
| `role` | String enum: `USER`, `ADMIN` | accepted but the user is always saved with role `USER` regardless of what's sent — see Known Issues |

**Response:** `200 OK`, body is a raw JWT string (not JSON-wrapped).

---

#### `POST /auth/login`
Authenticates and returns a JWT. Public (no token required).

**Request body:**
| Field | Type |
|---|---|
| `email` | String |
| `password` | String |

**Response:** `200 OK`, raw JWT string. `500` (raw `RuntimeException`, not a `ProblemDetail`) if the user doesn't exist or the password doesn't match.

---

### Activity

#### `GET /api/activity`
List all activities. Requires auth (any role).

**Response:** `200 OK`, JSON array of:
| Field | Type |
|---|---|
| `name` | String |
| `category` | enum: `STUDY`, `WORK`, `GAMING`, `CHORES`, `HEALTH`, `OTHER` |
| `xpMultiplier` | double |
| `active` | boolean |
| `description` | String |
| `createdAt` | ISO-8601 datetime string |

---

#### `GET /api/activity/{name}`
Fetch one activity by name. Requires auth (any role).

- `200 OK` — same shape as above (single object)
- `404` `ProblemDetail` — not found

⚠️ **Currently broken.** The upstream Activity Service route has a routing bug (`@GetMapping("/  {name}")` — stray whitespace) that makes it 404 for every name, including ones that exist. See Known Issues.

---

#### `POST /api/activity`
Create an activity. **Requires `ADMIN` role** (currently unreachable by anyone — see Known Issues above).

**Request body:**
| Field | Type | Notes |
|---|---|---|
| `name` | String | should be unique (enforced at the DB level) |
| `category` | enum: `STUDY`\|`WORK`\|`GAMING`\|`CHORES`\|`HEALTH`\|`OTHER` | |
| `xpMultiplier` | double | e.g. `1.5` for Study |
| `active` | boolean | |
| `description` | String | optional |
| `createdAt` | ISO-8601 datetime string | accepted but **ignored** — the server always sets `createdAt` to the current time |

**Response:** `200 OK`, same shape as `GET /api/activity/{name}`.

---

### Activity Log

#### `GET /api/activitylog/{id}`
Fetch one activity log by its numeric id. Requires auth.

**Response:** `200 OK` (shape below) or `404` if not found.

---

#### `POST /api/activitylog`
Records an activity session, computes XP (with a chance of a bonus roll), and forwards the XP to the Gamification Service. Requires auth.

**Request body:**
| Field | Type | Notes |
|---|---|---|
| `userId` | Long | |
| `activityName` | String | must match an existing `Activity.name`, else `404` |
| `startTime` | ISO-8601 datetime string | |
| `endTime` | ISO-8601 datetime string | must be after `startTime` |
| `notes` | String | optional |
| `createdAt` | ISO-8601 datetime string | accepted but **ignored** — server sets it to current time |

**Response:** `200 OK`:
| Field | Type | Notes |
|---|---|---|
| `id` | Long | |
| `userId` | Long | |
| `activity` | object | the full `Activity` (id, name, category, xpMultiplier, active, description, createdAt) |
| `startTime` | ISO-8601 datetime string | |
| `endTime` | ISO-8601 datetime string | |
| `durationMinutes` | Long | computed: `endTime - startTime` |
| `xpEarned` | double | computed: `durationMinutes × activity.xpMultiplier × bonus`. `bonus` is `1.0` normally, or a random value in `[1.1, 1.5)` on a ~20% chance roll |
| `notes` | String | |
| `createdAt` | ISO-8601 datetime string | |

- `404` `ProblemDetail` if `activityName` doesn't match any activity.

---

#### `GET /api/activitylog/user/{id}`
List all activity logs for a user. Requires auth.

**Response:** `200 OK`, JSON array of the same shape as the `POST` response above.

---

### Misc

#### `GET /api/hello`
Returns `"Hello {email}"` for the authenticated caller. Requires auth. Diagnostic/example endpoint only.

---

## Activity Service (port 8081) — internal

Base path `/activity` and `/activitylog`. No auth layer of its own — auth is only enforced at the Gateway.

#### `GET /activity/`
List all activities. Same response shape as the Gateway's `GET /api/activity`.

#### `GET /activity/{name}` ⚠️ currently broken
Fetch one activity by name.
- Intended: `200 OK` with the activity, or `404` `ProblemDetail` (`"Activity not found: {name}"`) if missing.
- **Actual current behavior:** the route is declared as `@GetMapping("/  {name}")` (stray double space), so it matches nothing — every request 404s with Spring's generic error body, regardless of whether the name exists.

#### `POST /activity/`
Create an activity. Same request/response shape as the Gateway's `POST /api/activity` (no role check at this layer — that's Gateway-only).

#### `GET /activitylog/{id}`
Fetch one activity log by id. `200 OK` or `404` `ProblemDetail` (`"Activity log not found: {id}"`).

#### `POST /activitylog/`
Create an activity log (computes duration + XP bonus, calls Gamification Service). Same request/response shape as the Gateway's `POST /api/activitylog`. `404` `ProblemDetail` if `activityName` doesn't match an existing activity.

#### `GET /activitylog/user/{id}`
List all activity logs for a user.

---

## Gamification Service (port 8082) — internal

Base paths `/level` and `/threshold`. No auth layer of its own.

### Level Tracker

#### `GET /level`
List every `LevelTracker` row (all users, all activities).

**Response shape** (all endpoints below return this):
| Field | Type | Notes |
|---|---|---|
| `userId` | Long | |
| `activityId` | Long | |
| `level` | Integer | |
| `totalXp` | double | ⚠️ **always returns `0.0`** — a pre-existing mapping gap that never carries the real accumulated total through to the DTO (the underlying entity does track it correctly internally) |
| `currentLevelXp` | double | XP accumulated within the current level |

#### `GET /level/{id}`
Fetch one `LevelTracker` by its internal numeric id. `200 OK` or `404` `ProblemDetail` (`"LevelTracker with id: {id} not found"`).

#### `POST /level`
Create-or-update a user's XP for an activity, recalculating level. This is what `ActivityLogService` (Activity Service) calls internally after each activity log is recorded — not typically called directly by clients.

**Request body:**
| Field | Type | Notes |
|---|---|---|
| `userId` | Long | |
| `activityId` | Long | |
| `xp` | double | XP to add. **Must be ≥ 0** — negative values are rejected with `400 Bad Request` before reaching the database |

**Response:** `200 OK`, the resulting `LevelTrackerDto` (shape above). Level-up logic: crosses the highest `ActivityLevelThreshold` whose `xpRequired` is ≤ the new total XP for that activity; `currentLevelXp` becomes `totalXp − threshold.xpRequired`.

#### `GET /level/user/{userId}`
List all `LevelTracker` rows for a given user (one per activity they've logged).

#### `GET /level/activity/{activityId}`
List all `LevelTracker` rows for a given activity (one per user who's logged it).

---

### Activity Level Threshold

Defines the XP required to reach each level, per activity.

#### `GET /threshold`
List all thresholds.

**Response shape:**
| Field | Type |
|---|---|
| `activityId` | Long |
| `level` | Integer |
| `xpRequired` | double |

#### `POST /threshold/activity`
Look up a single threshold by composite key (despite the `POST`, this is a read — the body is used purely as a key, not persisted).

**Request body:** `{ "activityId": Long, "level": Integer }` (`xpRequired` is ignored).

**Response:** `200 OK` with the matching threshold, or `500` (raw `NoSuchElementException`, not a `ProblemDetail` — inconsistent with the rest of this service) if no match.

#### `POST /threshold`
Create (or overwrite) a threshold.

**Request body:** full shape above (`activityId`, `level`, `xpRequired`).

**Response:** `200 OK`, the saved threshold.

---

## Error Response Format

Most `404` responses across all three services use Spring's RFC 7807 `ProblemDetail`:

```json
{
  "type": "about:blank",
  "title": "Not Found",
  "status": 404,
  "detail": "Activity not found: Study",
  "instance": "/activity/Study"
}
```

Exceptions to this (return a generic Spring Boot error body or a raw `500` instead):
- `POST /auth/login` failures (user not found / bad password) — raw `500`.
- `POST /threshold/activity` with no match — raw `500`.
- `GET /activity/{name}` and any other route that fails to match at all (e.g. the currently-broken activity lookup) — Spring's default 404 whitelabel body, not `ProblemDetail`.
- Validation failures on `POST /level` (negative `xp`) — generic `400` Bad Request body, not `ProblemDetail`.

---

## Known Issues Summary

| Issue | Impact |
|---|---|
| `ActivityController.getActivity` route has a stray-whitespace path (`"/  {name}"`) | `GET /activity/{name}` (and the Gateway route that proxies it) 404s unconditionally |
| `JwtFilter` grants no authorities to any authenticated user | `@PreAuthorize("hasRole('ADMIN')")` on `POST /api/activity` rejects everyone, including real admins |
| `AuthService.register` ignores the `role` field, always saves `USER` | Even if the authority bug above were fixed, there's currently no way to register an `ADMIN` via the API |
| `JwtUtil` hardcodes a 24h expiry, ignoring the `jwt.expiration` config | `JWT_EXPIRATION` env var has no effect |
| `LevelTrackerService.mapToDto` never sets `totalXp` on the response DTO | Every Level Tracker response shows `totalXp: 0.0` regardless of actual accumulated XP |
| Several error paths return raw `500`s / default error bodies instead of `ProblemDetail` | Inconsistent error shape for API consumers (see Error Response Format above) |
