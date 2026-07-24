export function formatCurrency(value) {
  return new Intl.NumberFormat("vi-VN", {
    style: "currency",
    currency: "VND",
    maximumFractionDigits: 0,
  }).format(Number(value ?? 0));
}

export function formatDateTime(value) {
  if (!value) return "—";
  return new Intl.DateTimeFormat("vi-VN", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value));
}

export function getErrorMessage(error, fallback = "Đã có lỗi xảy ra") {
  return (
    error?.response?.data?.message ||
    error?.message ||
    fallback
  );
}

export function compactText(value, maxLength = 110) {
  if (!value) return "Chưa có mô tả.";
  return value.length > maxLength
    ? `${value.slice(0, maxLength).trim()}…`
    : value;
}
