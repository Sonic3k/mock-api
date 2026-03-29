# Mock API — Comparison Tool Testing

A Spring Boot mock API simulating a legacy → modernized migration scenario, designed for testing the API Comparison Tool.

## Base URLs

| Environment | Base URL |
|---|---|
| Legacy | `/legacy/api` |
| Modernized | `/modernized/api` |

## Health Check

```
GET /health
```

## Domains & Endpoints (35 total)

| Domain | Endpoints |
|---|---|
| Users | GET /{id}, POST /, PUT /{id}/email, DELETE /{id}, GET / (list), GET /{id}/profile, PATCH /{id}/status, GET /search |
| Auth | POST /login, POST /register, POST /refresh, POST /logout, POST /forgot-password |
| Orders | GET /{orderId}, GET / (list), PATCH /{orderId}/cancel, GET /{orderId}/items, POST /, GET /{orderId}/tracking, GET /summary |
| Products | GET / (catalog), GET /{productId}, GET /search, POST /, GET /{productId}/inventory, GET /categories, PUT /{productId}/price |
| Payments | POST /, GET /{paymentId}, POST /{paymentId}/refund, GET / (list), GET /{paymentId}/receipt, POST /{paymentId}/capture |

## Test Distribution

| Result | % | Reason |
|---|---|---|
| ✅ Pass | ~50% | Same business logic, comparable response structure |
| ❌ Fail | ~25% | Field renames (snake_case→camelCase), type changes (ISO→epoch, String→Double), structure changes (flat→nested) |
| 🔴 Error | ~10% | 500 on specific data: `userId=20` (discount=100), suspended users on login |
| ⬜ Not Impl | ~15% | PATCH /users/{id}/status, POST /auth/logout, PATCH /orders/cancel, PUT /products/price, POST /payments/capture |

## Key Differences (Legacy vs Modernized)

- **Field naming**: `snake_case` → `camelCase` (e.g. `user_id` → `userId`, `total_count` → `total`)
- **Date format**: ISO string (`"2024-01-15T10:30:00Z"`) → Unix epoch (`1705316400`)
- **Price type**: String (`"99.99"`) → Double (`99.99`)
- **Address structure**: Flat fields → Nested `address` object
- **Pagination keys**: `total_count`/`total_records` → `total`/`totalItems`
- **List keys**: `items`/`results` → `products`/`data`
- **Auth tokens**: `token`/`token_type` → `accessToken`/`tokenType`

## Seeded Data

- 20 users (IDs 1–20)
- 15 products (PROD-001 to PROD-015)
- 20 orders (ORD-10001 to ORD-10020)
- 15 payments (PAY-20001 to PAY-20015)

## Running Locally

```bash
mvn spring-boot:run
```

Server starts on port 8080.
