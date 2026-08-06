# Hướng dẫn Access Token và Refresh Token của PetHub

Tài liệu này mô tả cơ chế xác thực đang dùng trong backend Spring Boot và frontend React của PetHub Ecommerce.

## 1. Thiết kế hiện tại

- Access token là JWT, thời hạn mặc định `900` giây (15 phút).
- Refresh token là chuỗi ngẫu nhiên opaque, thời hạn mặc định `30` ngày.
- MySQL chỉ lưu SHA-256 hash của refresh token; không lưu raw token.
- Raw refresh token chỉ được gửi bằng cookie `HttpOnly` có path `/auth`.
- Access token được trả trong JSON và frontend gửi qua `Authorization: Bearer ...`.
- Mỗi lần `POST /auth/refresh` thành công, refresh token cũ bị thu hồi và token mới được phát hành.
- `POST /auth/logout` thu hồi phiên hiện tại.
- `POST /auth/logout-all` thu hồi toàn bộ phiên của tài khoản.
- Đổi mật khẩu hoặc vô hiệu hóa tài khoản phải làm các refresh session cũ mất hiệu lực.

Backend dùng session stateless; cookie refresh token không thay thế access token cho API nghiệp vụ.

## 2. Cấu hình môi trường

### Localhost

```properties
JWT_ACCESS_TOKEN_EXPIRATION_SECONDS=900
JWT_REFRESH_TOKEN_EXPIRATION_DAYS=30
JWT_REFRESH_COOKIE_NAME=refreshToken
JWT_REFRESH_COOKIE_SECURE=false
JWT_REFRESH_COOKIE_SAME_SITE=Lax
CORS_ALLOWED_ORIGINS=http://localhost:5173
JPA_DDL_AUTO=validate
FLYWAY_ENABLED=true
```

### Production hiện tại

Frontend và API cùng site `dtvi.id.vn`, đều dùng HTTPS:

```properties
JWT_REFRESH_COOKIE_SECURE=true
JWT_REFRESH_COOKIE_SAME_SITE=Lax
CORS_ALLOWED_ORIGINS=https://pethub.dtvi.id.vn
JPA_DDL_AUTO=validate
FLYWAY_ENABLED=true
```

Không thêm dấu `/` cuối `CORS_ALLOWED_ORIGINS`. Nếu chạy đồng thời nhiều origin, phân cách bằng dấu phẩy.

`SameSite=None` chỉ cần khi frontend và backend thực sự khác site. Nếu dùng `None`, bắt buộc `JWT_REFRESH_COOKIE_SECURE=true`.

Không commit `JWT_SECRET`, password database, `VNPAY_HASH_SECRET` hoặc AWS credentials. `JWT_SECRET` production phải đủ dài, ngẫu nhiên và khác môi trường local.

## 3. Database và Flyway

Bảng `refresh_tokens` phải được tạo bởi Flyway migration hiện hành. Không dùng `JPA_DDL_AUTO=update` và không chạy `database/create_refresh_tokens.sql` trên database đã được Flyway quản lý.

Kiểm tra migration:

```sql
SELECT installed_rank, version, description, success
FROM flyway_schema_history
ORDER BY installed_rank;
```

Kiểm tra session của một user mà không làm lộ raw token:

```sql
SELECT id, user_id, expires_at, revoked_at, replaced_by_token_id, device_info, created_at
FROM refresh_tokens
WHERE user_id = ?
ORDER BY created_at DESC;
```

## 4. API và dữ liệu trả về

### Đăng nhập

```http
POST /auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "Password123!"
}
```

Body thành công:

```json
{
  "accessToken": "eyJ...",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "user": {
    "id": 1,
    "name": "Nguyen Van A",
    "role": "USER"
  }
}
```

Response còn có `Set-Cookie: refreshToken=...; Path=/auth; HttpOnly; ...`. Production phải có thuộc tính `Secure`.

### Làm mới token

```http
POST /auth/refresh
Cookie: refreshToken=<cookie do trình duyệt quản lý>
```

Response trả access token mới và `Set-Cookie` thay refresh token. Client phải để browser/Postman cập nhật cookie; không giữ một bản raw refresh token riêng.

### Đăng xuất

```http
POST /auth/logout
POST /auth/logout-all
```

- Logout hiện tại có thể xử lý idempotent và trả `204 No Content`.
- Logout-all cần cookie hợp lệ để xác định tài khoản.
- Cả hai phải gửi cookie xóa với `Max-Age=0` và path `/auth`.

## 5. Test bằng PowerShell và curl.exe

Các lệnh dưới đây dùng cookie jar để giữ cookie `HttpOnly`.

### Login

```powershell
curl.exe -i -c cookies.txt -H "Content-Type: application/json" -d '{"email":"user@example.com","password":"Password123!"}' http://localhost:2424/auth/login
```

Sao chép `accessToken` trong body để test API bảo vệ:

```powershell
$accessToken = "<accessToken>"
curl.exe -i -H "Authorization: Bearer $accessToken" http://localhost:2424/user/profile
```

### Refresh và xoay vòng

```powershell
curl.exe -i -b cookies.txt -c cookies.txt -X POST http://localhost:2424/auth/refresh
```

Sau lệnh này, `cookies.txt` chứa refresh token mới; token cũ trong database phải có `revoked_at` và liên kết tới token thay thế.

### Logout phiên hiện tại

```powershell
curl.exe -i -b cookies.txt -c cookies.txt -X POST http://localhost:2424/auth/logout
```

Gọi refresh lại bằng cookie đã xóa phải nhận `401 REFRESH_TOKEN_MISSING`.

### Logout mọi thiết bị

Đăng nhập ở hai cookie jar riêng:

```powershell
curl.exe -s -c device-a.txt -H "Content-Type: application/json" -d '{"email":"user@example.com","password":"Password123!"}' http://localhost:2424/auth/login
curl.exe -s -c device-b.txt -H "Content-Type: application/json" -d '{"email":"user@example.com","password":"Password123!"}' http://localhost:2424/auth/login
curl.exe -i -b device-a.txt -c device-a.txt -X POST http://localhost:2424/auth/logout-all
curl.exe -i -b device-b.txt -c device-b.txt -X POST http://localhost:2424/auth/refresh
```

Lệnh cuối phải nhận `401 REFRESH_TOKEN_REVOKED`.

## 6. Tích hợp React/Axios

Axios instance phải bật credentials:

```javascript
const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || "http://localhost:2424",
  withCredentials: true,
});
```

Request interceptor gắn access token cho API nghiệp vụ:

```javascript
api.interceptors.request.use((config) => {
  const accessToken = getAccessToken();
  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`;
  }
  return config;
});
```

Response interceptor chỉ nên refresh một lần cho một nhóm request đang gặp `401 TOKEN_EXPIRED`:

1. Không refresh cho chính `/auth/login`, `/auth/refresh`, `/auth/logout`.
2. Khi token hết hạn, gọi `POST /auth/refresh` với `withCredentials: true`.
3. Lưu access token mới trong state/session hiện tại.
4. Gửi lại request ban đầu đúng một lần bằng cờ `_retry`.
5. Nếu refresh thất bại, xóa session frontend và chuyển về đăng nhập.
6. Nếu nhiều request cùng hết hạn, dùng một refresh promise chung để tránh xoay vòng cùng token nhiều lần.

Không đọc refresh token bằng JavaScript; cookie `HttpOnly` được thiết kế để JS không truy cập được. Không gắn access token đã hết hạn vào `/auth/refresh`; backend cũng bỏ qua JWT filter cho các endpoint auth public để tránh vòng lặp refresh.

## 7. Bộ test bắt buộc

| Trường hợp | Kết quả mong đợi |
| --- | --- |
| Login đúng | `200`, có access token và cookie HttpOnly |
| Login sai mật khẩu | `401 INVALID_CREDENTIALS`, không đặt cookie hợp lệ |
| Gọi API bảo vệ không token | `401` |
| Access token hợp lệ | API trả dữ liệu đúng user |
| Access token hết hạn | `401 TOKEN_EXPIRED` |
| Access token bị sửa | `401 TOKEN_INVALID` |
| Refresh không cookie | `401 REFRESH_TOKEN_MISSING` |
| Refresh hợp lệ | Token mới được phát hành, token cũ bị revoke |
| Dùng lại token cũ sau rotation | `401 REFRESH_TOKEN_REVOKED` |
| Refresh token hết hạn | `401 REFRESH_TOKEN_EXPIRED` |
| Logout hiện tại | Cookie bị xóa, phiên hiện tại không refresh được |
| Logout-all | Mọi cookie của user không refresh được |
| Đổi mật khẩu | Mọi refresh session cũ bị thu hồi |
| User bị disable | Login/refresh/access token cũ đều bị từ chối |
| User gọi API admin | `403 ACCESS_DENIED` |
| Hai request cùng refresh | Chỉ một luồng refresh; request còn lại dùng kết quả chung |

## 8. Mã lỗi xác thực

| HTTP | Code | Ý nghĩa |
| --- | --- | --- |
| 401 | `INVALID_CREDENTIALS` | Email hoặc mật khẩu sai |
| 401 | `TOKEN_EXPIRED` | Access token hết hạn |
| 401 | `TOKEN_INVALID` | Access token sai định dạng, chữ ký hoặc claims |
| 401 | `REFRESH_TOKEN_MISSING` | Không gửi cookie refresh token |
| 401 | `REFRESH_TOKEN_INVALID` | Refresh token không tồn tại |
| 401 | `REFRESH_TOKEN_EXPIRED` | Refresh token hết hạn |
| 401 | `REFRESH_TOKEN_REVOKED` | Refresh token đã logout hoặc đã được xoay vòng |
| 401 | `ACCOUNT_DISABLED` | Tài khoản bị vô hiệu hóa |
| 403 | `ACCESS_DENIED` | Tài khoản không có quyền truy cập |

## 9. Lỗi thường gặp

### Login có cookie nhưng refresh vẫn báo thiếu

Kiểm tra:

- Axios/fetch có `withCredentials: true` hoặc `credentials: "include"`.
- Backend CORS cho phép đúng origin frontend và `allowCredentials=true`.
- Production dùng HTTPS và `JWT_REFRESH_COOKIE_SECURE=true`.
- Cookie có path `/auth`; request refresh phải là `/auth/refresh`.
- Không gọi frontend Vercel cũ hoặc API hostname khác với hostname đã đặt cookie.

### Refresh liên tục hoặc nhiều request bị logout

Nguyên nhân thường là nhiều request đồng thời đều gọi refresh. Vì refresh token được rotation, request thứ hai dùng lại token cũ sẽ nhận `REFRESH_TOKEN_REVOKED`. Frontend phải dùng một refresh promise chung.

### Localhost không nhận cookie

Ở local HTTP phải dùng `JWT_REFRESH_COOKIE_SECURE=false`. Không dùng `SameSite=None` với `Secure=false` vì backend sẽ từ chối cấu hình này.

### Production gửi CORS error

Đảm bảo:

```properties
CORS_ALLOWED_ORIGINS=https://pethub.dtvi.id.vn
JWT_REFRESH_COOKIE_SECURE=true
```

Sau khi đổi biến môi trường phải restart/redeploy backend. Thay đổi frontend trên GitHub/Vercel cần deployment mới hoặc đợi Vercel tự động deploy commit production.
