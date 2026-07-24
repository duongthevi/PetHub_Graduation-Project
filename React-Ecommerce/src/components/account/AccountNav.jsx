import { Heart, MapPin, Package, ShoppingBag, UserRound } from "lucide-react";
import { NavLink } from "react-router-dom";

const links = [
  { to: "/profile", label: "Thông tin tài khoản", icon: UserRound },
  { to: "/orders", label: "Đơn hàng của tôi", icon: Package },
  { to: "/wishlist", label: "Sản phẩm yêu thích", icon: Heart },
  { to: "/cart", label: "Giỏ hàng", icon: ShoppingBag },
];

export default function AccountNav() {
  return <aside className="account-nav"><h2>Tài khoản</h2>{links.map(({ to, label, icon: Icon }) => <NavLink key={to} to={to}><Icon /> {label}</NavLink>)}<span><MapPin /> Địa chỉ được quản lý trong hồ sơ</span></aside>;
}
