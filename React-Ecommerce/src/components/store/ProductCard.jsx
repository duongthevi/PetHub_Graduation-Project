import { useState } from "react";
import { Heart, ShoppingBag, Star } from "lucide-react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import toast from "react-hot-toast";
import { useAuth } from "../../context/AuthContext";
import { useCart } from "../../context/CartContext";
import { formatCurrency } from "../../utils/format";
import { useWishlist } from "../../context/WishlistContext";

export default function ProductCard({ product, onWishlistChange }) {
  const [wishlistLoading, setWishlistLoading] = useState(false);
  const { isAuthenticated, isAdmin } = useAuth();
  const { addItem } = useCart();
  const { isFavorite: checkFavorite, addFavorite, removeFavorite } = useWishlist();
  const navigate = useNavigate();
  const location = useLocation();
  const stock = product.quantity ?? product.stock ?? 0;
  const isFavorite = checkFavorite(product.id);

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

  const toggleWishlist = async () => {
    if (!requireUser() || wishlistLoading) return;
    setWishlistLoading(true);
    try {
      if (isFavorite) {
        await removeFavorite(product.id);
        toast.success("Đã xóa khỏi danh sách yêu thích");
      } else {
        await addFavorite(product.id);
        toast.success("Đã thêm vào danh sách yêu thích");
      }
      onWishlistChange?.(product.id, !isFavorite);
    } catch (error) {
      toast.error(error.response?.data?.message || "Không thể cập nhật danh sách yêu thích");
    } finally {
      setWishlistLoading(false);
    }
  };

  return (
    <article className="product-card">
      <div className="product-card__media">
        <Link to={`/products/${product.id}`}>
          <img src={product.imageUrl || "/product-placeholder.svg"} alt={product.name} />
        </Link>
        <button
          className={`product-card__heart${isFavorite ? " is-favorite" : ""}`}
          onClick={toggleWishlist}
          disabled={wishlistLoading}
          aria-label={isFavorite ? "Xóa khỏi yêu thích" : "Thêm vào yêu thích"}
          aria-pressed={isFavorite}
        >
          <Heart size={19} fill={isFavorite ? "currentColor" : "none"} />
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
