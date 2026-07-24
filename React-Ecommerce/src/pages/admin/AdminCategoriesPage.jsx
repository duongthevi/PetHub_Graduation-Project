import { Edit3, Plus, Shapes, Trash2, X } from "lucide-react";
import { useEffect, useState } from "react";
import toast from "react-hot-toast";
import { adminApi, catalogApi } from "../../api/api";
import AdminPageHeader from "../../components/admin/AdminPageHeader";
import Loader from "../../components/common/Loader";
import { getErrorMessage } from "../../utils/format";

export default function AdminCategoriesPage() {
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(true);
  const [editor, setEditor] = useState(null);
  const [name, setName] = useState("");
  const load = () => catalogApi.categories().then((response) => setCategories(response.categoryList ?? [])).finally(() => setLoading(false));
  useEffect(() => { load(); }, []);

  const open = (category = {}) => { setEditor(category); setName(category.name || ""); };
  const save = async (event) => {
    event.preventDefault();
    try {
      if (editor?.id) await adminApi.updateCategory(editor.id, { name });
      else await adminApi.createCategory({ name });
      toast.success(editor?.id ? "Đã cập nhật danh mục" : "Đã tạo danh mục");
      setEditor(null); load();
    } catch (error) { toast.error(getErrorMessage(error)); }
  };
  const remove = async (category) => {
    if (!window.confirm(`Xóa danh mục "${category.name}"?`)) return;
    try { await adminApi.deleteCategory(category.id); toast.success("Đã xóa danh mục"); load(); }
    catch (error) { toast.error(getErrorMessage(error, "Không thể xóa danh mục đang có sản phẩm")); }
  };

  return (
    <>
      <AdminPageHeader title="Quản lý danh mục" description="Tổ chức sản phẩm để khách hàng tìm kiếm dễ dàng hơn." action={<button className="button button--primary" onClick={() => open()}><Plus /> Thêm danh mục</button>} />
      {loading ? <Loader /> : <div className="category-admin-grid">{categories.map((category) => <article className="admin-card category-admin-card" key={category.id}><span><Shapes /></span><div><h2>{category.name}</h2><p>{category.productList?.length || 0} sản phẩm</p></div><div><button className="icon-button" onClick={() => open(category)}><Edit3 /></button><button className="icon-button icon-button--danger" onClick={() => remove(category)}><Trash2 /></button></div></article>)}</div>}
      {editor && <><div className="modal"><header><h2>{editor.id ? "Sửa danh mục" : "Thêm danh mục"}</h2><button className="icon-button" onClick={() => setEditor(null)}><X /></button></header><form onSubmit={save}><label>Tên danh mục<input autoFocus required maxLength="100" value={name} onChange={(e) => setName(e.target.value)} /></label><div className="modal__actions"><button type="button" className="button button--ghost" onClick={() => setEditor(null)}>Hủy</button><button className="button button--primary">Lưu danh mục</button></div></form></div><button className="modal-backdrop" onClick={() => setEditor(null)} aria-label="Đóng" /></>}
    </>
  );
}
