import { Heart, ShoppingBag, Trash2 } from "lucide-react";
import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import toast from "react-hot-toast";
import EmptyState from "../../components/common/EmptyState";
import Loader from "../../components/common/Loader";
import Pagination from "../../components/common/Pagination";
import { useCart } from "../../context/CartContext";
import { useWishlist } from "../../context/WishlistContext";
import { formatCurrency, getErrorMessage } from "../../utils/format";

const PAGE_SIZE = 6;

export default function WishlistPage() {
  const { items, loading, removeFavorite } = useWishlist();
  const { addItem } = useCart();
  const [page, setPage] = useState(0);
  const totalPages = Math.ceil(items.length / PAGE_SIZE);
  const visibleItems = items.slice(page * PAGE_SIZE, (page + 1) * PAGE_SIZE);

  useEffect(() => {
    setPage((current) => Math.min(current, Math.max(0, totalPages - 1)));
  }, [totalPages]);

  const changePage = (nextPage) => {
    setPage(nextPage);
    window.scrollTo({ top: 0, behavior: "smooth" });
  };

  const remove = async (productId) => {
    try {
      await removeFavorite(productId);
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
        <>
          <div className="wishlist-grid">
            {visibleItems.map(({ id, product }) => (
              <article className="wishlist-item card" key={id}>
                <Link to={`/products/${product.id}`}><img src={product.imageUrl || "/product-placeholder.svg"} alt={product.name} /></Link>
                <div><small>{product.sku || "PetHub"}</small><Link to={`/products/${product.id}`}><h3>{product.name}</h3></Link><strong>{formatCurrency(product.price)}</strong><span className={product.active && product.stock > 0 ? "text-success" : "text-danger"}>{product.active && product.stock > 0 ? `Còn ${product.stock} sản phẩm` : "Tạm hết hàng"}</span></div>
                <footer><button className="button button--primary button--small" disabled={!product.active || product.stock <= 0} onClick={() => moveToCart(product.id)}><ShoppingBag /> Thêm vào giỏ</button><button className="icon-button icon-button--danger" onClick={() => remove(product.id)}><Trash2 /></button></footer>
              </article>
            ))}
          </div>
          <Pagination page={page} totalPages={totalPages} onChange={changePage} />
        </>
      )}
    </div>
  );
}
