import { useEffect, useState } from "react";
import {
  ArrowRight,
  BadgeCheck,
  Headphones,
  ShieldCheck,
  Sparkles,
  Truck,
} from "lucide-react";
import { Link } from "react-router-dom";
import { catalogApi } from "../api/api";
import ProductCard from "../components/store/ProductCard";
import Loader from "../components/common/Loader";

const benefits = [
  { icon: BadgeCheck, title: "Hàng chính hãng", text: "Nguồn gốc minh bạch" },
  { icon: Truck, title: "Giao hàng nhanh", text: "Theo dõi đơn dễ dàng" },
  { icon: ShieldCheck, title: "Thanh toán an toàn", text: "COD hoặc VNPay" },
  { icon: Headphones, title: "Hỗ trợ tận tâm", text: "AI tư vấn 24/7" },
];

export default function HomePage() {
  const [products, setProducts] = useState([]);
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    Promise.all([
      catalogApi.search({ page: 0, size: 8, sortBy: "soldCount", direction: "desc" }),
      catalogApi.categories(),
    ])
      .then(([productResponse, categoryResponse]) => {
        setProducts(productResponse.data?.content ?? []);
        setCategories(categoryResponse.categoryList ?? []);
      })
      .finally(() => setLoading(false));
  }, []);

  return (
    <>
      <section className="hero">
        <div className="container hero__inner">
          <div className="hero__content">
            <span className="eyebrow"><Sparkles size={16} /> Chọn điều tốt nhất cho người bạn nhỏ</span>
            <h1>Chăm sóc thú cưng<br /><em>trọn vẹn hơn.</em></h1>
            <p>
              Thức ăn, phụ kiện và sản phẩm chăm sóc được tuyển chọn kỹ.
              Mua sắm thuận tiện, tư vấn thông minh và giao hàng tận nơi.
            </p>
            <div className="hero__actions">
              <Link className="button button--primary" to="/products">
                Khám phá sản phẩm <ArrowRight size={18} />
              </Link>
              <Link className="button button--ghost" to="/products?petType=CAT">
                Bộ sưu tập cho mèo
              </Link>
            </div>
            <div className="hero__proof">
              <div><strong>1.000+</strong><span>Sản phẩm</span></div>
              <div><strong>5.000+</strong><span>Khách hàng</span></div>
              <div><strong>4.9/5</strong><span>Mức hài lòng</span></div>
            </div>
          </div>
          <div className="hero__visual" aria-hidden="true">
            <div className="hero__blob" />
            <span className="hero__pet hero__pet--cat">🐈</span>
            <span className="hero__pet hero__pet--dog">🐕</span>
            <div className="hero__card">
              <span>Được yêu thích</span>
              <strong>Chăm sóc bằng cả trái tim</strong>
            </div>
          </div>
        </div>
      </section>

      <section className="benefits">
        <div className="container benefits__grid">
          {benefits.map(({ icon: Icon, title, text }) => (
            <div className="benefit" key={title}>
              <Icon />
              <div><strong>{title}</strong><span>{text}</span></div>
            </div>
          ))}
        </div>
      </section>

      <section className="section">
        <div className="container">
          <div className="section-heading">
            <div><span className="eyebrow">Mua theo nhu cầu</span><h2>Danh mục nổi bật</h2></div>
            <Link to="/products">Xem tất cả <ArrowRight size={17} /></Link>
          </div>
          <div className="category-grid">
            {categories.slice(0, 6).map((category, index) => (
              <Link
                key={category.id}
                to={`/products?categoryId=${category.id}`}
                className={`category-card category-card--${(index % 4) + 1}`}
              >
                <span>{["🐾", "🦴", "🧶", "🛁", "🥣", "🎾"][index % 6]}</span>
                <div><h3>{category.name}</h3><small>Khám phá ngay</small></div>
                <ArrowRight />
              </Link>
            ))}
          </div>
        </div>
      </section>

      <section className="section section--tint">
        <div className="container">
          <div className="section-heading">
            <div><span className="eyebrow">Khách hàng tin chọn</span><h2>Sản phẩm bán chạy</h2></div>
            <Link to="/products?sortBy=soldCount&direction=desc">Xem thêm <ArrowRight size={17} /></Link>
          </div>
          {loading ? <Loader /> : (
            <div className="product-grid">
              {products.map((product) => <ProductCard key={product.id} product={product} />)}
            </div>
          )}
        </div>
      </section>

      <section className="container promo-banner">
        <div>
          <span className="eyebrow">Trợ lý PetHub AI</span>
          <h2>Băn khoăn chọn sản phẩm?</h2>
          <p>Nhấn biểu tượng trò chuyện để nhận gợi ý dựa trên kho kiến thức của cửa hàng.</p>
        </div>
        <span className="promo-banner__icon">✨</span>
      </section>
    </>
  );
}
