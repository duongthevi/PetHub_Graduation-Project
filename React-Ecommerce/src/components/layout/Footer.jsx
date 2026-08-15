import { Camera, Mail, MapPin, MessagesSquare, PawPrint, Phone } from "lucide-react";
import { Link } from "react-router-dom";

export default function Footer() {
  return (
    <footer className="site-footer">
      <div className="container footer-grid">
        <div>
          <Link to="/" className="brand brand--light">
            <span className="brand__mark"><PawPrint /></span>
            <span><strong>Pet</strong>Hub<small>Yêu thương mỗi ngày</small></span>
          </Link>
          <p className="footer-copy">
            Sản phẩm chính hãng và kiến thức đáng tin cậy cho hành trình chăm sóc
            thú cưng khỏe mạnh, hạnh phúc.
          </p>
          <div className="socials">
            <a href="https://facebook.com" aria-label="Facebook"><MessagesSquare size={18} /></a>
            <a href="https://instagram.com" aria-label="Instagram"><Camera size={18} /></a>
          </div>
        </div>
        <div>
          <h3>Khám phá</h3>
          <Link to="/products">Tất cả sản phẩm</Link>
          <Link to="/products?petType=DOG">Sản phẩm cho chó</Link>
          <Link to="/products?petType=CAT">Sản phẩm cho mèo</Link>
          <Link to="/orders">Kiểm tra đơn hàng</Link>
        </div>
        <div>
          <h3>Chính sách</h3>
          <span>Hướng dẫn mua hàng</span>
          <span>Chính sách đổi trả</span>
          <span>Bảo mật thông tin</span>
          <span>Điều khoản sử dụng</span>
        </div>
        <div>
          <h3>Liên hệ</h3>
          <span><MapPin size={17} /> TP. Cần Thơ, Việt Nam</span>
          <a href="tel:0967 101 853"><Phone size={17} /> 0967 101 853</a>
          <a href="mailto:pethub.support@gmail.com"><Mail size={17} /> pethub.support@gmail.com</a>
        </div>
      </div>
      <div className="footer-bottom">
        <div className="container">
          <span>© {new Date().getFullYear()} PetHub</span>
          <span>Thanh toán an toàn với COD & VNPay</span>
        </div>
      </div>
    </footer>
  );
}
