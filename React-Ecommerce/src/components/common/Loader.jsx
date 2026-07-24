export default function Loader({ fullPage = false, label = "Đang tải dữ liệu..." }) {
  return (
    <div className={fullPage ? "loader loader--page" : "loader"} role="status">
      <span className="loader__spinner" />
      <span>{label}</span>
    </div>
  );
}
