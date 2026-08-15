import { AlertTriangle, Boxes, CircleDollarSign, ClipboardList, Clock3, Users } from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { adminApi } from "../../api/api";
import AdminPageHeader from "../../components/admin/AdminPageHeader";
import Loader from "../../components/common/Loader";
import { formatCurrency } from "../../utils/format";

export default function DashboardPage() {
  const now = new Date();
  const [dashboard, setDashboard] = useState(null);
  const [monthly, setMonthly] = useState(null);
  const [period, setPeriod] = useState({ month: now.getMonth() + 1, year: now.getFullYear() });

  useEffect(() => {
    adminApi.dashboard().then((response) => setDashboard(response.data));
  }, []);
  useEffect(() => {
    adminApi.monthlyStatistics(period.month, period.year).then(setMonthly);
  }, [period]);

  const metrics = useMemo(() => dashboard ? [
    { label: "Doanh thu đã trả", value: formatCurrency(dashboard.paidRevenue), icon: CircleDollarSign, tone: "green" },
    { label: "Tổng đơn hàng", value: dashboard.totalOrders, icon: ClipboardList, tone: "blue" },
    { label: "Đơn chờ xử lý", value: dashboard.pendingOrders, icon: Clock3, tone: "orange" },
    { label: "Sản phẩm hoạt động", value: dashboard.activeProducts, icon: Boxes, tone: "purple" },
    { label: "Sắp hết hàng", value: dashboard.lowStockProducts, icon: AlertTriangle, tone: "red" },
    { label: "Người dùng", value: dashboard.users, icon: Users, tone: "teal" },
  ] : [], [dashboard]);

  if (!dashboard) return <Loader fullPage />;
  const best = monthly?.bestSellingProducts ?? [];
  const maxSold = Math.max(...best.map((item) => item.totalQuantity || item.totalSold || item.quantitySold || 0), 1);

  return (
    <>
      <AdminPageHeader title="Tổng quan kinh doanh" description="Theo dõi những chỉ số quan trọng của cửa hàng trong một màn hình." />
      <div className="metric-grid">{metrics.map(({ label, value, icon: Icon, tone }) => {
        const content = <><span><Icon /></span><div><small>{label}</small><strong>{value}</strong></div></>;
        return tone === "red"
          ? <Link className={`metric-card metric-card--${tone} metric-card--clickable`} to="/admin/products?lowStock=true" key={label}>{content}</Link>
          : <article className={`metric-card metric-card--${tone}`} key={label}>{content}</article>;
      })}</div>
      <div className="dashboard-grid">
        <section className="admin-card">
          <header className="admin-card__header"><div><h2>Hiệu quả theo tháng</h2><p>Doanh thu và số đơn hợp lệ</p></div><div className="period-select"><select value={period.month} onChange={(e) => setPeriod({ ...period, month: Number(e.target.value) })}>{Array.from({ length: 12 }, (_, index) => <option key={index + 1} value={index + 1}>Tháng {index + 1}</option>)}</select><input type="number" min="2020" max="2100" value={period.year} onChange={(e) => setPeriod({ ...period, year: Number(e.target.value) })} /></div></header>
          <div className="month-summary"><div><small>Doanh thu</small><strong>{formatCurrency(monthly?.totalRevenue)}</strong></div><div><small>Đơn hàng</small><strong>{monthly?.totalOrders || 0}</strong></div></div>
        </section>
        <section className="admin-card">
          <header className="admin-card__header"><div><h2>Sản phẩm bán chạy</h2><p>Xếp hạng trong kỳ đã chọn</p></div></header>
          <div className="rank-list">{best.length ? best.slice(0, 5).map((item, index) => {
            const sold = item.totalQuantity || item.totalSold || item.quantitySold || 0;
            return <div key={item.productId || index}><strong>{index + 1}</strong><span><b>{item.productName || item.name}</b><i><em style={{ width: `${(sold / maxSold) * 100}%` }} /></i></span><small>{sold} đã bán</small></div>;
          }) : <p className="muted">Chưa có dữ liệu bán hàng trong kỳ này.</p>}</div>
        </section>
      </div>
    </>
  );
}
