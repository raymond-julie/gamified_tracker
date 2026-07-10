# API Gateway

**Public entry point — JWT authentication/authorization and routing to the backend services.** · Port **8080**

The only service a client should talk to. It handles user registration/login (issuing JWTs), authenticates every other request, enforces role-based access, and forwards calls to the Activity Service over Feign.

## Role in the system

```
   client
     │  Bearer <JWT>
     ▼
  api-gateway (8080) ──Feign──► activity-service (8081) ──► gamification-service (8082)
     │  Spring Security + JWT
     ▼
  PostgreSQL (5433)  ← user_entity (auth)
```

Calls: **activity-service** (via `ActivityClient`). Registers with **Eureka**; owns a small **PostgreSQL** table for users. (A `GamificationClient` exists but no controller currently exposes gamification endpoints through the gateway — see Inter-service dependencies.)

## Responsibilities

- Register and authenticate users; issue signed JWTs carrying the user's role.
- Validate the JWT on every non-public request and populate the security context.
- Enforce authorization — method-level `@PreAuthorize` for admin-only operations.
- Proxy activity and activity-log requests to the Activity Service.

## Tech stack

- Java 17, Spring Boot 3.5, Spring Cloud 2025 (Eureka client, **OpenFeign**)
- **Spring Security 6** + **JWT** (`io.jsonwebtoken:jjwt` 0.13), `BCryptPasswordEncoder`
- Spring Data JPA + PostgreSQL (users only)
- Entry point: `ApiGatewayApplication` (`@SpringBootApplication` + `@EnableFeignClients`)

## Security model (the centerpiece)

- **`SecurityConfig`** — `@EnableWebSecurity` + **`@EnableMethodSecurity`**. `/auth/**` is `permitAll`; **everything else requires authentication**. `JwtFilter` runs before `UsernamePasswordAuthenticationFilter`.
- **`JwtUtil`** — `generateToken(email, role)` signs an HS256 token with the user's `role` as a claim and a configurable expiry (`jwt.expiration`); `validateToken` parses and returns the claims.
- **`JwtFilter`** — on each request, if a `Bearer` token is present it validates it and sets an `Authentication` whose authority is derived from the token's `role` claim via `Role.authority()` (`ROLE_USER` / `ROLE_ADMIN`). Requests to protected paths without a valid token are rejected by Spring Security (`401`).
- **Authorization** — `POST /api/activity` is annotated `@PreAuthorize("hasRole('ADMIN')")`; a non-admin token gets `403`. Because method security is enabled *and* the filter now grants real authorities, this is actually enforced.

> ⚠️ `POST /auth/register` honors the requested `role`, so anyone can self-register as `ADMIN`. Acceptable for this demo; not production-safe.

## API reference

JSON bodies. `/auth/**` is public; all `/api/**` paths require `Authorization: Bearer <token>`.

### Auth — `/auth`

#### `POST /auth/register`
Create a user and return a JWT.

Request:
| Field | Type | Notes |
|-------|------|-------|
| `firstName`, `lastName` | String | |
| `email` | String | unique |
| `password` | String | BCrypt-hashed before storage |
| `role` | enum | `USER` \| `ADMIN` — optional, defaults to `USER`; honored as-is (see warning above) |

Response `200 OK`: a raw JWT string (not JSON-wrapped).

#### `POST /auth/login`
Authenticate and return a JWT. Request: `{ "email", "password" }`.
- `200 OK` → raw JWT string
- `401 Unauthorized` → `ProblemDetail` (`"Invalid email or password"`) — same message whether the email is unknown or the password is wrong (no user enumeration).

### Activity (proxied) — `/api/activity`

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `GET` | `/api/activity` | authenticated | list all activities |
| `GET` | `/api/activity/{name}` | authenticated | one activity by name (`404` if missing) |
| `POST` | `/api/activity` | **ADMIN** | create an activity (`403` for non-admins) |

Request/response bodies mirror the Activity Service (`name`, `category`, `xpMultiplier`, `active`, `description`, `createdAt`) — see [API.md](../API.md) and the [activity-service README](../activity-service/README.md).

### Activity Log (proxied) — `/api/activitylog`

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `GET` | `/api/activitylog/{id}` | authenticated | one log by id |
| `POST` | `/api/activitylog` | authenticated | record a session (computes XP, notifies gamification) |
| `GET` | `/api/activitylog/user/{id}` | authenticated | all logs for a user |

### Misc

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `GET` | `/api/hello` | authenticated | returns `"Hello {email}"` — diagnostic |

## Data model

**`user_entity`** (unique on `email`):
| Column | Type | Notes |
|--------|------|-------|
| `id` | bigint | PK |
| `first_name` / `last_name` | String | |
| `email` | String | unique |
| `password` | String | BCrypt hash |
| `role` | enum (STRING) | `USER` \| `ADMIN` |

## Configuration

| Var / key | Default | Purpose |
|-----------|---------|---------|
| `JWT_SECRET` | (committed dev fallback) | HS256 signing secret — override in prod |
| `JWT_EXPIRATION` | `86400000` (24h, ms) | token lifetime — read via `@Value("${jwt.expiration}")` |
| `SPRING_DATASOURCE_URL` / `USERNAME` / `PASSWORD` | postgres defaults | users DB |
| `server.port` | `8080` | HTTP port |
| `eureka.client.service-url.defaultZone` | `http://eureka-server:8761/eureka` | registry |

See root [`.env.example`](../.env.example).

## Inter-service dependencies

- **Calls:** activity-service via `ActivityClient` (`@FeignClient(name = "activity-service")`) — activity + activity-log endpoints.
- **Declared but unused:** `GamificationClient` exists in `client/`, but no controller currently proxies gamification (`/level`, `/threshold`) through the gateway.
- **Called by:** external clients.
- **Infra:** Eureka, PostgreSQL.

## Running

```bash
docker-compose up --build          # whole stack, from repo root
# or standalone (needs Postgres + Eureka; activity-service for proxied calls):
cd api-gateway && mvn spring-boot:run
```

### Quick auth walkthrough
```bash
# 1. register (returns a token)
TOKEN=$(curl -s -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{"firstName":"Ada","lastName":"L","email":"ada@example.com","password":"secret","role":"USER"}')

# 2. call a protected endpoint
curl http://localhost:8080/api/activity -H "Authorization: Bearer $TOKEN"
```

## Testing

```bash
mvn test          # from api-gateway/
```

Includes `@WebMvcTest` controller tests and auth tests.

> ⚠️ `ApiGatewayApplicationTests` (full `@SpringBootTest` context load) needs the `postgres` host and only resolves inside the docker-compose network — it fails under a bare local `mvn test`. This is a pre-existing test-config gap, not a code defect.

## Troubleshooting

- **`401` on every `/api/**` call** — missing/expired/invalid `Bearer` token; re-login.
- **`403` on `POST /api/activity`** — the token's role is `USER`, not `ADMIN`. Register/login as an admin.
- **`/actuator/health` 404s** — actuator not yet wired ([issue #28](https://github.com/prashant-singh-2001/gamified_tracker/issues/28)); note it would also need `/actuator/**` permitted in `SecurityConfig`.

## Related docs

- [Root README](../README.md) · [API.md](../API.md) · [activity-service README](../activity-service/README.md)
