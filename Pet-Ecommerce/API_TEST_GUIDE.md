# Hướng dẫn chạy và kiểm thử API PetHub Ecommerce

Tài liệu này áp dụng cho phiên bản hiện tại của dự án:

- Backend: Spring Boot 3, Java 21, cổng `2424`.
- Frontend: React/Vite, cổng phát triển `5173`.
- Cơ sở dữ liệu: MySQL 8, schema được quản lý bằng Flyway.
- Website production: `https://pethub.dtvi.id.vn`.
- Backend public qua Cloudflare Tunnel: `https://api.dtvi.id.vn`.
- VNPay: Sandbox; chức năng Refund đang tắt cho đến khi VNPay kích hoạt API.

## 1. URL và công cụ kiểm thử

| Môi trường | Base URL |
| --- | --- |
| Backend local | `http://localhost:2424` |
| Swagger local | `http://localhost:2424/swagger-ui.html` |
| Health check local | `http://localhost:2424/actuator/health` |
| Backend public | `https://api.dtvi.id.vn` |

Có thể dùng Swagger UI, Postman hoặc `curl.exe`. Trong Postman nên tạo các biến:

```text
baseUrl=http://localhost:2424
accessToken=<token nhận được sau khi đăng nhập>
userId=<id user>
productId=<id sản phẩm>
orderId=<id đơn hàng>
```

Các API cần đăng nhập phải gửi:

```http
Authorization: Bearer {{accessToken}}
```

Refresh token không nằm trong JSON. Backend đặt token này vào cookie `HttpOnly` có path `/auth`; Postman hoặc trình duyệt phải lưu và gửi lại cookie.

## 2. Chuẩn bị và chạy backend

Yêu cầu: JDK 21, MySQL 8 và các biến môi trường hợp lệ trong `.env`.

Các cấu hình schema bắt buộc:

```properties
JPA_DDL_AUTO=validate
FLYWAY_ENABLED=true
```

Không dùng `JPA_DDL_AUTO=update` đồng thời với Flyway. Không sửa các migration đã áp dụng; thay đổi schema mới phải tạo migration `V...__mo_ta.sql` tiếp theo.

Trên PowerShell:

```powershell
cd D:\LVTN_HC\Java\Pet-Ecommerce
.\mvnw.cmd clean test
.\mvnw.cmd spring-boot:run
```

Kiểm tra backend:

```powershell
curl.exe http://localhost:2424/actuator/health
```

Kết quả mong đợi có `"status":"UP"`.

### Chạy backend và MySQL bằng Docker

Đặt `Dockerfile`, `docker-compose.yml`, `.env`, `pom.xml`, `mvnw`, thư mục `.mvn` và `src` trong thư mục gốc backend, sau đó chạy:

```powershell
docker compose config
docker compose up --build -d
docker compose ps
docker compose logs -f backend
```

Dừng container nhưng giữ dữ liệu MySQL:

```powershell
docker compose down
```

Lệnh sau xóa cả volume và toàn bộ dữ liệu MySQL của bộ Compose; chỉ dùng khi thực sự muốn tạo lại database:

```powershell
docker compose down -v
```

Compose hiện chỉ chạy MySQL và backend. Nó không chạy frontend, Ollama hay Cloudflare Tunnel. Frontend vẫn chạy bằng Vite/Vercel; Ollama và tunnel phải chạy riêng.

## 3. Quy ước response và lỗi

Các API mới thường trả response dạng:

```json
{
  "status": 200,
  "code": "SUCCESS",
  "message": "...",
  "data": {},
  "timestamp": "2026-08-06T10:00:00"
}
```

Một số API sản phẩm/danh mục cũ dùng DTO `Response`, vì vậy tên trường có thể khác. Khi test cần kiểm tra cả HTTP status và trường `code`, không chỉ kiểm tra nội dung `message`.

Các kết quả bảo mật thường gặp:

| HTTP | Code | Ý nghĩa |
| --- | --- | --- |
| 400 | `VALIDATION_ERROR` hoặc `INVALID_REQUEST` | Payload hoặc tham số không hợp lệ |
| 401 | `INVALID_CREDENTIALS` | Sai email/mật khẩu |
| 401 | `TOKEN_EXPIRED` | Access token đã hết hạn |
| 401 | `TOKEN_INVALID` | Access token sai hoặc bị sửa |
| 403 | `ACCESS_DENIED` | Đã đăng nhập nhưng không đủ quyền |
| 404 | `RESOURCE_NOT_FOUND` | Không tìm thấy dữ liệu |
| 409 | `DATA_CONFLICT` | Dữ liệu trùng hoặc xung đột |
| 429 | mã giới hạn tần suất | Gửi yêu cầu quá nhanh |

## 4. Auth và phiên đăng nhập

### 4.1 Đăng ký

```http
POST /auth/register
Content-Type: application/json

{
  "email": "user@example.com",
  "name": "Nguyen Van A",
  "phoneNumber": "0912345678",
  "password": "Password123!"
}
```

Test thêm: email trùng, email sai định dạng, thiếu tên, số điện thoại sai, mật khẩu dưới 8 ký tự. Không được cho client tự đăng ký role `ADMIN`.

### 4.2 Đăng nhập

```http
POST /auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "Password123!"
}
```

Kết quả thành công phải có `accessToken`, `tokenType=Bearer`, `expiresIn=900`, thông tin user và header `Set-Cookie` chứa refresh token `HttpOnly`.

### 4.3 Refresh và logout

```http
POST /auth/refresh
POST /auth/logout
POST /auth/logout-all
```

- `/auth/refresh` dùng cookie và xoay vòng refresh token.
- `/auth/logout` thu hồi phiên hiện tại, trả `204` và xóa cookie.
- `/auth/logout-all` thu hồi mọi phiên của tài khoản.
- Không gửi refresh token trong body hoặc lưu nó vào `localStorage`.

Chi tiết và test bảo mật nằm trong `AUTH_REFRESH_TOKEN_GUIDE.md`.

### 4.4 Quên và đặt lại mật khẩu

```http
POST /auth/forgot-password
Content-Type: application/json

{"email":"user@example.com"}
```

```http
POST /auth/reset-password
Content-Type: application/json

{"token":"token-from-email","newPassword":"NewPassword123!"}
```

Khi `MAIL_ENABLED=false`, không dùng chức năng gửi mail như một test production. Khi bật mail, test token đúng, token sai, token hết hạn, token đã dùng và giới hạn tần suất.

## 5. Sản phẩm và danh mục

Các API đọc public:

```http
GET /category/get-all
GET /category/get-category-by-id/1
GET /product/get-all
GET /product/get-by-product-id/1
GET /product/get-by-category-id/1
GET /product/search?searchValue=cho
GET /products?q=cho&categoryId=1&petType=DOG&minPrice=50000&maxPrice=500000&page=0&size=20&sortBy=price&direction=asc
```

Admin tạo sản phẩm bằng `multipart/form-data`:

```text
POST /product/create
categoryId=1
image=<file ảnh>
name=Thức ăn chó con
description=Mô tả sản phẩm
price=150000
quantity=50
sku=DOG-FOOD-001
brand=Pet Brand
petType=DOG
```

Các API quản trị khác:

```http
PUT /product/update
DELETE /product/delete/{productId}
POST /category/create
PUT /category/update/{categoryId}
DELETE /category/delete/{categoryId}
GET /admin/products?page=0&size=10&q=thuc-an
GET /admin/products/{productId}
```

Test tối thiểu:

- User không có token vẫn xem được danh sách/chi tiết sản phẩm.
- User thường gọi API tạo/sửa/xóa phải nhận `403`.
- Giá phải lớn hơn 0 và là số nguyên VND; SKU không được trùng.
- Số lượng không được âm; sản phẩm hết hàng không thể thêm mới vào giỏ.
- Lọc `DOG` và `CAT` phải trả đúng dữ liệu; trạng thái active/inactive phải được tôn trọng ở catalog public.

## 6. Giỏ hàng và wishlist

```http
GET /cart
POST /cart/items
Content-Type: application/json

{"productId":1,"quantity":2}
```

```http
PATCH /cart/items/{itemId}?quantity=3
DELETE /cart/items/{itemId}
DELETE /cart
```

Mỗi dòng sản phẩm cho phép số lượng từ `1` đến `99`, đồng thời không được vượt tồn kho. Đây là giới hạn theo từng sản phẩm, không phải số loại sản phẩm trong toàn bộ giỏ.

Wishlist:

```http
GET /wishlist
POST /wishlist/{productId}
DELETE /wishlist/{productId}
```

Test thêm sản phẩm trùng, cập nhật về 0, cập nhật 100, vượt tồn kho, sản phẩm inactive, xóa item không thuộc user và gọi API không có token.

## 7. Mã ưu đãi

Admin quản lý mã:

```http
GET /coupons
POST /coupons
PUT /coupons/{id}
DELETE /coupons/{id}
```

Payload tạo/sửa:

```json
{
  "code": "WELCOME10",
  "description": "Giảm 10%",
  "type": "PERCENT",
  "value": 10,
  "minimumOrderValue": 200000,
  "maximumDiscount": 50000,
  "startsAt": "2026-08-01T00:00:00",
  "expiresAt": "2026-12-31T23:59:59",
  "usageLimit": 1000,
  "perUserLimit": 1,
  "active": true
}
```

User lấy mã đang khả dụng và kiểm tra mã đã chọn:

```http
GET /coupons/available?subtotal=300000
GET /coupons/validate?code=WELCOME10&subtotal=300000
```

Test các trường hợp: hợp lệ, chưa tới ngày bắt đầu, hết hạn, inactive, chưa đủ giá trị đơn tối thiểu, hết lượt toàn hệ thống, user đã hết lượt, phần trăm vượt `maximumDiscount`, mã FIXED lớn hơn subtotal và thay đổi giỏ sau khi validate. Backend phải tính lại coupon trong checkout; không tin số giảm giá do frontend gửi.

## 8. Checkout và đơn hàng COD

Checkout bắt buộc có `Idempotency-Key`, là chuỗi không rỗng và tối đa 64 ký tự. Mỗi lần người dùng chủ động tạo đơn mới dùng một UUID mới; khi retry do timeout phải dùng lại cùng key.

```http
POST /orders/checkout
Authorization: Bearer {{accessToken}}
Idempotency-Key: 6ca76078-79d6-4b10-8cb3-91d340fc74a1
Content-Type: application/json

{
  "recipientName": "Nguyen Van A",
  "recipientPhone": "0912345678",
  "street": "1 Nguyen Trai",
  "ward": "Ben Thanh",
  "district": "Quan 1",
  "city": "Ho Chi Minh",
  "country": "Viet Nam",
  "couponCode": "WELCOME10",
  "paymentMethod": "COD",
  "note": "Giao giờ hành chính"
}
```

API đơn hàng:

```http
GET /orders/my?page=0&size=10
GET /orders/{orderId}
POST /orders/{orderId}/cancel
```

Admin:

```http
GET /orders/admin/all?page=0&size=20&q=PET-&status=PENDING
PATCH /orders/admin/{orderId}/status
Content-Type: application/json

{"status":"CONFIRMED","adminMessage":"Đã xác nhận"}
```

Luồng giao hàng thông thường: `PENDING -> CONFIRMED -> PROCESSING -> SHIPPING -> DELIVERED`. Không được nhảy trạng thái trái quy tắc. Hủy đơn phải hoàn tồn kho và chỉ hoàn đúng một lần.

Test idempotency bằng cách gửi cùng payload và cùng key hai lần: hệ thống không được tạo hai đơn hoặc trừ kho hai lần. Gửi cùng key với payload khác phải bị từ chối.

## 9. Thanh toán VNPay Sandbox

Đổi `paymentMethod` trong checkout thành `VNPAY`. Đơn ban đầu phải ở `PENDING_PAYMENT`, sau đó gọi:

```http
POST /payments/vnpay/create/{orderId}
Authorization: Bearer {{accessToken}}
```

Mở `paymentUrl` được trả về trong trình duyệt và thực hiện thanh toán Sandbox.

Cấu hình production hiện tại:

```properties
VNPAY_RETURN_URL=https://pethub.dtvi.id.vn/payment/vnpay/return
```

IPN đăng ký trên VNPay Sandbox phải chính xác, không có dấu `/` kép:

```text
https://api.dtvi.id.vn/payments/vnpay/ipn
```

Các endpoint callback:

```http
GET /payments/vnpay/ipn
GET /payments/vnpay/return
```

Không tự tạo chữ ký hoặc sửa tham số callback. Return URL dùng để hiển thị kết quả; IPN hợp lệ mới là nguồn xác nhận server-to-server để chốt giao dịch.

Các case cần test:

1. Thanh toán thành công: payment thành `PAID`, đơn rời `PENDING_PAYMENT`, không còn nút “Thanh toán ngay”.
2. User hủy trên trang VNPay: đơn vẫn cho retry trong thời hạn thanh toán; không hoàn tồn kho quá sớm.
3. Retry: tạo payment attempt mới, transaction reference mới, nhưng vẫn cùng order.
4. Quá `VNPAY_PENDING_MINUTES` (mặc định 15): đơn pending hết hạn bị hủy và hoàn kho đúng một lần.
5. Callback sai chữ ký, sai số tiền, sai TmnCode hoặc transaction ref không tồn tại: không được đánh dấu paid.
6. Gửi lại cùng IPN thành công: phải idempotent, không cộng/trừ kho hay soldCount lần thứ hai.
7. Return đến trước IPN: giao diện có thể báo đang chờ; khi IPN đến, theo dõi đơn phải cập nhật đúng.

### Refund hiện đang tắt

Giữ cấu hình:

```properties
REFUND_ENABLED=false
```

Không hiển thị nút hoàn tiền và không đưa Refund vào kịch bản demo chính thức. Không được mô phỏng Refund thành công bằng cách sửa trực tiếp `paymentStatus`. Chỉ bật lại sau khi VNPay cấp API Refund/QueryDR và hoàn tất kiểm thử state machine, idempotency, hoàn kho và đối soát.

## 10. Đánh giá sản phẩm

```http
GET /reviews/product/{productId}?page=0&size=10
POST /reviews/product/{productId}
Content-Type: application/json

{"rating":5,"comment":"Sản phẩm tốt"}
```

```http
PUT /reviews/{reviewId}
DELETE /reviews/{reviewId}
DELETE /reviews/admin/{reviewId}
```

Chỉ user đã nhận đơn `DELIVERED` có sản phẩm đó mới được đánh giá; rating từ 1 đến 5. Test user chưa mua, đơn chưa delivered, đánh giá trùng, sửa/xóa review của người khác và admin ẩn/xóa review.

Số sao trung bình và số lượt hiển thị ở đầu trang phải được tính từ cùng dữ liệu review đã publish với danh sách bên dưới; không seed riêng `ratingAverage/ratingCount` nếu không có review tương ứng.

## 11. Hồ sơ và quản trị user

```http
GET /user/profile
PUT /user/profile
Content-Type: application/json

{"name":"Nguyen Van B","phoneNumber":"0987654321"}
```

```http
POST /user/change-password
Content-Type: application/json

{"currentPassword":"Password123!","newPassword":"NewPassword123!"}
```

Admin:

```http
GET /user/get-all?page=0&size=20&q=user@example.com
PATCH /user/admin/{userId}/enabled?enabled=false
```

Đổi mật khẩu phải thu hồi toàn bộ refresh session. User bị disable không được login/refresh và access token cũ phải bị từ chối.

## 12. Thống kê, audit log và RAG

Admin:

```http
GET /statistics/dashboard
GET /statistics/monthly?month=8&year=2026
GET /api/admin/audit-logs?page=0&size=20&sort=createdAt,desc
GET /api/admin/audit-logs/user/{userId}?page=0&size=20
```

Audit log là append-only; không có API sửa hoặc xóa.

Chatbot public:

```http
POST /rag/chat
Content-Type: application/json

{"conversationId":null,"question":"Tư vấn thức ăn cho mèo con"}
```

Admin quản lý tài liệu:

```http
GET /rag/admin/documents
POST /rag/admin/documents
PATCH /rag/admin/documents/{id}?active=false
DELETE /rag/admin/documents/{id}
```

Payload ingest:

```json
{
  "title": "Kiến thức dinh dưỡng",
  "sourceUrl": "https://example.com",
  "content": "Nội dung tài liệu..."
}
```

RAG cần Ollama và đủ model chat/embedding. Nếu Ollama chạy trên máy host còn backend chạy trong Docker, `localhost:11434` bên trong container là sai; cấu hình `AI_BASE_URL` tới host Docker hoặc chạy Ollama thành một service riêng.

## 13. Checklist trước khi demo

- `mvnw.cmd clean test` thành công.
- `npm ci`, `npm run lint`, `npm run build` thành công.
- `/actuator/health` trả `UP`.
- Flyway chạy thành công, Hibernate `validate` không báo lệch schema.
- Không commit `.env`, JWT secret, VNPay hash secret, AWS key hoặc mật khẩu DB.
- User thường không truy cập được API admin.
- Checkout lặp không tạo đơn trùng.
- Hủy đơn/timeout chỉ hoàn kho một lần.
- VNPay IPN dùng URL một dấu `/` và Cloudflare Tunnel đang hoạt động.
- Refund bị ẩn/tắt trong cả frontend và backend.
- Coupon được backend tính lại lúc checkout.
- Rating tổng hợp khớp danh sách review thực tế.
- `/legacy/**` không được dùng trong frontend và phải bị từ chối.

