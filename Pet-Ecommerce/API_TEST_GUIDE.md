# API test guide

Base URL: `http://localhost:2424`

Các API cần đăng nhập dùng header:

```http
Authorization: Bearer <accessToken>
```

Refresh token nằm trong cookie HttpOnly và được Postman/browser lưu tự động.

## Auth

### Đăng ký

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

### Đăng nhập

```http
POST /auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "Password123!"
}
```

### Refresh/logout

```http
POST /auth/refresh
POST /auth/logout
POST /auth/logout-all
```

### Quên/đặt lại mật khẩu

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

## Sản phẩm và danh mục

Public:

```http
GET /category/get-all
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
image=<file>
name=Thức ăn chó con
description=Mô tả
price=150000
quantity=50
sku=DOG-FOOD-001
brand=Pet Brand
petType=DOG
```

## Cart

```http
GET /cart
```

```http
POST /cart/items
Content-Type: application/json

{"productId":1,"quantity":2}
```

```http
PATCH /cart/items/1?quantity=3
DELETE /cart/items/1
DELETE /cart
```

## Wishlist

```http
GET /wishlist
POST /wishlist/1
DELETE /wishlist/1
```

## Coupon

Admin:

```http
POST /coupons
Content-Type: application/json

{
  "code": "WELCOME10",
  "description": "Giảm 10%",
  "type": "PERCENT",
  "value": 10,
  "minimumOrderValue": 200000,
  "maximumDiscount": 50000,
  "startsAt": "2026-01-01T00:00:00",
  "expiresAt": "2027-01-01T00:00:00",
  "usageLimit": 1000,
  "perUserLimit": 1,
  "active": true
}
```

User kiểm tra:

```http
GET /coupons/validate?code=WELCOME10&subtotal=300000
```

## Checkout và đơn hàng

```http
POST /orders/checkout
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

Đổi `paymentMethod` thành `VNPAY` để tạo đơn online, sau đó:

```http
POST /payments/vnpay/create/{orderId}
```

Order:

```http
GET /orders/my?page=0&size=10
GET /orders/{orderId}
POST /orders/{orderId}/cancel
```

Admin:

```http
GET /orders/admin/all?page=0&size=20
PATCH /orders/admin/{orderId}/status
Content-Type: application/json

{"status":"CONFIRMED","adminMessage":"Đã xác nhận"}
```

Các bước tiếp theo: `PROCESSING`, `SHIPPING`, `DELIVERED`.

## Review

Chỉ user có order item `DELIVERED` mới được tạo:

```http
POST /reviews/product/1
Content-Type: application/json

{"rating":5,"comment":"Sản phẩm tốt"}
```

```http
GET /reviews/product/1?page=0&size=10
PUT /reviews/{reviewId}
DELETE /reviews/{reviewId}
```

## Profile

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

## Statistics

Admin:

```http
GET /statistics/dashboard
GET /statistics/monthly?month=7&year=2026
```

## RAG

Admin nạp tài liệu:

```http
POST /rag/admin/documents
Content-Type: application/json

{
  "title":"Kiến thức dinh dưỡng",
  "sourceUrl":"https://example.com",
  "content":"Nội dung tài liệu..."
}
```

Chat public:

```http
POST /rag/chat
Content-Type: application/json

{"conversationId":null,"question":"Tư vấn thức ăn cho mèo con"}
```
