# Ghi chú migration từ phiên bản backend cũ

Phiên bản mới thay đổi mô hình `Order`, `OrderItem`, `UserRole` và
`OrderStatus`. Phương án an toàn nhất trong giai đoạn làm đồ án là:

1. Export database cũ.
2. Tạo database mới.
3. Cho Hibernate tạo schema với `JPA_DDL_AUTO=update`.
4. Chỉ import lại `users`, `categories`, `products` sau khi chuyển enum.
5. Không import đơn hàng thử nghiệm cũ nếu không cần cho báo cáo.

## Chuyển enum trước khi import

Backend cũ có thể lưu:

- `users.role`: `0=ADMIN`, `1=USER`.
- `order_items.status`: `0=PENDING`, `1=CONFIRMED`, `2=DELIVERED`,
  `3=CANCELLED`, `4=RETURNED`.

Phiên bản mới lưu tên enum (`ADMIN`, `USER`, `PENDING`, ...). Nếu import thủ
công, hãy chuyển các giá trị số sang tên trước.

## Vì sao không cung cấp lệnh ALTER tự động

Schema thực tế có thể khác tùy thời điểm Hibernate đã chạy. Một lệnh `ALTER`
chung có thể làm mất role hoặc sai quan hệ địa chỉ. Hãy so sánh schema backup
và schema mới bằng MySQL Workbench trước khi chuyển dữ liệu.

Không chạy thao tác migration lần đầu trên database duy nhất.
