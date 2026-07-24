# Access token and refresh token guide

## Implemented flow

- Access tokens are JWTs and expire after 900 seconds by default.
- Refresh tokens are opaque random values and expire after 30 days by default.
- Only the SHA-256 hash of each refresh token is stored in MySQL.
- The raw refresh token is sent only through an `HttpOnly` cookie scoped to `/auth`.
- Refresh tokens are rotated whenever `POST /auth/refresh` succeeds.
- `POST /auth/logout` revokes the current refresh token.
- `POST /auth/logout-all` revokes every active refresh token belonging to the user.

## Environment configuration

Keep the development values below in `.env`:

```properties
JWT_ACCESS_TOKEN_EXPIRATION_SECONDS=900
JWT_REFRESH_TOKEN_EXPIRATION_DAYS=30
JWT_REFRESH_COOKIE_NAME=refreshToken
JWT_REFRESH_COOKIE_SECURE=false
JWT_REFRESH_COOKIE_SAME_SITE=Lax
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:5173
```

For production over HTTPS:

```properties
JWT_REFRESH_COOKIE_SECURE=true
JWT_REFRESH_COOKIE_SAME_SITE=Lax
CORS_ALLOWED_ORIGINS=https://your-frontend-domain.example
JPA_DDL_AUTO=validate
```

Never commit the real `JWT_SECRET`, database password, or AWS credentials.

## Database

With the current default `JPA_DDL_AUTO=update`, Hibernate creates the
`refresh_tokens` table on startup. If schema auto-update is disabled, run:

```text
database/create_refresh_tokens.sql
```

## API checks

The commands below use a cookie jar so the `HttpOnly` cookie is retained.

### Login

```bash
curl -i -c cookies.txt \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"your-password"}' \
  http://localhost:2424/auth/login
```

Successful response body:

```json
{
  "accessToken": "...",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "user": {
    "id": 1,
    "name": "...",
    "role": "USER"
  }
}
```

### Refresh and rotate

```bash
curl -i -b cookies.txt -c cookies.txt \
  -X POST http://localhost:2424/auth/refresh
```

The response contains a new access token and replaces the refresh-token cookie.

### Logout current device

```bash
curl -i -b cookies.txt -c cookies.txt \
  -X POST http://localhost:2424/auth/logout
```

### Logout every device

```bash
curl -i -b cookies.txt -c cookies.txt \
  -X POST http://localhost:2424/auth/logout-all
```

## React/Axios requirement

Requests that use the refresh cookie must enable credentials:

```javascript
const api = axios.create({
  baseURL: "http://localhost:2424",
  withCredentials: true,
});
```

Do not try to read the refresh token in JavaScript. The browser manages its
`HttpOnly` cookie. Use the returned `accessToken` for the `Authorization` header:

```text
Authorization: Bearer <accessToken>
```

Do not attach an expired access token to `/auth/refresh`; the backend also skips
JWT filtering for the public authentication endpoints to prevent refresh loops.

## Authentication error codes

| HTTP status | Code | Meaning |
| --- | --- | --- |
| 401 | `TOKEN_EXPIRED` | The access token has expired. |
| 401 | `TOKEN_INVALID` | The access token is malformed or has an invalid signature. |
| 401 | `REFRESH_TOKEN_MISSING` | The refresh-token cookie was not sent. |
| 401 | `REFRESH_TOKEN_INVALID` | The refresh token does not exist. |
| 401 | `REFRESH_TOKEN_EXPIRED` | The refresh token has expired. |
| 401 | `REFRESH_TOKEN_REVOKED` | The refresh token was already revoked or rotated. |
| 403 | `ACCESS_DENIED` | The user is authenticated but lacks permission. |
