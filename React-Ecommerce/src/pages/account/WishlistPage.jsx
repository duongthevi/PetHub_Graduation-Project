import { Heart, ShoppingBag, Trash2 } from "lucide-react";
import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import toast from "react-hot-toast";
import { wishlistApi } from "../../api/api";
import EmptyState from "../../components/common/EmptyState";
import Loader from "../../components/common/Loader";
import { useCart } from "../../context/CartContext";
import { formatCurrency, getErrorMessage } from "../../utils/format";

export default function WishlistPage() {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const { addItem } = useCart();

  const load = () => wishlistApi.get().then((response) => setItems(response.data ?? [])).finally(() => setLoading(false));
  useEffect(() => { load(); }, []);

  const remove = async (productId) => {
    try {
      await wishlistApi.remove(productId);
      setItems((current) => current.filter((item) => item.product.id !== productId));
      toast.success("Đã xóa khỏi danh sách yêu thích");
    } catch (error) { toast.error(getErrorMessage(error)); }
  };

  const moveToCart = async (productId) => {
    try {
      await addItem(productId, 1);
      await remove(productId);
      toast.success("Đã chuyển sản phẩm vào giỏ hàng");
    } catch (error) { toast.error(getErrorMessage(error)); }
  };

  if (loading) return <Loader fullPage />;
  return (
    <div className="container section">
      <div className="page-heading"><div><span className="eyebrow">Dành riêng cho bạn</span><h1>Sản phẩm yêu thích</h1><p>{items.length} sản phẩm đã lưu</p></div></div>
      {!items.length ? <EmptyState icon={Heart} title="Chưa có sản phẩm yêu thích" description="Nhấn biểu tượng trái tim để lưu lại sản phẩm bạn quan tâm." action={<Link to="/products" className="button button--primary">Khám phá sản phẩm</Link>} /> : (
        <div className="wishlist-grid">
          {items.map(({ id, product }) => (
            <article className="wishlist-item card" key={id}>
              <Link to={`/products/${product.id}`}><img src={product.imageUrl || "/product-placeholder.svg"} alt={product.name} /></Link>
              <div><small>{product.sku || "PetHub"}</small><Link to={`/products/${product.id}`}><h3>{product.name}</h3></Link><strong>{formatCurrency(product.price)}</strong><span className={product.active && product.stock > 0 ? "text-success" : "text-danger"}>{product.active && product.stock > 0 ? `Còn ${product.stock} sản phẩm` : "Tạm hết hàng"}</span></div>
              <footer><button className="button button--primary button--small" disabled={!product.active || product.stock <= 0} onClick={() => moveToCart(product.id)}><ShoppingBag /> Thêm vào giỏ</button><button className="icon-button icon-button--danger" onClick={() => remove(product.id)}><Trash2 /></button></footer>
            </article>
          ))}
        </div>
      )}
    </div>
  );
}
