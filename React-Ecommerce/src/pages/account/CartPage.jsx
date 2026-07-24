import { Minus, Plus, ShoppingBag, Trash2 } from "lucide-react";
import { Link, useNavigate } from "react-router-dom";
import toast from "react-hot-toast";
import EmptyState from "../../components/common/EmptyState";
import Loader from "../../components/common/Loader";
import { useCart } from "../../context/CartContext";
import { formatCurrency, getErrorMessage } from "../../utils/format";

export default function CartPage() {
  const { cart, loading, updateItem, removeItem, clearCart } = useCart();
  const navigate = useNavigate();

  const update = async (item, quantity) => {
    if (quantity < 1) return;
    try {
      await updateItem(item.id, quantity);
    } catch (error) {
      toast.error(getErrorMessage(error, "Không thể cập nhật số lượng"));
    }
  };

  const remove = async (id) => {
    try {
      await removeItem(id);
      toast.success("Đã xóa sản phẩm khỏi giỏ");
    } catch (error) {
      toast.error(getErrorMessage(error));
    }
  };

  if (loading) return <Loader fullPage />;
  if (!cart.items?.length) {
    return <div className="container section"><EmptyState icon={ShoppingBag} title="Giỏ hàng đang trống" description="Khám phá các sản phẩm được PetHub tuyển chọn cho thú cưng của bạn." action={<Link to="/products" className="button button--primary">Tiếp tục mua sắm</Link>} /></div>;
  }

  return (
    <div className="container checkout-shell">
      <section className="cart-section">
        <div className="page-heading"><div><span className="eyebrow">Giỏ hàng của bạn</span><h1>{cart.totalItems} sản phẩm</h1></div><button className="text-button text-danger" onClick={async () => { if (window.confirm("Xóa toàn bộ giỏ hàng?")) await clearCart(); }}><Trash2 /> Xóa tất cả</button></div>
        <div className="cart-list">
          {cart.items.map((item) => (
            <article className={`cart-item ${!item.available ? "is-unavailable" : ""}`} key={item.id}>
              <img src={item.product.imageUrl || "/product-placeholder.svg"} alt={item.product.name} />
              <div className="cart-item__info"><Link to={`/products/${item.product.id}`}>{item.product.name}</Link><small>SKU: {item.product.sku || "—"}</small>{!item.available && <span className="text-danger">Sản phẩm không còn đủ tồn kho</span>}</div>
              <div className="quantity-control"><button onClick={() => update(item, item.quantity - 1)}><Minus /></button><input value={item.quantity} onChange={(e) => update(item, Number(e.target.value))} /><button onClick={() => update(item, item.quantity + 1)}><Plus /></button></div>
              <strong>{formatCurrency(item.lineTotal)}</strong>
              <button className="icon-button icon-button--danger" onClick={() => remove(item.id)} aria-label="Xóa"><Trash2 /></button>
            </article>
          ))}
        </div>
      </section>
      <aside className="order-summary card">
        <h2>Tóm tắt đơn hàng</h2>
        <dl><dt>Tạm tính</dt><dd>{formatCurrency(cart.subtotal)}</dd><dt>Phí vận chuyển</dt><dd>Tính khi đặt hàng</dd><dt>Giảm giá</dt><dd>Áp dụng ở bước tiếp theo</dd></dl>
        <div className="order-summary__total"><span>Tổng dự kiến</span><strong>{formatCurrency(cart.subtotal)}</strong></div>
        <button className="button button--primary button--full" disabled={cart.items.some((item) => !item.available)} onClick={() => navigate("/checkout")}>Tiến hành đặt hàng</button>
        <Link to="/products" className="text-link">← Tiếp tục mua sắm</Link>
      </aside>
    </div>
  );
}
