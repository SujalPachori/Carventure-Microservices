# Carventure API Map

This map records the browser-facing routes implemented from the supplied controllers.

## Auth — 8080

- `POST /api/auth/register` — CUSTOMER or SELLER self-registration.
- `POST /api/auth/login` — credentials -> access/refresh token pair.
- `POST /api/auth/refreshToken` — rotate refresh token.
- `POST /api/auth/admin/users/{username}/roles` — ADMIN role grant.
- `GET /api/auth/admin/users/{username}/roles` — ADMIN role lookup.

## Car — 8081

- `POST /api/v1/car/addCar`
- `GET /api/v1/car/viewAllCars` (not used because it returns the entity rather than CarDTO)
- `GET /api/v1/car/viewAllCarsBySellerId/{sellerId}` (backend path-variable mismatch; avoided)
- `GET /api/v1/car/viewCar/{carId}`
- `PUT /api/v1/car/updateCar/{carId}`
- `PUT /api/v1/car/updateCarStatus/{carId}?status=...`
- `POST multipart /api/v1/car/uploadCarDocuments/{carId}` with `rc`, `insurance`, `puc`
- `DELETE /api/v1/car/deleteCar/{carId}`
- `GET /api/v1/car/searchCars`
- `GET /api/v1/car/suggestPrice?carId=...`
- `GET /api/v1/car/recommendations/{customerId}`
- `GET /api/v1/car/download/{filename}`
- `POST multipart /api/v1/car/uploadCarPictures/{carId}` with `img`
- `DELETE /api/v1/car/deleteCarPictures/{carId}?imgId=...`
- `GET /api/v1/car/viewCarPayments/{carId}`
- `GET /api/v1/car/viewCarSched/{carId}`

## Scheduling — 8083

- `POST /api/v1/availability`
- `PUT /api/v1/availability/{id}`
- `DELETE /api/v1/availability/{id}`
- `GET /api/v1/cars/{carId}/availability`
- `POST /api/v1/test-drives`
- `POST /api/v1/test-drives/{id}/confirm`
- `POST /api/v1/test-drives/{id}/reject`
- `POST /api/v1/test-drives/{id}/cancel`
- `POST /api/v1/test-drives/{id}/reschedule`
- `GET /api/v1/test-drives/mine`
- `GET /api/v1/test-drives`

## Payments — 8082

- `POST /api/v1/orders`
- `GET /api/v1/orders/{id}`
- `GET /api/v1/orders?buyerId=...` or `?sellerId=...`
- `POST /api/v1/orders/{id}/cancel?buyerId=...`
- Stripe webhook is server-to-server and is intentionally not called from the browser.

## User Management — 8086

- Customer registration/list/detail/update/delete.
- Seller registration/list/detail/update/delete.
- Seller account status update.
- Admin profile detail/update/delete.
- Username existence check.
- Dispute creation endpoint exists but its `DisputeDTO` is empty, so no useful dispute-creation form is fabricated.

## Admin Governance — 8084

- `PUT /api/v1/governance/verifyListing/{carId}`
- `PUT /api/v1/governance/resolveDispute/{disputeId}`

## Admin Reporting — 8085

- `GET /api/v1/reporting/viewReports`
