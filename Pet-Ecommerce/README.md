# Pet E-commerce Backend

Backend cho đồ án **website thương mại điện tử thức ăn và phụ kiện thú cưng,
tích hợp chatbot tư vấn RAG**. Hệ thống theo mô hình một cửa hàng, hai vai trò
`ADMIN` và `USER`.

## Chức năng đã triển khai

| Nhóm | Chức năng chính |
|---|---|
| Xác thực | Đăng ký, đăng nhập JWT, access token 15 phút, refresh token rotation, đăng xuất một thiết bị/tất cả thiết bị |
| Tài khoản | Hồ sơ, địa chỉ mặc định, đổi mật khẩu, quên mật khẩu qua SMTP, khóa/mở tài khoản |
| Catalog | Danh mục, sản phẩm, SKU, thương hiệu, loại thú cưng, ảnh S3, soft delete, tồn kho, rating |
| Mua sắm | Giỏ hàng, yêu thích, đánh giá sau khi đơn đã giao, mã giảm giá |
| Đơn hàng | Checkout từ giỏ, server tự tính giá, snapshot sản phẩm/địa chỉ, khóa tồn kho, hủy và hoàn kho |
| Thanh toán | COD và VNPay Sandbox, HMAC-SHA512, Return URL, IPN idempotent, kiểm tra số tiền |
| Quản trị | Quản lý sản phẩm/danh mục/coupon/đơn/user, doanh thu, đơn hàng, sản phẩm bán chạy |
| Chatbot RAG | Nạp tài liệu, chunking, embedding, cosine retrieval, gợi ý sản phẩm, lịch sử hội thoại |
| Tài liệu | OpenAPI/Swagger UI và ví dụ API |

## Yêu cầu

- JDK 21
- MySQL 8
- PowerShell hoặc terminal
- Tài khoản AWS S3 nếu dùng upload ảnh
- Tài khoản VNPay Sandbox nếu kiểm thử VNPay
- API hỗ trợ `/v1/embeddings` và `/v1/chat/completions` nếu dùng chatbot

## 1. Chuẩn bị database

Vì entity cũ lưu enum theo số còn phiên bản này lưu enum theo chuỗi, khi đang
phát triển nên tạo database mới để tránh dữ liệu cũ sai kiểu:

```sql
CREATE DATABASE pet_ecommerce
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

Nếu cần giữ dữ liệu cũ, hãy backup trước và đọc
[`database/MIGRATION_V2.md`](database/MIGRATION_V2.md). Không chạy migration
thử trực tiếp trên database duy nhất.

## 2. Cấu hình

Sao chép `.env.example` thành `.env`:

```powershell
Copy-Item .env.example .env
```

Cấu hình tối thiểu:

```properties
DB_URL=jdbc:mysql://localhost:3306/pet_ecommerce?useSSL=false&serverTimezone=Asia/Ho_Chi_Minh&allowPublicKeyRetrieval=true
DB_USERNAME=root
DB_PASSWORD=your_password
JWT_SECRET=replace_with_at_least_32_random_bytes
AWS_S3_ACCESS=your_access_key
AWS_S3_SECRET=your_secret_key
AWS_S3_BUCKET=dtvi-ecommerce
AWS_S3_REGION=ap-southeast-2
```

Không commit `.env`. File này đã được chặn trong `.gitignore`.

## 3. Chạy kiểm thử và ứng dụng

PowerShell không tự chạy file trong thư mục hiện tại, vì vậy cần tiền tố `.\`:

```powershell
.\mvnw.cmd clean test
.\mvnw.cmd spring-boot:run
```

Sau khi chạy:

- API: `http://localhost:2424`
- Swagger UI: `http://localhost:2424/swagger-ui.html`
- OpenAPI JSON: `http://localhost:2424/v3/api-docs`

Đăng ký một user trước, sau đó cấp quyền admin:

```sql
UPDATE users SET role = 'ADMIN' WHERE email = 'admin@example.com';
```

Đăng xuất và đăng nhập lại để JWT mới có quyền `ADMIN`.

## 4. Luồng mua hàng chuẩn

1. `POST /auth/login`.
2. Gửi access token trong `Authorization: Bearer <token>`.
3. `POST /cart/items`.
4. `POST /orders/checkout`.
5. Nếu `paymentMethod=COD`, admin xử lý đơn đến `DELIVERED`.
6. Nếu `paymentMethod=VNPAY`, gọi `POST /payments/vnpay/create/{orderId}` và
   chuyển trình duyệt đến `paymentUrl`.
7. IPN là kênh xác nhận chính khi triển khai thật. Ở môi trường demo localhost,
   Return URL có chữ ký và số tiền hợp lệ cũng hoàn tất giao dịch theo cách
   idempotent, vì VNPay không thể gọi IPN vào localhost.

Checkout không nhận giá/tổng tiền từ client. Server đọc lại giá sản phẩm, kiểm
tra coupon và khóa bản ghi sản phẩm trước khi trừ kho.

### Trạng thái đơn

```text
VNPay: PENDING_PAYMENT -> PENDING -> CONFIRMED -> PROCESSING -> SHIPPING -> DELIVERED
COD:                     PENDING -> CONFIRMED -> PROCESSING -> SHIPPING -> DELIVERED
```

Đơn đang chờ có thể chuyển `CANCELLED`; đơn đã giao có thể chuyển `RETURNED`.
Mọi chuyển trạng thái ngoài quy tắc đều trả `409 INVALID_ORDER_TRANSITION`.

## 5. VNPay Sandbox

1. Đăng ký miễn phí tại `https://sandbox.vnpayment.vn/devreg/`.
2. Lấy `vnp_TmnCode` và `vnp_HashSecret` do VNPay Sandbox cấp.
3. Điền vào `.env`:

```properties
VNPAY_TMN_CODE=
VNPAY_HASH_SECRET=
VNPAY_PAY_URL=https://sandbox.vnpayment.vn/paymentv2/vpcpay.html
VNPAY_RETURN_URL=http://localhost:5173/payment/vnpay/return
```

4. Khởi động lại backend, chọn VNPay khi checkout và dùng thông tin thẻ test
   trên trang tài liệu chính thức của VNPay Sandbox.

Khi triển khai public, đăng ký IPN URL:

```text
https://your-public-domain/payments/vnpay/ipn
```

IPN phải là HTTPS công khai; `localhost` không nhận được callback từ VNPay.
Return URL đã ký đủ cho demo local; khi bảo vệ luận văn với luồng IPN đầy đủ có
thể dùng tunnel HTTPS miễn phí. Không đặt `VNPAY_HASH_SECRET` trong frontend.

## 6. Chatbot RAG

### Cách demo miễn phí bằng Ollama

1. Cài Ollama cho Windows từ `https://ollama.com/download/windows`.
2. Mở PowerShell và tải hai model:

```powershell
ollama pull qwen2.5:3b-instruct
ollama pull nomic-embed-text
```

3. Cấu hình `.env`:

```properties
AI_BASE_URL=http://localhost:11434/v1
AI_API_KEY=ollama
AI_CHAT_MODEL=qwen2.5:3b-instruct
AI_EMBEDDING_MODEL=nomic-embed-text
AI_TOP_K=5
AI_MINIMUM_SCORE=0.2
```

`AI_API_KEY=ollama` là giá trị giả bắt buộc theo định dạng OpenAI-compatible;
Ollama chạy local và bỏ qua key này. Nếu máy chỉ có 8 GB RAM, có thể thay model
chat bằng `qwen2.5:1.5b`. Sau khi đổi cấu hình, khởi động lại backend.

### Nạp tri thức và kiểm thử

Đăng nhập ADMIN, mở **Kho tri thức AI**, tạo ít nhất một tài liệu. Lần tạo tài
liệu sẽ gọi embedding local và lưu vector trong MySQL. Sau đó mở nút
**Hỏi PetHub AI** để đặt câu hỏi.

Nếu dùng API trả phí thay cho Ollama, cấu hình:

```properties
AI_BASE_URL=https://api.openai.com/v1
AI_API_KEY=
AI_CHAT_MODEL=gpt-4.1-mini
AI_EMBEDDING_MODEL=text-embedding-3-small
```

API tương thích khác có thể dùng bằng cách đổi bốn biến trên. Provider cần hỗ
trợ cấu trúc OpenAI-compatible cho embeddings và chat completions.

Admin nạp tài liệu:

```http
POST /rag/admin/documents
Authorization: Bearer <admin-access-token>
Content-Type: application/json

{
  "title": "Hướng dẫn chọn thức ăn cho chó",
  "sourceUrl": "https://example.com/source",
  "content": "Nội dung kiến thức đã được kiểm duyệt..."
}
```

Khách hỏi:

```http
POST /rag/chat
Content-Type: application/json

{
  "conversationId": null,
  "question": "Chó con 3 tháng nên chọn loại thức ăn nào?"
}
```

Embedding hiện được lưu dạng JSON trong MySQL và tìm cosine trong Java. Cách
này phù hợp dữ liệu đồ án nhỏ. Khi số chunk tăng lớn, có thể chuyển sang
PostgreSQL/pgvector hoặc vector database mà không đổi API chatbot.

## 7. Kiểm tra và sửa dữ liệu giá bất thường

Giá nhập trong database phải là số VND không có dấu phân cách hàng nghìn. Ví dụ
`100000` nghĩa là 100.000đ. Kiểm tra sản phẩm có giá quá lớn:

```sql
USE ecommerce;
SELECT id, name, sku, price
FROM products
ORDER BY price DESC;
```

Sửa đúng `id` sản phẩm, ví dụ:

```sql
UPDATE products
SET price = 100000
WHERE id = 2;
```

Backend giới hạn giá mỗi sản phẩm ở 1 tỷ đồng và tổng đơn ở 10 tỷ đồng để dữ
liệu nhập sai trả lỗi nghiệp vụ rõ ràng, không còn rơi xuống lỗi database 500.

## 8. Email đặt lại mật khẩu

Khi chưa cấu hình SMTP, giữ `MAIL_ENABLED=false`. Để bật:

```properties
MAIL_ENABLED=true
MAIL_FROM=no-reply@example.com
MAIL_HOST=smtp.example.com
MAIL_PORT=587
MAIL_USERNAME=
MAIL_PASSWORD=
MAIL_SMTP_AUTH=true
MAIL_STARTTLS=true
PASSWORD_RESET_URL=http://localhost:5173/reset-password
```

Token reset chỉ lưu hash SHA-256 trong database, dùng một lần và mặc định hết
hạn sau 15 phút.

## 9. Production checklist

- Đặt `JWT_REFRESH_COOKIE_SECURE=true` và chỉ chạy HTTPS.
- Giới hạn `CORS_ALLOWED_ORIGINS` đúng domain frontend.
- Dùng secret manager cho JWT, AWS, VNPay, SMTP và AI key.
- Đặt `JPA_DDL_AUTO=validate` sau khi đã quản lý migration chính thức.
- Không log JWT, refresh token, VNPay secret hoặc AI API key.
- Dùng database user riêng, backup định kỳ và bật monitoring.
- Đăng ký đúng Return URL/IPN URL production với VNPay.

Danh sách endpoint và request mẫu chi tiết nằm trong
[`API_TEST_GUIDE.md`](API_TEST_GUIDE.md).

## 10. Chạy bằng Docker

Tạo `.env` trước, sau đó:

```powershell
docker compose up --build
```

Compose khởi tạo MySQL 8.4, chờ database healthy rồi mới chạy backend. Dữ liệu
MySQL nằm trong volume `pet_mysql_data`.
