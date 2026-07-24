import { PawPrint, ShieldCheck, Sparkles } from "lucide-react";
import { Link } from "react-router-dom";

export default function AuthShell({ title, subtitle, children, footer }) {
  return (
    <div className="auth-page container">
      <section className="auth-aside">
        <Link to="/" className="brand brand--light">
          <span className="brand__mark"><PawPrint /></span>
          <span><strong>Pet</strong>Hub<small>Yêu thương mỗi ngày</small></span>
        </Link>
        <div>
          <span className="eyebrow eyebrow--light"><Sparkles /> Thành viên PetHub</span>
          <h2>Chăm sóc tốt hơn,<br />mua sắm nhẹ nhàng hơn.</h2>
          <p>Lưu sản phẩm yêu thích, theo dõi đơn hàng và nhận tư vấn phù hợp cho thú cưng.</p>
        </div>
        <span className="auth-aside__secure"><ShieldCheck /> Phiên đăng nhập được bảo vệ an toàn</span>
      </section>
      <section className="auth-card">
        <div><span className="eyebrow">Chào mừng bạn</span><h1>{title}</h1><p>{subtitle}</p></div>
        {children}
        {footer && <div className="auth-card__footer">{footer}</div>}
      </section>
    </div>
  );
}
