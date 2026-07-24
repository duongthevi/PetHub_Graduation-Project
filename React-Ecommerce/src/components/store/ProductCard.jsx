import { Heart, ShoppingBag, Star } from "lucide-react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import toast from "react-hot-toast";
import { useAuth } from "../../context/AuthContext";
import { useCart } from "../../context/CartContext";
import { wishlistApi } from "../../api/api";
import { formatCurrency } from "../../utils/format";

export default function ProductCard({ product, onWishlistChange }) {
  const { isAuthenticated, isAdmin } = useAuth();
  const { addItem } = useCart();
  const navigate = useNavigate();
  const location = useLocation();
  const stock = product.quantity ?? product.stock ?? 0;

  const requireUser = () => {
    if (!isAuthenticated) {
      navigate("/login", { state: { from: location } });
      return false;
    }
    if (isAdmin) {
      toast.error("Tài khoản quản trị không sử dụng giỏ hàng");
      return false;
    }
    return true;
  };

  const addToCart = async () => {
    if (!requireUser()) return;
    try {
      await addItem(product.id, 1);
      toast.success(`Đã thêm ${product.name} vào giỏ`);
    } catch (error) {
      toast.error(error.response?.data?.message || "Không thể thêm vào giỏ hàng");
    }
  };

  const addToWishlist = async () => {
    if (!requireUser()) return;
    try {
      await wishlistApi.add(product.id);
      toast.success("Đã thêm vào danh sách yêu thích");
      onWishlistChange?.(product.id);
    } catch (error) {
      toast.error(error.response?.data?.message || "Sản phẩm đã có trong yêu thích");
    }
  };

  return (
    <article className="product-card">
      <div className="product-card__media">
        <Link to={`/products/${product.id}`}>
          <img src={product.imageUrl || "/product-placeholder.svg"} alt={product.name} />
        </Link>
        <button className="product-card__heart" onClick={addToWishlist} aria-label="Yêu thích">
          <Heart size={19} />
        </button>
        {stock <= 0 && <span className="product-card__badge">Hết hàng</span>}
      </div>
      <div className="product-card__body">
        <span className="product-card__category">{product.category?.name || product.petType || "Pet care"}</span>
        <Link to={`/products/${product.id}`} className="product-card__name">{product.name}</Link>
        <div className="rating-line">
          <Star size={15} fill="currentColor" />
          <strong>{Number(product.averageRating || 0).toFixed(1)}</strong>
          <span>({product.reviewCount || 0})</span>
        </div>
        <div className="product-card__footer">
          <strong>{formatCurrency(product.price)}</strong>
          <button
            className="product-card__cart"
            onClick={addToCart}
            disabled={stock <= 0 || product.active === false}
            aria-label="Thêm vào giỏ"
          >
            <ShoppingBag size={18} />
          </button>
        </div>
      </div>
    </article>
  );
}
