import { AlertTriangle, Edit3, Eye, Plus, Search, Trash2 } from "lucide-react";
import { useEffect, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import toast from "react-hot-toast";
import { adminApi} from "../../api/api";
import AdminPageHeader from "../../components/admin/AdminPageHeader";
import Loader from "../../components/common/Loader";
import Pagination from "../../components/common/Pagination";
import { formatCurrency, getErrorMessage } from "../../utils/format";

export default function AdminProductsPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const lowStock = searchParams.get("lowStock") === "true";
  const [data, setData] = useState({ content: [], number: 0, totalPages: 0, totalElements: 0 });
  const [query, setQuery] = useState("");
  const [loading, setLoading] = useState(true);

  const load = (page = 0, q = query) => {
    setLoading(true);
    adminApi.products({ q: q || undefined, lowStock: lowStock || undefined, page, size: 10, sortBy: lowStock ? "quantity" : "createdAt", direction: lowStock ? "asc" : "desc" })
      .then((response) => setData(response.data))
      .finally(() => setLoading(false));
  };
  useEffect(() => {
    setLoading(true);
    adminApi.products({ lowStock: lowStock || undefined, page: 0, size: 10, sortBy: lowStock ? "quantity" : "createdAt", direction: lowStock ? "asc" : "desc" })
      .then((response) => setData(response.data))
      .finally(() => setLoading(false));
  }, [lowStock]);

  const remove = async (product) => {
    if (!window.confirm(`Ngừng kinh doanh "${product.name}"?`)) return;
    try { await adminApi.deleteProduct(product.id); toast.success("Đã ngừng kinh doanh sản phẩm"); load(data.number); }
    catch (error) { toast.error(getErrorMessage(error)); }
  };

  return (
    <>
      <AdminPageHeader title="Quản lý sản phẩm" description={`${data.totalElements || 0} sản phẩm trong danh mục`} action={<Link className="button button--primary" to="/admin/products/new"><Plus /> Thêm sản phẩm</Link>} />
      <section className="admin-card">
        {lowStock && <div className="active-filter"><AlertTriangle size={18} /> Đang lọc sản phẩm gần hết hàng <button type="button" onClick={() => setSearchParams({})}>Xem tất cả</button></div>}
        <form className="table-toolbar" onSubmit={(e) => { e.preventDefault(); load(0); }}><span className="admin-search"><Search /><input value={query} onChange={(e) => setQuery(e.target.value)} placeholder="Tìm theo tên, SKU..." /></span><button className="button button--outline button--small">Tìm kiếm</button></form>
        {loading ? <Loader /> : <div className="table-wrap"><table className="data-table"><thead><tr><th>Sản phẩm</th><th>Danh mục</th><th>Giá bán</th><th>Tồn kho</th><th>Trạng thái</th><th aria-label="Thao tác" /></tr></thead><tbody>{data.content.map((product) => <tr key={product.id}><td><div className="table-product"><img src={product.imageUrl || "/product-placeholder.svg"} alt="" /><span><strong>{product.name}</strong><small>{product.sku || "Chưa có SKU"}</small></span></div></td><td>{product.category?.name || "—"}</td><td><strong>{formatCurrency(product.price)}</strong></td><td><span className={product.quantity < 10 ? "text-danger" : ""}>{product.quantity}</span></td><td><span className={`status ${product.active ? "status--active" : "status--inactive"}`}>{product.active ? "Đang bán" : "Tạm ẩn"}</span></td><td><div className="table-actions"><Link className="icon-button" to={`/products/${product.id}`} title="Xem"><Eye /></Link><Link className="icon-button" to={`/admin/products/${product.id}/edit`} title="Sửa"><Edit3 /></Link><button className="icon-button icon-button--danger" onClick={() => remove(product)} title="Ngừng bán"><Trash2 /></button></div></td></tr>)}</tbody></table></div>}
        <Pagination page={data.number} totalPages={data.totalPages} onChange={load} />
      </section>
    </>
  );
}
