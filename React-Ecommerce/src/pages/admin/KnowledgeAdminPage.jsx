import { BookOpenText, ExternalLink, FilePlus2, Power, Trash2, X } from "lucide-react";
import { useEffect, useState } from "react";
import toast from "react-hot-toast";
import { adminApi } from "../../api/api";
import AdminPageHeader from "../../components/admin/AdminPageHeader";
import Loader from "../../components/common/Loader";
import { formatDateTime, getErrorMessage } from "../../utils/format";

export default function KnowledgeAdminPage() {
  const [documents, setDocuments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [editor, setEditor] = useState(false);
  const [form, setForm] = useState({ title: "", sourceUrl: "", content: "" });
  const [saving, setSaving] = useState(false);
  const load = () => adminApi.knowledgeDocuments().then((response) => setDocuments(response.data ?? [])).finally(() => setLoading(false));
  useEffect(() => { load(); }, []);

  const create = async (event) => {
    event.preventDefault(); setSaving(true);
    try { await adminApi.createKnowledgeDocument(form); toast.success("Đã lập chỉ mục tài liệu"); setEditor(false); setForm({ title: "", sourceUrl: "", content: "" }); load(); }
    catch (error) { toast.error(getErrorMessage(error)); }
    finally { setSaving(false); }
  };
  const toggle = async (document) => {
    try { const response = await adminApi.setKnowledgeDocumentActive(document.id, !document.active); setDocuments((current) => current.map((item) => item.id === document.id ? response.data : item)); toast.success("Đã cập nhật tài liệu"); }
    catch (error) { toast.error(getErrorMessage(error)); }
  };
  const remove = async (document) => {
    if (!window.confirm(`Xóa vĩnh viễn tài liệu "${document.title}"?`)) return;
    try { await adminApi.deleteKnowledgeDocument(document.id); setDocuments((current) => current.filter((item) => item.id !== document.id)); toast.success("Đã xóa tài liệu"); }
    catch (error) { toast.error(getErrorMessage(error)); }
  };

  return (
    <>
      <AdminPageHeader title="Kho tri thức RAG" description="Nguồn dữ liệu để PetHub AI tư vấn đúng sản phẩm và kiến thức chăm sóc." action={<button className="button button--primary" onClick={() => setEditor(true)}><FilePlus2 /> Thêm tài liệu</button>} />
      <div className="knowledge-note"><BookOpenText /><div><strong>Tài liệu tốt giúp AI trả lời tốt hơn</strong><p>Mỗi tài liệu nên tập trung vào một chủ đề, có nội dung chính xác và ghi rõ nguồn tham khảo.</p></div></div>
      {loading ? <Loader /> : <div className="knowledge-grid">{documents.map((document) => <article className="admin-card knowledge-card" key={document.id}><header><span><BookOpenText /></span><div><h2>{document.title}</h2><p>{document.chunkCount} đoạn tri thức · {formatDateTime(document.updatedAt)}</p></div></header>{document.sourceUrl && <a href={document.sourceUrl} target="_blank" rel="noreferrer">{document.sourceUrl} <ExternalLink /></a>}<footer><span className={`status ${document.active ? "status--active" : "status--inactive"}`}>{document.active ? "Đang sử dụng" : "Đã tạm dừng"}</span><div><button className="icon-button" title={document.active ? "Tạm dừng" : "Kích hoạt"} onClick={() => toggle(document)}><Power /></button><button className="icon-button icon-button--danger" title="Xóa" onClick={() => remove(document)}><Trash2 /></button></div></footer></article>)}</div>}
      {editor && <><div className="modal modal--large"><header><h2>Thêm tài liệu vào kho tri thức</h2><button className="icon-button" onClick={() => setEditor(false)}><X /></button></header><form onSubmit={create}><label>Tiêu đề<input required maxLength="255" value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} placeholder="Ví dụ: Hướng dẫn chọn thức ăn cho mèo con" /></label><label>URL nguồn tham khảo<input type="url" maxLength="1000" value={form.sourceUrl} onChange={(e) => setForm({ ...form, sourceUrl: e.target.value })} placeholder="https://..." /></label><label>Nội dung<textarea required rows="14" maxLength="200000" value={form.content} onChange={(e) => setForm({ ...form, content: e.target.value })} placeholder="Dán nội dung kiến thức đã kiểm chứng..." /></label><div className="modal__actions"><button type="button" className="button button--ghost" onClick={() => setEditor(false)}>Hủy</button><button className="button button--primary" disabled={saving}>{saving ? "Đang lập chỉ mục..." : "Lưu và lập chỉ mục"}</button></div></form></div><button className="modal-backdrop" onClick={() => setEditor(false)} aria-label="Đóng" /></>}
    </>
  );
}
