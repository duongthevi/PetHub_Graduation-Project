import { ArrowLeft, ImagePlus, Save } from "lucide-react";
import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import toast from "react-hot-toast";
import { adminApi, catalogApi } from "../../api/api";
import AdminPageHeader from "../../components/admin/AdminPageHeader";
import Loader from "../../components/common/Loader";
import { getErrorMessage } from "../../utils/format";

const empty = { name: "", description: "", price: "", quantity: "", categoryId: "", sku: "", brand: "", petType: "DOG", active: true };

export default function ProductFormPage() {
  const { productId } = useParams();
  const editing = Boolean(productId);
  const [form, setForm] = useState(empty);
  const [categories, setCategories] = useState([]);
  const [image, setImage] = useState(null);
  const [preview, setPreview] = useState("");
  const [loading, setLoading] = useState(editing);
  const [submitting, setSubmitting] = useState(false);
  const navigate = useNavigate();

  useEffect(() => {
    catalogApi.categories().then((response) => setCategories(response.categoryList ?? []));
    if (editing) {
      catalogApi.getById(productId).then((response) => {
        const product = response.product;
        setForm({ name: product.name || "", description: product.description || "", price: product.price || "", quantity: product.quantity ?? "", categoryId: product.category?.id || "", sku: product.sku || "", brand: product.brand || "", petType: product.petType || "DOG", active: product.active });
        setPreview(product.imageUrl || "");
      }).finally(() => setLoading(false));
    }
  }, [editing, productId]);

  const selectImage = (event) => {
    const file = event.target.files?.[0];
    if (!file) return;
    setImage(file);
    setPreview(URL.createObjectURL(file));
  };

  const submit = async (event) => {
    event.preventDefault();
    if (!editing && !image) return toast.error("Vui lòng chọn ảnh sản phẩm");
    if (Number(form.price) > 1_000_000_000) {
      return toast.error("Giá sản phẩm không được vượt quá 1.000.000.000đ");
    }
    setSubmitting(true);
    const payload = new FormData();
    if (editing) payload.append("productId", productId);
    Object.entries(form).forEach(([key, value]) => payload.append(key, value));
    if (image) payload.append("image", image);
    try {
      if (editing) await adminApi.updateProduct(payload);
      else await adminApi.createProduct(payload);
      toast.success(editing ? "Đã cập nhật sản phẩm" : "Đã tạo sản phẩm");
      navigate("/admin/products");
    } catch (error) {
      toast.error(getErrorMessage(error, "Không thể lưu sản phẩm"));
    } finally { setSubmitting(false); }
  };

  if (loading) return <Loader fullPage />;
  const field = (name, label, type = "text", attributes = {}) => <label>{label}<input type={type} required value={form[name]} onChange={(e) => setForm({ ...form, [name]: e.target.value })} {...attributes} /></label>;

  return (
    <>
      <Link to="/admin/products" className="back-link"><ArrowLeft /> Quay lại sản phẩm</Link>
      <AdminPageHeader title={editing ? "Cập nhật sản phẩm" : "Thêm sản phẩm mới"} description="Điền thông tin rõ ràng để khách hàng dễ ra quyết định." />
      <form className="admin-form-layout" onSubmit={submit}>
        <section className="admin-card product-form-main"><h2>Thông tin sản phẩm</h2>{field("name", "Tên sản phẩm")}<label>Mô tả<textarea rows="8" required value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} /></label><div className="form-grid">{field("price", "Giá bán (VND)", "number", { min: 1, max: 1_000_000_000, step: 1 })}{field("quantity", "Tồn kho", "number", { min: editing ? 0 : 1, step: 1 })}{field("sku", "Mã SKU")}{field("brand", "Thương hiệu")}</div></section>
        <aside><section className="admin-card"><h2>Ảnh đại diện</h2><label className="image-upload">{preview ? <img src={preview} alt="Xem trước" /> : <ImagePlus />}<span>{image ? image.name : "Chọn ảnh JPG, PNG hoặc WebP"}</span><input type="file" accept="image/png,image/jpeg,image/webp" onChange={selectImage} /></label></section><section className="admin-card"><h2>Phân loại</h2><label>Danh mục<select required value={form.categoryId} onChange={(e) => setForm({ ...form, categoryId: e.target.value })}><option value="">Chọn danh mục</option>{categories.map((category) => <option key={category.id} value={category.id}>{category.name}</option>)}</select></label><label>Loại thú cưng<select value={form.petType} onChange={(e) => setForm({ ...form, petType: e.target.value })}><option value="DOG">Chó</option><option value="CAT">Mèo</option><option value="OTHER">Khác</option></select></label>{editing && <label className="switch"><input type="checkbox" checked={form.active} onChange={(e) => setForm({ ...form, active: e.target.checked })} /><span /> Đang kinh doanh</label>}</section><button className="button button--primary button--full" disabled={submitting}><Save /> {submitting ? "Đang lưu..." : "Lưu sản phẩm"}</button></aside>
      </form>
    </>
  );
}
