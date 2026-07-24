# PetHub Frontend v2.0.2

Frontend thương mại điện tử cho cửa hàng thú cưng PetHub, được xây dựng bằng
React 19, Vite 6 và REST API của Pet-Ecommerce Backend v2.

## Chức năng

### Khách hàng

- Đăng ký, đăng nhập, tự làm mới access token bằng refresh-token cookie.
- Quên mật khẩu, đặt lại mật khẩu, đổi mật khẩu và đăng xuất mọi thiết bị.
- Tìm kiếm, lọc, sắp xếp và phân trang sản phẩm.
- Xem chi tiết, tồn kho, đánh giá và sản phẩm liên quan.
- Wishlist và giỏ hàng đồng bộ với tài khoản trên server.
- Áp dụng coupon và checkout bằng COD hoặc VNPay.
- Theo dõi trạng thái, xem chi tiết và hủy đơn hợp lệ.
- Cập nhật hồ sơ và địa chỉ nhận hàng mặc định.
- Đánh giá sản phẩm sau khi đơn đã giao.
- Chatbot RAG tư vấn sản phẩm, hiển thị nguồn và sản phẩm đề xuất.

### Quản trị

- Dashboard doanh thu, đơn hàng, người dùng, tồn kho thấp và sản phẩm bán chạy.
- CRUD danh mục, sản phẩm và ảnh sản phẩm.
- Danh sách đơn, chi tiết và cập nhật vòng đời đơn hàng.
- CRUD coupon theo phần trăm hoặc số tiền cố định.
- Khóa/mở tài khoản khách hàng.
- Quản lý tài liệu trong kho tri thức RAG.

## Yêu cầu

- Node.js 18 trở lên.
- Backend chạy tại `http://localhost:2424` hoặc URL được cấu hình.

## Chạy local

```bash
npm install
cp .env.example .env
npm run dev
```

Trên Windows PowerShell:

```powershell
Copy-Item .env.example .env
npm run dev
```

Mở `http://localhost:5173`.

## Biến môi trường

```env
VITE_API_URL=http://localhost:2424
VITE_SHIPPING_FEE=30000
VITE_FREE_SHIPPING_THRESHOLD=500000
```

Không đưa file `.env` có thông tin production vào Git.

## Cấu hình backend cần khớp

Vì refresh token được lưu trong cookie HttpOnly, backend phải cho phép credential
từ origin của frontend:

```env
CORS_ALLOWED_ORIGINS=http://localhost:5173
```

Để VNPay trả kết quả về giao diện React:

```env
VNPAY_RETURN_URL=http://localhost:5173/payment/vnpay/return
```

Cookie refresh token nên dùng `Secure=true` trên production có HTTPS. Khi phát
triển bằng HTTP local, cấu hình `Secure=false`.

## Kiểm tra chất lượng

```bash
npm run lint
npm test
npm run build
```

Production bundle nằm trong thư mục `dist`. Máy chủ static phải fallback mọi
route chưa khớp về `index.html` để React Router hoạt động.

## Cấu trúc chính

```text
src/
├── api/          Axios client, interceptor và API modules
├── components/   Thành phần dùng lại theo common/layout/store/account/admin
├── context/      AuthContext và CartContext
├── pages/        Storefront, tài khoản, xác thực và quản trị
├── styles/       Design system responsive
└── utils/        Session và định dạng dữ liệu
```

## Lưu ý tích hợp

- FE mới dùng endpoint `/orders/checkout`; endpoint `/order/create` của bản cũ
  không còn được gọi.
- Giỏ hàng không lưu ở localStorage. Người dùng cần đăng nhập để thêm sản phẩm.
- Access token nằm ở localStorage để phục vụ Bearer header; refresh token chỉ do
  backend quản lý bằng HttpOnly cookie.
- Với endpoint trả `ApiResponse<T>`, dữ liệu nghiệp vụ nằm trong thuộc tính
  `data`. Các endpoint legacy như category vẫn dùng `categoryList`.
