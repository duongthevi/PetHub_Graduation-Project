import { useState } from "react";
import {
  Heart,
  LayoutDashboard,
  LogOut,
  Menu,
  Package,
  PawPrint,
  Search,
  ShoppingBag,
  UserRound,
  X,
} from "lucide-react";
import { Link, NavLink, useNavigate } from "react-router-dom";
import toast from "react-hot-toast";
import { useAuth } from "../../context/AuthContext";
import { useCart } from "../../context/CartContext";

export default function Header() {
  const [menuOpen, setMenuOpen] = useState(false);
  const [query, setQuery] = useState("");
  const { user, isAuthenticated, isAdmin, logout } = useAuth();
  const { cartCount } = useCart();
  const navigate = useNavigate();

  const submitSearch = (event) => {
    event.preventDefault();
    navigate(`/products${query.trim() ? `?q=${encodeURIComponent(query.trim())}` : ""}`);
    setMenuOpen(false);
  };

  const handleLogout = async () => {
    await logout();
    toast.success("Bạn đã đăng xuất");
    navigate("/");
  };

  return (
    <>
      <div className="announcement">
        Miễn phí giao hàng cho đơn từ 500.000đ · Tư vấn chăm sóc thú cưng miễn phí
      </div>
      <header className="site-header">
        <div className="container header__inner">
          <Link to="/" className="brand" aria-label="Pet Hub - Trang chủ">
            <span className="brand__mark"><PawPrint /></span>
            <span><strong>Pet</strong>Hub<small>Yêu thương mỗi ngày</small></span>
          </Link>

          <form className="header-search" onSubmit={submitSearch}>
            <Search size={19} />
            <input
              value={query}
              onChange={(event) => setQuery(event.target.value)}
              placeholder="Tìm thức ăn, phụ kiện, thương hiệu..."
              aria-label="Tìm kiếm sản phẩm"
            />
            <button type="submit">Tìm kiếm</button>
          </form>

          <div className="header-actions">
            {isAuthenticated && !isAdmin && (
              <>
                <Link to="/wishlist" className="header-action" title="Yêu thích">
                  <Heart />
                </Link>
                <Link to="/cart" className="header-action header-action--cart" title="Giỏ hàng">
                  <ShoppingBag />
                  {cartCount > 0 && <span>{cartCount > 99 ? "99+" : cartCount}</span>}
                </Link>
              </>
            )}
            {isAuthenticated ? (
              <div className="account-menu">
                <button className="header-account">
                  <span className="avatar">{user?.name?.charAt(0)?.toUpperCase() || "U"}</span>
                  <span><small>Xin chào</small>{user?.name}</span>
                </button>
                <div className="account-dropdown">
                  {isAdmin ? (
                    <Link to="/admin/dashboard"><LayoutDashboard size={17} /> Quản trị</Link>
                  ) : (
                    <>
                      <Link to="/profile"><UserRound size={17} /> Tài khoản</Link>
                      <Link to="/orders"><Package size={17} /> Đơn hàng</Link>
                    </>
                  )}
                  <button onClick={handleLogout}><LogOut size={17} /> Đăng xuất</button>
                </div>
              </div>
            ) : (
              <Link to="/login" className="button button--primary button--small">
                Đăng nhập
              </Link>
            )}
            <button
              className="mobile-menu-button"
              onClick={() => setMenuOpen((value) => !value)}
              aria-label="Mở trình đơn"
            >
              {menuOpen ? <X /> : <Menu />}
            </button>
          </div>
        </div>

        <nav className={`main-nav ${menuOpen ? "is-open" : ""}`}>
          <div className="container main-nav__inner">
            <NavLink to="/" end onClick={() => setMenuOpen(false)}>Trang chủ</NavLink>
            <NavLink to="/products" onClick={() => setMenuOpen(false)}>Sản phẩm</NavLink>
            <NavLink to="/products?petType=DOG" onClick={() => setMenuOpen(false)}>Dành cho chó</NavLink>
            <NavLink to="/products?petType=CAT" onClick={() => setMenuOpen(false)}>Dành cho mèo</NavLink>
            {!isAdmin && isAuthenticated && (
              <NavLink to="/orders" onClick={() => setMenuOpen(false)}>Theo dõi đơn</NavLink>
            )}
          </div>
        </nav>
      </header>
    </>
  );
}
