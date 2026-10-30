# FerryWala API

Base path `/api`. JSON in, JSON out. Auth: `Authorization: Bearer <jwt>` from login/register.
Errors always look like `{ "code": "...", "message": "human readable", "details": { "field": "message" } }`.

| Role | Meaning |
|---|---|
| public | no token |
| any | any signed-in user |
| CUSTOMER / VENDOR / ADMIN | enforced on the server in `SecurityConfig` + services (ownership checks) |

## Auth
| Method & path | Who | Notes |
|---|---|---|
| POST `/auth/register` | public | `{name, username, email, password, role: CUSTOMER\|VENDOR, shopName?, category?}`. 409 `Email already registered.` / `Username already taken.` Returns `{token, user}`; `token` is null when email confirmation is required first. ADMIN cannot self-register. |
| POST `/auth/login` | public | `{identifier (email or username), password}`. Same error for unknown user and wrong password. Rate limited. |
| GET `/auth/me` | any | current user |
| GET `/auth/verify-email?token=` | public | confirms email (token stored hashed, 48 h) |
| POST `/auth/resend-verification` | any | rate limited |
| POST `/auth/forgot-password` | public | `{email}`. Always answers the same message whether or not the account exists. Emails a single-use link (token stored hashed, 1 h). Rate limited. |
| POST `/auth/reset-password` | public | `{token, password}`. Password needs 8-72 chars with a letter and a number. 400 `INVALID_TOKEN` if used, wrong or expired. Rate limited. |

## Public browsing
| GET `/vendors/nearby?lat&lng&radiusKm&category&q&onlineOnly` | public | sorted by distance |
|---|---|---|
| GET `/vendors/{id}` | public | `{vendor, products}` |
| GET `/vendors/{id}/reviews` | public | |
| GET `/media/vendors/{file}` | public | uploaded shop photos |

## Customer
| POST `/orders` | CUSTOMER | `{vendorId, items:[{productId, quantity}], note?, latitude, longitude, areaName?}`. Prices are computed on the server. |
|---|---|---|
| GET `/orders/mine` | CUSTOMER | |
| POST `/orders/{id}/cancel` | CUSTOMER | only PENDING or ACCEPTED |
| POST `/orders/{id}/review` | CUSTOMER | `{rating 1-5, comment?}`, completed orders only, once |
| POST `/demand/signals` | CUSTOMER | `{type: REQUEST\|SEARCH\|VIEW, productName, latitude, longitude, areaName?}` |
| GET `/notifications`, POST `/notifications/read-all`, POST `/notifications/{id}/read` | any | |

## Vendor (`/vendor/**` requires VENDOR)
| GET/PUT `/vendor/me` | profile |
|---|---|
| PUT `/vendor/me/status` | `{online}` |
| PUT `/vendor/me/location` | `{latitude, longitude, area?, address?}` |
| POST `/vendor/me/image` | multipart `file`; JPEG/PNG/WebP, max 2 MB; type verified by content, re-encoded, EXIF stripped, stored on disk with a random name |
| DELETE `/vendor/me/image` | |
| GET `/vendor/stats`, GET `/vendor/reviews` | |
| GET `/vendor/opportunities?lat&lng&radiusKm` | "Today's best opportunity" + other areas (see below) |
| GET/POST `/vendor/products`, PUT/DELETE `/vendor/products/{id}`, PATCH `/vendor/products/{id}/availability` | |
| GET `/vendor/orders?status=`, PUT `/vendor/orders/{id}/status` | PENDING→ACCEPTED/REJECTED; ACCEPTED→ON_THE_WAY/COMPLETED/CANCELLED; ON_THE_WAY→COMPLETED/CANCELLED. Stock is reserved on accept and restored on cancel. |

## AI Concierge, meeting, support (any signed-in user; every order check returns a uniform 404 unless the caller is the customer or the vendor on that request)
| Endpoint | Notes |
|---|---|
| GET `/concierge/status` | name, whether a language model is enabled, retention days, support contact |
| POST `/concierge/chat` `{message<=800, orderId?, language?}` | returns `{reply:{text,cards,actions,suggestions,verified}, provider, at}`; `actions` are proposals only |
| GET / DELETE `/concierge/history` | last 60 messages / delete all of mine |
| GET `/orders/{id}/meeting` | meeting point, updates, messages, my share state, other party's share (coordinates only while they are actively sharing and the request is ACCEPTED/ON_THE_WAY) |
| POST `/orders/{id}/meeting/updates` `{type,etaMinutes?,note?,source?}` | RUNNING_LATE, PROPOSE_TIME (need ETA), ARRIVED, CANT_FIND, NEED_HELP. Only while ACCEPTED/ON_THE_WAY |
| POST `/orders/{id}/messages` `{body<=500}` | while PENDING/ACCEPTED/ON_THE_WAY |
| PUT / DELETE `/orders/{id}/location-share` | PUT `{latitude,longitude,durationMinutes 5-60}`; refreshing never extends expiry; DELETE removes the stored location |
| POST `/support/cases`, GET `/support/cases/mine` | `{category, orderId?, summary 5-1000, source}` |
| GET `/admin/support/cases?status=`, PUT `/admin/support/cases/{id}` | ADMIN only; `{status, resolutionNote?}` |

Rate limits: chat is limited per user and per IP; meeting updates, messages, location refreshes and case creation have their own limits (see `RateLimitFilter`).

## Admin (`/admin/**` requires ADMIN)
`GET /admin/stats`, `GET /admin/users?q&page&size`, `PUT /admin/users/{id}/enabled`, `GET /admin/vendors`, `PUT /admin/vendors/{id}/verified`, `GET /admin/products`, `DELETE /admin/products/{id}`, `GET /admin/orders?status=`, `GET /admin/demand/areas`, `GET /admin/suspicious`.

## Realtime (STOMP over SockJS at `/ws`)
Read-only for clients (they can never SEND). `/topic/vendors/status` is public (vendor online/offline). `/user/queue/orders` and `/user/queue/notifications` need the JWT in the CONNECT `Authorization` header.

## Demand scoring (explainable MVP)
Signals: purchase 3.0, pre-order 2.5, request 2.5, replenishment 2.0, search 1.0, view 0.5. Each signal decays with a 7-day half-life over a 30-day window; one customer counts once per product per day (strongest wins). Signals are grouped in ~1.1 km grid cells. Score = `100 × (1 − e^(−weight/10))`; HIGH ≥ 65, MEDIUM ≥ 35. Customers are shown as a **range**; the best 3-hour window comes from the hours signals actually occurred in (falls back to a flagged "typical" evening window). Every result carries a confidence level, the reasons behind it and a disclaimer. Nothing is guaranteed.

## Other
`GET /actuator/health` for health checks. Rate limits apply to login, register, resend-verification, demand signals, order placement and image upload.
