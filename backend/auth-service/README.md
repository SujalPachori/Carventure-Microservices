# auth-service

CarVenture's **Authentication & Authorization Service** (SRS Spring API Service #1).
Registers and authenticates users, issues role-scoped JWT tokens, and manages
role assignments under an admin-only, multi-role model.

## Role model

Three roles exist: `CUSTOMER`, `SELLER`, `ADMIN`.

- **A user may hold `CUSTOMER` and/or `SELLER` simultaneously** - a person can browse
  to buy and list cars to sell at the same time.
- **`ADMIN` is always mutually exclusive with both.** An admin account can never
  also be a buyer or seller, and a buyer/seller can never also be promoted to admin
  while still holding those roles. This is enforced in `RoleServiceImpl` on every
  role grant, not just documented as a convention.
- **Self-registration always assigns `CUSTOMER` only.** `RegisterRequestDTO` has no
  role field at all - it's not validated-and-rejected, it structurally does not
  exist on the payload, so there's no way to smuggle a role through registration
  (verified in `AuthControllerTest` with a raw-JSON request that tries anyway).
- **Granting `SELLER` or `ADMIN` requires an existing admin** to call the
  role-management endpoint below.

## Endpoints

| Endpoint | Method | Auth required | Description |
|---|---|---|---|
| `/api/auth/register` | POST | No | Self-register. Always assigned `CUSTOMER` only. |
| `/api/auth/login` | POST | No | Authenticate and issue an access + refresh token pair |
| `/api/auth/refreshToken` | POST | No | Exchange a valid refresh token for a new token pair |
| `/api/auth/admin/users/{username}/roles` | POST | **Admin only** | Grant a role to an existing user |
| `/api/auth/admin/users/{username}/roles` | GET | **Admin only** | View a user's current roles |

Swagger UI: `http://localhost:8080/swagger-ui.html`

## Security model (read this before deploying)

**Authorization is DB-backed, not JWT-backed.** `CustomUserDetailsService` re-fetches
a user's current roles from the database on every authenticated request - the JWT's
`roles` claim is purely informational (useful for a client to render UI without an
extra call), never the source of truth for what a request is allowed to do. This
means a role grant or revocation by an admin takes effect on the affected user's
very next request, with no window where a just-demoted user can keep acting on
stale permissions until their token expires.

This tradeoff costs a DB read per request in exchange for immediate revocation. For
a small team/project this is the right default; if you later need to scale down DB
load, the alternative is trusting the JWT's roles claim directly, accepting a
propagation delay of up to `jwt.access-token-expiration-ms` for role changes - see
inline comments in `JwtUtil` and `CustomUserDetailsService` if you want to switch.

**Two independent layers enforce the admin-only routes** (defense in depth):
1. `SecurityConfig`'s `requestMatchers("/api/auth/admin/**").hasRole("ADMIN")`
2. `@PreAuthorize("hasRole('ADMIN')")` on each `RoleController` method

Removing either layer still leaves the other one enforcing the boundary.

**The bootstrap problem:** if only admins can grant the admin role, how does the
*first* admin get created? `AdminBootstrapRunner` solves this: on startup, if
`ADMIN_BOOTSTRAP_USERNAME` and `ADMIN_BOOTSTRAP_PASSWORD` are both set as real
environment variables (nothing happens by default) *and* no admin currently exists,
it creates exactly one admin account. This is deliberately NOT a public HTTP
endpoint - "become an admin" over HTTP, even behind a shared secret, is a much
larger attack surface than an operator-controlled, env-var-gated startup task.
**Set these two vars for the deployment's first boot only, verify the account
works, then unset them and rotate the password directly in the database.**

## Setup

1. Create a Postgres database:
   ```sql
   CREATE DATABASE carventure_auth;
   ```
2. Set environment variables (or edit `application.yml` directly):
   ```
   DB_USERNAME=postgres
   DB_PASSWORD=postgres
   JWT_SECRET=<base64-encoded-secret-at-least-32-bytes>
   ```
   Generate a secret: `openssl rand -base64 32`
3. **First boot only** - create your first admin:
   ```
   ADMIN_BOOTSTRAP_USERNAME=admin.bootstrap
   ADMIN_BOOTSTRAP_PASSWORD=<a-strong-password>
   ```
   Then unset both and restart, or rotate the password immediately.
4. Run:
   ```
   mvn spring-boot:run
   ```
   Service starts on port **8080**.

## How it fits the platform

Per the SRS, this service only owns **credentials** (`username`, `password`,
`roles`) — full profile data (name, address, contact info) belongs to the separate
**User Management Service**, which identifies callers via this service's issued
JWTs once the API Gateway is in place.

## What's deliberately NOT here yet

Per the phased build plan:
- **API Gateway / Eureka / Config Server** — not built yet; this service currently
  runs standalone on a hardcoded port. Once the gateway is built, routing +
  centralized JWT validation will move there.
- **A role-revocation endpoint** — only granting a role exists
  (`POST .../roles`), not removing one. A natural follow-up if needed.
- **User Management, Car Inventory, Transaction, Communication, Engagement,
  Admin & Reporting services** — separate services, not yet built.
- Social login (Google/Facebook) — SRS marks this optional; can be added as a
  follow-up (`/oauth2/...` endpoints via Spring Security OAuth2 Client).

## Tests

```
mvn test
```

Covers:
- `AuthServiceImplTest` — register/login/refresh, including proof that registration
  always assigns `CUSTOMER` only regardless of any request content
- `RoleServiceImplTest` — the core exclusivity business rule: granting SELLER to a
  CUSTOMER (allowed), granting ADMIN to a CUSTOMER/SELLER (rejected in both
  directions), duplicate role grants (rejected)
- `AuthControllerTest` — all 3 public endpoints, **including a test that sends raw
  JSON with a smuggled `"role":"ADMIN"` field and confirms it's silently ignored**
- `RoleControllerTest` — imports the *real* `SecurityConfig` (not mocked) to prove
  the admin-only enforcement actually rejects non-admin and unauthenticated callers
  with 403/401, not just that the annotation is present
- `JwtUtilTest` — multi-role token generation/validation/expiry
- `AdminBootstrapRunnerTest` — bootstrap only fires when both env vars are set and
  no admin exists yet; skips if an admin already exists or the username is taken
- `UserRepositoryTest` — `@DataJpaTest` against H2, including persisting multiple
  roles on one user
- `AuthServiceApplicationTests` — full context load smoke test
