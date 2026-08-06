import { useEffect, useState } from "react";
import { Check, ChevronRight, Heart, Minus, Plus, ShieldCheck, ShoppingBag, Star, Truck } from "lucide-react";
import { Link, useLocation, useNavigate, useParams } from "react-router-dom";
import toast from "react-hot-toast";
import { catalogApi, reviewApi, wishlistApi } from "../api/api";
import Loader from "../components/common/Loader";
import EmptyState from "../components/common/EmptyState";
import Pagination from "../components/common/Pagination";
import ProductCard from "../components/store/ProductCard";
import { useAuth } from "../context/AuthContext";
import { useCart } from "../context/CartContext";
import { formatCurrency, formatDateTime, getErrorMessage } from "../utils/format";

export default function ProductDetailPage() {
  const { productId } = useParams();
  const [product, setProduct] = useState(null);
  const [related, setRelated] = useState([]);
  const [reviews, setReviews] = useState({ content: [], number: 0, totalPages: 0, totalElements: 0 });
  const [loading, setLoading] = useState(true);
  const [quantity, setQuantity] = useState(1);
  const [reviewForm, setReviewForm] = useState({ rating: 5, comment: "" });
  const [submitting, setSubmitting] = useState(false);
  const { isAuthenticated, isAdmin, user } = useAuth();
  const { addItem } = useCart();
  const navigate = useNavigate();
  const location = useLocation();

  const loadProduct = () =>
  catalogApi
    .getById(productId)
    .then((response) => {
      setProduct(response.product);
    });

  const loadReviews = (page = 0) =>
    reviewApi.list(productId, { page, size: 5 })
      .then((response) => setReviews(response.data));

  useEffect(() => {
    setLoading(true);
    Promise.all([catalogApi.getById(productId), reviewApi.list(productId, { page: 0, size: 5 })])
      .then(async ([productResponse, reviewResponse]) => {
        const current = productResponse.product;
        setProduct(current);
        setReviews(reviewResponse.data);
        if (current?.category?.id) {
          const relatedResponse = await catalogApi.search({ categoryId: current.category.id, page: 0, size: 5 });
          setRelated((relatedResponse.data?.content ?? []).filter((item) => item.id !== current.id).slice(0, 4));
        }
      })
      .catch(() => setProduct(null))
      .finally(() => setLoading(false));
  }, [productId]);

  const requireUser = () => {
    if (!isAuthenticated) {
      navigate("/login", { state: { from: location } });
      return false;
    }
    if (isAdmin) {
      toast.error("Tài khoản quản trị không thể mua hàng");
      return false;
    }
    return true;
  };

  const handleCart = async () => {
    if (!requireUser()) return;
    try {
      await addItem(product.id, quantity);
      toast.success("Đã thêm sản phẩm vào giỏ hàng");
    } catch (error) {
      toast.error(getErrorMessage(error, "Không thể thêm vào giỏ hàng"));
    }
  };

  const handleWishlist = async () => {
    if (!requireUser()) return;
    try {
      await wishlistApi.add(product.id);
      toast.success("Đã lưu vào danh sách yêu thích");
    } catch (error) {
      toast.error(getErrorMessage(error, "Sản phẩm đã có trong yêu thích"));
    }
  };

  const submitReview = async (event) => {
    event.preventDefault();
    if (!requireUser()) return;
    setSubmitting(true);
    try {
      await reviewApi.create(product.id, reviewForm);
      setReviewForm({ rating: 5, comment: "" });
      await Promise.all([
        loadProduct(),
        loadReviews(0),
      ]);
      toast.success("Cảm ơn bạn đã đánh giá sản phẩm");
    } catch (error) {
      toast.error(getErrorMessage(error, "Chỉ khách đã nhận hàng mới có thể đánh giá"));
    } finally {
      setSubmitting(false);
    }
  };

  const removeReview = async (reviewId) => {
    if (!window.confirm("Bạn muốn xóa đánh giá này?")) return;
    try {
      await reviewApi.remove(reviewId);
      await Promise.all([
        loadProduct(),
        loadReviews(reviews.number),
      ]);
      toast.success("Đã xóa đánh giá");
    } catch (error) {
      toast.error(getErrorMessage(error));
    }
  };

  if (loading) return <Loader fullPage />;
  if (!product) {
    return <div className="container section"><EmptyState title="Không tìm thấy sản phẩm" action={<Link to="/products" className="button button--primary">Quay lại cửa hàng</Link>} /></div>;
  }

  const stock = product.quantity ?? 0;

  return (
    <div className="container product-detail-page">
      <nav className="breadcrumbs">
        <Link to="/">Trang chủ</Link><ChevronRight /><Link to="/products">Sản phẩm</Link><ChevronRight /><span>{product.name}</span>
      </nav>
      <section className="product-detail">
        <div className="product-detail__image">
          <img src={product.imageUrl || "/product-placeholder.svg"} alt={product.name} />
          {stock === 0 && <span>Hết hàng</span>}
        </div>
        <div className="product-detail__content">
          <span className="eyebrow">{product.category?.name || "Pet care"}</span>
          <h1>{product.name}</h1>
          <div className="product-detail__meta">
            <span className="rating-line"><Star fill="currentColor" /> {Number(product.averageRating || 0).toFixed(1)} ({product.reviewCount || 0} đánh giá)</span>
            <span>Đã bán {product.soldCount || 0}</span>
            {product.sku && <span>SKU: {product.sku}</span>}
          </div>
          <div className="product-detail__price">{formatCurrency(product.price)}</div>
          <p className="product-detail__description">{product.description || "Sản phẩm chăm sóc thú cưng được PetHub tuyển chọn."}</p>
          <dl className="product-facts">
            {product.brand && <><dt>Thương hiệu</dt><dd>{product.brand}</dd></>}
            {product.petType && <><dt>Phù hợp</dt><dd>{product.petType === "DOG" ? "Chó" : product.petType === "CAT" ? "Mèo" : "Thú cưng khác"}</dd></>}
            <dt>Tình trạng</dt><dd className={stock > 0 ? "text-success" : "text-danger"}>{stock > 0 ? `Còn ${stock} sản phẩm` : "Tạm hết hàng"}</dd>
          </dl>
          <div className="purchase-row">
            <div className="quantity-control">
              <button onClick={() => setQuantity((value) => Math.max(1, value - 1))}><Minus /></button>
              <input value={quantity} onChange={(e) => setQuantity(Math.min(stock, Math.max(1, Number(e.target.value) || 1)))} />
              <button onClick={() => setQuantity((value) => Math.min(stock, value + 1))}><Plus /></button>
            </div>
            <button className="button button--primary" onClick={handleCart} disabled={stock <= 0}>
              <ShoppingBag /> Thêm vào giỏ hàng
            </button>
            <button className="button button--outline button--icon" onClick={handleWishlist} aria-label="Thêm yêu thích"><Heart /></button>
          </div>
          <div className="product-assurances">
            <span><Truck /><span><strong>Giao hàng nhanh</strong>Toàn quốc</span></span>
            <span><ShieldCheck /><span><strong>Thanh toán an toàn</strong>COD & VNPay</span></span>
            <span><Check /><span><strong>Chính hãng</strong>Cam kết nguồn gốc</span></span>
          </div>
        </div>
      </section>

      <section className="reviews-section">
        <div className="reviews-header">
          <div><span className="eyebrow">Chia sẻ từ khách hàng</span><h2>Đánh giá sản phẩm</h2></div>
          <div className="rating-summary"><strong>{Number(product.averageRating || 0).toFixed(1)}</strong><span><Star fill="currentColor" /> {reviews.totalElements || 0} đánh giá</span></div>
        </div>
        <div className="reviews-layout">
          <div>
            {reviews.content.length ? reviews.content.map((review) => (
              <article className="review-card" key={review.id}>
                <div className="review-card__top">
                  <div className="review-author"><span className="avatar">{review.userName?.charAt(0)}</span><span><strong>{review.userName}</strong><small>{formatDateTime(review.createdAt)}</small></span></div>
                  <span className="review-stars">{Array.from({ length: 5 }, (_, index) => <Star key={index} fill={index < review.rating ? "currentColor" : "none"} />)}</span>
                </div>
                {review.verifiedPurchase && <span className="verified"><Check /> Đã mua hàng</span>}
                <p>{review.comment}</p>
                {user?.id === review.userId && <button className="text-button text-danger" onClick={() => removeReview(review.id)}>Xóa đánh giá</button>}
              </article>
            )) : <EmptyState title="Chưa có đánh giá" description="Hãy là người đầu tiên chia sẻ trải nghiệm." />}
            <Pagination page={reviews.number} totalPages={reviews.totalPages} onChange={loadReviews} />
          </div>
          <form className="review-form card" onSubmit={submitReview}>
            <h3>Viết đánh giá của bạn</h3>
            <p>Áp dụng cho khách hàng đã nhận sản phẩm.</p>
            <label>Mức hài lòng
              <div className="star-input">
                {Array.from({ length: 5 }, (_, index) => (
                  <button type="button" key={index} onClick={() => setReviewForm((form) => ({ ...form, rating: index + 1 }))}>
                    <Star fill={index < reviewForm.rating ? "currentColor" : "none"} />
                  </button>
                ))}
              </div>
            </label>
            <label>Nhận xét
              <textarea required minLength="5" maxLength="1000" rows="5" value={reviewForm.comment} onChange={(e) => setReviewForm((form) => ({ ...form, comment: e.target.value }))} placeholder="Sản phẩm có phù hợp với thú cưng của bạn không?" />
            </label>
            <button className="button button--primary button--full" disabled={submitting}>{submitting ? "Đang gửi..." : "Gửi đánh giá"}</button>
          </form>
        </div>
      </section>

      {related.length > 0 && <section className="section related-section"><div className="section-heading"><div><span className="eyebrow">Có thể bạn quan tâm</span><h2>Sản phẩm cùng danh mục</h2></div></div><div className="product-grid">{related.map((item) => <ProductCard key={item.id} product={item} />)}</div></section>}
    </div>
  );
}
