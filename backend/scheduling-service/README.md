# Scheduling Service (Test Drives)

Standalone Spring Boot microservice owning **seller availability** and **test-drive
booking lifecycle**, designed to sit alongside a separate Car Discovery Service and
User Management Service.

## How overlap safety actually works (read this first)

Two layers, and both matter:

1. **Application pre-check** (`TestDriveRepository.findOverlapping`) — runs before
   insert, gives a clean `409` in the common case.
2. **PostgreSQL partial exclusion constraint** (`V2__test_drive_exclusion_constraint.sql`):

   ```sql
   ALTER TABLE test_drive
     ADD CONSTRAINT excl_test_drive_no_overlap
     EXCLUDE USING gist (
       car_id WITH =,
       tsrange(start_time, end_time, '[)') WITH &&
     )
     WHERE (status IN ('PENDING', 'CONFIRMED'));
   ```

   This is the actual source of truth. It's enforced by Postgres itself on every
   `INSERT`/`UPDATE`, so it holds even with N service instances running behind a
   load balancer — there is no in-JVM lock to coordinate. If two concurrent
   requests both pass step 1 and both try to commit, the second commit throws
   `DataIntegrityViolationException`, which `GlobalExceptionHandler` turns into a
   `409 SCHEDULING_CONFLICT`.

   Requires the `btree_gist` extension (created in the same migration) to combine
   a scalar equality column (`car_id`) with a range-overlap operator in one
   exclusion constraint.

Status-transition endpoints (`confirm` / `reject` / `cancel` / `reschedule`)
additionally take a `SELECT ... FOR UPDATE` row lock
(`TestDriveRepository.findByIdForUpdate`) on the specific booking, so two
concurrent actions on the *same* row (e.g. seller confirms while buyer cancels)
serialize correctly instead of racing on a stale read.

**Reschedule is a single atomic `UPDATE`** on the existing row inside one
transaction — never a cancel-then-recreate — so the exclusion constraint
protects it exactly the same way it protects a fresh `INSERT`.

Transactions are kept short: the outbox row is written in the *same* transaction
as the state change, but the actual Kafka publish happens later, out-of-band, in
`OutboxPublisher` (a `@Scheduled` poller) — a slow/unavailable broker can never
block or fail a booking transaction.

## Cross-cutting concerns (AOP)

Two aspects, both under `aop/`:

- **`AuditAspect`** — around every `@Audited`-annotated service method (create /
  confirm / reject / cancel / reschedule on `TestDriveService`). Logs actor
  (userId + role, pulled from the `CurrentUser` argument), method, and outcome
  (success with resulting status, or failure with the exception message) in one
  place, so the audit trail can't be silently skipped on a new code path. Swap
  the `log.info` calls for a persisted `audit_log` table or an outbox event if
  you need it queryable later — that's a self-contained change inside the aspect.
- **`ServiceTimingAspect`** — wraps every public method in
  `com.cardrive.scheduling.service` and logs execution time, flagging anything
  over 500ms as `SLOW`. Since the concurrency design depends on transactions
  staying short, this is a cheap tripwire for a booking operation that
  accidentally starts doing slow work inside `@Transactional`.

## API documentation (OpenAPI / Swagger)

Springdoc is wired in; with the app running:

- Swagger UI: `http://localhost:8083/swagger-ui.html`
- Raw spec: `http://localhost:8083/v3/api-docs`

Both paths are `permitAll` in `SecurityConfig` (everything else still requires a
valid JWT). Controllers carry `@Tag`/`@Operation`/`@ApiResponse` annotations and
a global `bearerAuth` security scheme (`OpenApiConfig`) so "Authorize" in the UI
lets you paste a JWT and try requests directly.

## Testing

- **`TestDriveServiceTest` / `AvailabilityServiceTest`** (Mockito, no Spring
  context) — the real unit-test coverage: every authorization rule, every state
  transition (valid and invalid), the availability-window fit check, the
  application-level overlap pre-check, and — importantly — a test asserting the
  service does *not* swallow a `DataIntegrityViolationException` from the DB
  (i.e. it's left for `GlobalExceptionHandler` to turn into the 409).
- **`TestDriveControllerTest`** (`@WebMvcTest`, security filters disabled,
  service mocked) — HTTP status codes, validation error shape, and the
  exception-handler mapping (`ConflictException` → 409, `ForbiddenException` →
  403, bean-validation failure → 400).
- **`OverlapExclusionConstraintIT`** (Testcontainers + real PostgreSQL) — the
  test that actually matters for the "no two conflicting bookings" requirement:
  fires 10 concurrent overlapping inserts for the same car at real Postgres and
  asserts exactly one commits, with every rejection carrying SQLSTATE `23P01`
  (`exclusion_violation`). Confirms the guarantee lives in the database, not in
  application code.

```bash
mvn test                                    # unit + web-layer tests
mvn test -Dtest=OverlapExclusionConstraintIT # concurrency proof (needs Docker)
```

## Identity (matches auth-service)

Wired directly against `carventure-authservice`'s actual JWT format — not a generic
OIDC setup:

- **Username-keyed, not numeric.** auth-service's `User` entity is keyed by
  `username` — there's no numeric user id anywhere in that service. So `car_id`
  stays `BIGINT` (owned by Car Discovery Service) but every seller/buyer
  reference here (`availability.seller_username`, `test_drive.seller_username`,
  `test_drive.buyer_username`, `car_schedule_config.seller_username`) is a
  `VARCHAR`, sourced from the JWT's `sub` claim.
- **Symmetric HMAC verification, not JWKS.** auth-service's `JwtUtil` signs
  with `Keys.hmacShaKeyFor(secret)` — a shared secret, no issuer-discovery
  document or JWKS endpoint exists. `SecurityConfig.jwtDecoder()` verifies with
  the same base64 `jwt.secret` instead of `issuer-uri`/`jwk-set-uri`. **This
  secret must be byte-for-byte identical to auth-service's deployed secret** —
  share one `JWT_SECRET` env var across both services.
- **Algorithm is secret-length-dependent.** jjwt 0.12.x auto-selects the HMAC
  algorithm by key length when `signWith(Key)` is called without an explicit
  algorithm. auth-service's default 58-byte secret lands on **HS384**
  (satisfies HS384's 48-byte minimum, not HS512's 64-byte minimum). Set
  `jwt.mac-algorithm` to match if that secret ever changes length — otherwise
  every token silently fails verification. Worth flagging to whoever owns
  auth-service: pinning an explicit algorithm there instead of relying on
  key-length inference would remove this footgun for every downstream service.
- **Roles are plural and multi-valued.** auth-service puts a `"roles"` JSON
  array in the token (`CUSTOMER`, `SELLER`, `ADMIN` — note **`CUSTOMER`**, not
  `BUYER`), and explicitly allows a user to hold `CUSTOMER` *and* `SELLER` at
  the same time. `CurrentUser.roles` is a `Set<Role>`; `isBuyer()`/`isSeller()`/
  `isAdmin()` check membership, so a dual-role user can request a test drive on
  someone else's car and manage availability on their own in the same session.
- Car Discovery Service is **assumed** to expose `sellerUsername` on `CarDTO`
  the same way — if it currently returns a numeric `sellerId` instead, that's
  the next thing to align platform-wide, not something fixable from this side
  alone.

## Assumptions made (flag these to the team)

- **Test-drive duration is per-car, not global.** Since Car Discovery Service owns
  car catalog data but the prompt didn't specify where duration lives, this
  service owns a small `car_schedule_config(car_id, seller_username, duration_minutes)`
  table, defaulting to 45 minutes and upsertable via
  `CreateAvailabilityRequest.durationMinutes`.
- **Availability visibility (`GET /api/v1/cars/{carId}/availability`) is open to
  any authenticated role**, since a buyer needs to see it to pick a slot, not
  just the owning seller.
- **Reschedule resets status to `PENDING`**, requiring the seller to re-confirm —
  since a materially different time slot is effectively a new request. Flag if
  you'd rather preserve `CONFIRMED` across a reschedule.
- **Admin authorization** is enforced with a simple `roles.contains(ADMIN)` check in
  `TestDriveService.getAllForAdmin`, even though the spec says the Scheduling
  Service "does not need Admin-specific authorization" for the main flows —
  interpreted as "no complex admin logic," not "no admin route at all," since
  admins viewing all bookings is an explicit requirement.
- Car existence/ownership is resolved via a Feign client
  (`CarDiscoveryClient`) to the Car Discovery Service — the client-supplied
  `carId` is validated there, and `sellerUsername` is *always* taken from that
  response, never trusted from the request body.

## Endpoints

**Availability (seller-owned)**
- `POST   /api/v1/availability`
- `PUT    /api/v1/availability/{id}`
- `DELETE /api/v1/availability/{id}` (soft deactivate)
- `GET    /api/v1/cars/{carId}/availability`

**Test drives**
- `POST /api/v1/test-drives`
- `POST /api/v1/test-drives/{id}/confirm`
- `POST /api/v1/test-drives/{id}/reject`
- `POST /api/v1/test-drives/{id}/cancel`
- `POST /api/v1/test-drives/{id}/reschedule`
- `GET  /api/v1/test-drives/mine` (buyer: own requests, seller: requests on their cars)
- `GET  /api/v1/test-drives` (admin only — all bookings)

## Running locally

```bash
docker compose up --build
```

Brings up Postgres, Kafka+Zookeeper, and the service on `:8083`. Point
`CAR_DISCOVERY_SERVICE_URL` at wherever that service actually runs.

## Not included (deliberately)

- The OAuth authorization server / login flow itself (deferred, per your note).
- Car Discovery Service and User Management Service — only their contracts
  (`CarDiscoveryClient`) are assumed.
