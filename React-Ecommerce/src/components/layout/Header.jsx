import { useEffect, useRef, useState } from "react";
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
import {
  Link,
  NavLink,
  useLocation,
  useNavigate,
} from "react-router-dom";
import toast from "react-hot-toast";
import { useAuth } from "../../context/AuthContext";
import { useCart } from "../../context/CartContext";
import { catalogApi } from "../../api/api";
import { formatCurrency } from "../../utils/format";

export default function Header() {
  const [menuOpen, setMenuOpen] = useState(false);
  const [query, setQuery] = useState("");
  const [suggestions, setSuggestions] = useState([]);
  const [suggestionsOpen, setSuggestionsOpen] = useState(false);
  const [suggestionsLoading, setSuggestionsLoading] = useState(false);
  const [activeSuggestion, setActiveSuggestion] = useState(-1);
  const searchRef = useRef(null);
  const { user, isAuthenticated, isAdmin, logout } = useAuth();
  const { cartCount } = useCart();
  const navigate = useNavigate();
  const location = useLocation();

  useEffect(() => {
    const urlQuery = location.pathname === "/products"
      ? new URLSearchParams(location.search).get("q") || ""
      : "";
    setQuery(urlQuery);
  }, [location.pathname, location.search]);

  const petType = new URLSearchParams(
    location.search,
  ).get("petType");

  const isProductsPage =
    location.pathname === "/products";

  const isProductDetail =
    location.pathname.startsWith("/products/");

  useEffect(() => {
    const keyword = query.trim();
    if (keyword.length < 2) {
      setSuggestions([]);
      setSuggestionsOpen(false);
      setSuggestionsLoading(false);
      return undefined;
    }

    let active = true;
    setSuggestionsOpen(true);
    setSuggestionsLoading(true);
    const timer = window.setTimeout(async () => {
      try {
        const response = await catalogApi.search({ q: keyword, page: 0, size: 6 });
        if (!active) return;
        setSuggestions(response.data?.content ?? []);
        setSuggestionsOpen(true);
        setActiveSuggestion(-1);
      } catch {
        if (active) setSuggestions([]);
      } finally {
        if (active) setSuggestionsLoading(false);
      }
    }, 350);

    return () => {
      active = false;
      window.clearTimeout(timer);
    };
  }, [query]);

  useEffect(() => {
    const closeSuggestions = (event) => {
      if (!searchRef.current?.contains(event.target)) setSuggestionsOpen(false);
    };
    document.addEventListener("mousedown", closeSuggestions);
    return () => document.removeEventListener("mousedown", closeSuggestions);
  }, []);

  const submitSearch = (event) => {
    event.preventDefault();
    navigate(`/products${query.trim() ? `?q=${encodeURIComponent(query.trim())}` : ""}`);
    setSuggestionsOpen(false);
    setMenuOpen(false);
  };

  const openProduct = (productId) => {
    setSuggestionsOpen(false);
    navigate(`/products/${productId}`);
  };

  const handleSearchKeyDown = (event) => {
    if (!suggestionsOpen || suggestions.length === 0) return;
    if (event.key === "ArrowDown") {
      event.preventDefault();
      setActiveSuggestion((current) => (current + 1) % suggestions.length);
    } else if (event.key === "ArrowUp") {
      event.preventDefault();
      setActiveSuggestion((current) =>
        current <= 0 ? suggestions.length - 1 : current - 1,
      );
    } else if (event.key === "Enter" && activeSuggestion >= 0) {
      event.preventDefault();
      openProduct(suggestions[activeSuggestion].id);
    } else if (event.key === "Escape") {
      setSuggestionsOpen(false);
    }
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

          <form className="header-search" onSubmit={submitSearch} ref={searchRef}>
            <Search size={19} />
            <input
              value={query}
              onChange={(event) => setQuery(event.target.value)}
              onFocus={() => query.trim().length >= 2 && setSuggestionsOpen(true)}
              onKeyDown={handleSearchKeyDown}
              placeholder="Tìm thức ăn, phụ kiện, thương hiệu..."
              aria-label="Tìm kiếm sản phẩm"
              aria-expanded={suggestionsOpen}
              aria-controls="header-search-suggestions"
              autoComplete="off"
            />
            <button type="submit">Tìm kiếm</button>
            {suggestionsOpen && (
              <div className="search-suggestions" id="header-search-suggestions">
                {suggestionsLoading ? (
                  <p className="search-suggestions__status">Đang tìm sản phẩm...</p>
                ) : suggestions.length ? (
                  <>
                    {suggestions.map((product, index) => (
                      <button
                        type="button"
                        className={`search-suggestion${index === activeSuggestion ? " is-active" : ""}`}
                        key={product.id}
                        onMouseEnter={() => setActiveSuggestion(index)}
                        onClick={() => openProduct(product.id)}
                      >
                        <img src={product.imageUrl || "/product-placeholder.svg"} alt="" />
                        <span><strong>{product.name}</strong><small>{formatCurrency(product.price)}</small></span>
                      </button>
                    ))}
                    <button type="submit" className="search-suggestions__all">
                      Xem tất cả kết quả cho “{query.trim()}”
                    </button>
                  </>
                ) : (
                  <p className="search-suggestions__status">Không tìm thấy sản phẩm phù hợp</p>
                )}
              </div>
            )}
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
            <Link
              to="/products"
              className={
                (isProductsPage && !petType)
                  || isProductDetail
                  ? "active"
                  : ""
              }
              onClick={() => setMenuOpen(false)}
            >
              Sản phẩm
            </Link>

            <Link
              to="/products?petType=DOG"
              className={
                isProductsPage && petType === "DOG"
                  ? "active"
                  : ""
              }
              onClick={() => setMenuOpen(false)}
            >
              Dành cho chó
            </Link>

            <Link
              to="/products?petType=CAT"
              className={
                isProductsPage && petType === "CAT"
                  ? "active"
                  : ""
              }
              onClick={() => setMenuOpen(false)}
            >
              Dành cho mèo
            </Link>
            {!isAdmin && isAuthenticated && (
              <NavLink to="/orders" onClick={() => setMenuOpen(false)}>Theo dõi đơn</NavLink>
            )}
          </div>
        </nav>
      </header>
    </>
  );
}
