# Carventure Frontend

Production-oriented React + Vite frontend generated from the supplied Carventure backend source.

## Run

```bash
npm install
npm run dev
```

The Vite dev server runs on `http://localhost:5173` and proxies frontend service prefixes to the backend ports:

| Frontend prefix | Backend | Default port |
|---|---|---:|
| `/backend/auth` | auth-service | 8080 |
| `/backend/car` | car | 8081 |
| `/backend/payment` | payment-financing-service | 8082 |
| `/backend/scheduling` | scheduling-service | 8083 |
| `/backend/governance` | admingovernance | 8084 |
| `/backend/reporting` | adminreporting | 8085 |
| `/backend/user` | usermanagement | 8086 |

Override proxy targets with the variables in `.env.example` when necessary.

## Authentication

The auth service issues an access token and a refresh token. The frontend stores both in local storage, injects `Authorization: Bearer <accessToken>` into service requests, refreshes on a 401 once, rotates the refresh token through `/api/auth/refreshToken`, and clears the session when refresh fails.

JWT roles are read from the backend's `roles` claim. The backend supports `CUSTOMER`, `SELLER`, and `ADMIN`.

## Implemented backend-backed areas

- Authentication: login, self-registration, refresh, logout/session cleanup.
- User profiles: customer/seller profile creation and updates.
- Car discovery: search/filter, details, recommendations endpoint, price suggestion.
- Seller inventory: create/edit/delete, status updates, document uploads, picture uploads.
- Test-drive scheduling: availability windows, buyer requests, seller confirm/reject/cancel, buyer cancel/reschedule, admin all-bookings.
- Purchase orders: create order, Stripe Payment Element, order listing, order cancellation.
- Admin: role assignment, customer/seller lists, seller account status, reporting, listing verification, dispute resolution by ID, all test drives.
- Responsive marketplace UI, loading/error/empty states and centralized API handling.

## Important backend contract gaps found during source inspection

These are not fabricated frontend omissions; they are limitations/inconsistencies in the supplied backend.

1. **Offers are not exposed.** There is no offer controller/service in the supplied source, so no offer UI or fake offer API was created.
2. **Notifications/SSE are not exposed.** No communication/notification controller or SSE endpoint exists in the supplied source. The frontend therefore does not poll or fabricate notifications.
3. **Payment uses numeric `buyerId`, while the auth/profile contract exposes `username`.** `UserDTO` does not expose `userId`, so checkout/order listing cannot safely infer the numeric ID. The UI asks for the real buyer ID instead of guessing.
4. **Seller-car endpoint has a path-variable mismatch.** `viewAllCarsBySellerId/{sellerId}` has a method parameter annotated as `@PathVariable("sellerUsername")`. The frontend avoids that broken endpoint and filters the actual `searchCars` DTO results by `sellerUsername`.
5. **Car file URLs are built inconsistently by the backend.** Uploaded document/image URLs are built under `/download/...`, while the controller is `/api/v1/car/download/{filename}`. The frontend normalizes returned `/download/{filename}` URLs to the actual controller route for rendering.
6. **Admin governance `@PreAuthorize` annotations are commented out.** The frontend still role-gates admin screens based on the auth role, but the backend currently enforces only authentication on those governance endpoints.
7. **CORS is not configured in the supplied services.** The frontend uses Vite development proxying so browser requests are same-origin during local development. A deployed frontend will need appropriate gateway/CORS configuration.
8. **FinancingPlanResponse exists but no controller endpoint exposes financing plans.** No unsupported financing-plan API call was invented.

## Backend services inspected

- auth-service
- car
- payment-financing-service
- scheduling-service
- admingovernance
- adminreporting
- usermanagement
- eureka-server-app (discovery infrastructure; the browser does not call Eureka directly)

Kafka/event/outbox behavior is backend-internal and does not require a browser client for the exposed flows in the supplied source.
