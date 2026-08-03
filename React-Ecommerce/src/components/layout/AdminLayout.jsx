import { useState } from "react";
import {
  BookOpenText,
  Boxes,
  ChevronLeft,
  ClipboardList,
  History,
  LayoutDashboard,
  LogOut,
  Menu,
  PawPrint,
  Percent,
  RotateCcw,
  ScrollText,
  Shapes,
  Store,
  Users,
} from "lucide-react";
import { Link, NavLink, Outlet, useNavigate } from "react-router-dom";
import toast from "react-hot-toast";
import { useAuth } from "../../context/AuthContext";
import { REFUND_ENABLED } from "../../config/features";

const links = [
  { to: "/admin/dashboard", label: "Tổng quan", icon: LayoutDashboard },
  { to: "/admin/products", label: "Sản phẩm", icon: Boxes },
  { to: "/admin/categories", label: "Danh mục", icon: Shapes },
  { to: "/admin/orders", label: "Đơn hàng", icon: ClipboardList },
  ...(REFUND_ENABLED
  ? [{
      to: "/admin/refunds",
      label: "Hoàn tiền",
      icon: RotateCcw,
    }]
  : []),
  { to: "/admin/inventory", label: "Lịch sử tồn kho", icon: History },
  { to: "/admin/audit-logs", label: "Nhật ký quản trị", icon: ScrollText },
  { to: "/admin/coupons", label: "Mã giảm giá", icon: Percent },
  { to: "/admin/users", label: "Khách hàng", icon: Users },
  { to: "/admin/knowledge", label: "Kho tri thức AI", icon: BookOpenText },
];

export default function AdminLayout() {
  const [collapsed, setCollapsed] = useState(false);
  const [mobileOpen, setMobileOpen] = useState(false);
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = async () => {
    await logout();
    toast.success("Đã đăng xuất khỏi trang quản trị");
    navigate("/login");
  };

  return (
    <div className={`admin-shell ${collapsed ? "is-collapsed" : ""}`}>
      <aside className={`admin-sidebar ${mobileOpen ? "is-open" : ""}`}>
        <Link to="/admin/dashboard" className="brand brand--light admin-brand">
          <span className="brand__mark"><PawPrint /></span>
          <span><strong>Pet</strong>Hub<small>ADMIN CONSOLE</small></span>
        </Link>
        <nav className="admin-nav">
          {links.map(({ to, label, icon: Icon }) => (
            <NavLink key={to} to={to} onClick={() => setMobileOpen(false)}>
              <Icon size={20} /><span>{label}</span>
            </NavLink>
          ))}
        </nav>
        <div className="admin-sidebar__footer">
          <Link to="/"><Store size={19} /><span>Xem cửa hàng</span></Link>
          <button onClick={handleLogout}><LogOut size={19} /><span>Đăng xuất</span></button>
        </div>
      </aside>
      <div className="admin-main">
        <header className="admin-topbar">
          <div>
            <button className="admin-mobile-toggle" onClick={() => setMobileOpen((v) => !v)}>
              <Menu />
            </button>
            <button className="admin-collapse" onClick={() => setCollapsed((v) => !v)}>
              <ChevronLeft />
            </button>
          </div>
          <div className="admin-user">
            <span className="avatar">{user?.name?.charAt(0)?.toUpperCase() || "A"}</span>
            <span><strong>{user?.name}</strong><small>Quản trị viên</small></span>
          </div>
        </header>
        <main className="admin-content"><Outlet /></main>
      </div>
    </div>
  );
}
