const STATUS_LABELS = {
  PENDING_PAYMENT: "Chờ thanh toán",
  PENDING: "Chờ xác nhận",
  CONFIRMED: "Đã xác nhận",
  PROCESSING: "Đang xử lý",
  SHIPPING: "Đang giao",
  DELIVERED: "Đã giao",
  CANCELLED: "Đã hủy",
  RETURNED: "Đã hoàn",
  UNPAID: "Chưa thanh toán",
  PAID: "Đã thanh toán",
  FAILED: "Thanh toán lỗi",
  REFUNDED: "Đã hoàn tiền",
  REQUESTED: "Đã yêu cầu",
  APPROVED: "Đã duyệt",
  COMPLETED: "Hoàn tiền thành công",
  REJECTED: "Từ chối hoàn tiền",
};

export default function StatusBadge({ status }) {
  return (
    <span className={`status status--${String(status).toLowerCase()}`}>
      {STATUS_LABELS[status] ?? status}
    </span>
  );
}
