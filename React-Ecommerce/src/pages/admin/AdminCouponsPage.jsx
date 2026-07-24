import { Edit3, Plus, TicketPercent, Trash2, X } from "lucide-react";
import { useEffect, useState } from "react";
import toast from "react-hot-toast";
import { adminApi } from "../../api/api";
import AdminPageHeader from "../../components/admin/AdminPageHeader";
import Loader from "../../components/common/Loader";
import { formatCurrency, formatDateTime, getErrorMessage } from "../../utils/format";

const toLocalInput = (value) => value ? new Date(value).toISOString().slice(0, 16) : "";
const empty = { code: "", description: "", type: "PERCENT", value: "", minimumOrderValue: "0", maximumDiscount: "", startsAt: "", expiresAt: "", usageLimit: "", perUserLimit: 1, active: true };

export default function AdminCouponsPage() {
  const [coupons, setCoupons] = useState([]);
  const [loading, setLoading] = useState(true);
  const [editor, setEditor] = useState(null);
  const [form, setForm] = useState(empty);
  const [saving, setSaving] = useState(false);
  const load = () => adminApi.coupons().then((response) => setCoupons(response.data ?? [])).finally(() => setLoading(false));
  useEffect(() => { load(); }, []);

  const open = (coupon = null) => {
    setEditor(coupon || {});
    setForm(coupon ? { ...coupon, startsAt: toLocalInput(coupon.startsAt), expiresAt: toLocalInput(coupon.expiresAt), maximumDiscount: coupon.maximumDiscount ?? "", usageLimit: coupon.usageLimit ?? "" } : { ...empty });
  };
  const save = async (event) => {
    event.preventDefault();
    setSaving(true);
    const payload = {
      ...form,
      code: form.code.toUpperCase(),
      value: Number(form.value),
      minimumOrderValue: Number(form.minimumOrderValue),
      maximumDiscount: form.maximumDiscount === "" ? null : Number(form.maximumDiscount),
      usageLimit: form.usageLimit === "" ? null : Number(form.usageLimit),
      perUserLimit: Number(form.perUserLimit),
      startsAt: new Date(form.startsAt).toISOString().slice(0, 19),
      expiresAt: new Date(form.expiresAt).toISOString().slice(0, 19),
    };
    try {
      if (editor.id) await adminApi.updateCoupon(editor.id, payload);
      else await adminApi.createCoupon(payload);
      toast.success(editor.id ? "Đã cập nhật mã giảm giá" : "Đã tạo mã giảm giá");
      setEditor(null); load();
    } catch (error) { toast.error(getErrorMessage(error)); }
    finally { setSaving(false); }
  };
  const remove = async (coupon) => {
    if (!window.confirm(`Ngừng mã "${coupon.code}"?`)) return;
    try { await adminApi.deleteCoupon(coupon.id); toast.success("Đã ngừng mã giảm giá"); load(); }
    catch (error) { toast.error(getErrorMessage(error)); }
  };

  const input = (name, label, type = "text", required = true) => <label>{label}<input type={type} required={required} value={form[name] ?? ""} onChange={(e) => setForm({ ...form, [name]: e.target.value })} /></label>;

  return (
    <>
      <AdminPageHeader title="Mã giảm giá" description="Tạo và kiểm soát chương trình khuyến mãi của cửa hàng." action={<button className="button button--primary" onClick={() => open()}><Plus /> Tạo mã mới</button>} />
      {loading ? <Loader /> : <section className="admin-card"><div className="table-wrap"><table className="data-table"><thead><tr><th>Mã</th><th>Ưu đãi</th><th>Điều kiện</th><th>Thời hạn</th><th>Lượt dùng</th><th>Trạng thái</th><th /></tr></thead><tbody>{coupons.map((coupon) => <tr key={coupon.id}><td><span className="coupon-code"><TicketPercent /> {coupon.code}</span><small className="table-subtitle">{coupon.description}</small></td><td><strong>{coupon.type === "PERCENT" ? `${coupon.value}%` : formatCurrency(coupon.value)}</strong></td><td>Từ {formatCurrency(coupon.minimumOrderValue)}</td><td>{formatDateTime(coupon.startsAt)}<br />→ {formatDateTime(coupon.expiresAt)}</td><td>{coupon.usedCount}/{coupon.usageLimit ?? "∞"}</td><td><span className={`status ${coupon.active ? "status--active" : "status--inactive"}`}>{coupon.active ? "Hoạt động" : "Tạm dừng"}</span></td><td><div className="table-actions"><button className="icon-button" onClick={() => open(coupon)}><Edit3 /></button><button className="icon-button icon-button--danger" onClick={() => remove(coupon)}><Trash2 /></button></div></td></tr>)}</tbody></table></div></section>}
      {editor && <><div className="modal modal--large"><header><h2>{editor.id ? `Cập nhật ${editor.code}` : "Tạo mã giảm giá"}</h2><button className="icon-button" onClick={() => setEditor(null)}><X /></button></header><form onSubmit={save}><div className="form-grid">{input("code", "Mã giảm giá")}<label>Loại ưu đãi<select value={form.type} onChange={(e) => setForm({ ...form, type: e.target.value })}><option value="PERCENT">Theo phần trăm</option><option value="FIXED">Số tiền cố định</option></select></label><span className="form-span-2">{input("description", "Mô tả", "text", false)}</span>{input("value", form.type === "PERCENT" ? "Phần trăm giảm" : "Số tiền giảm", "number")}{input("minimumOrderValue", "Giá trị đơn tối thiểu", "number")}{input("maximumDiscount", "Giảm tối đa (để trống nếu không giới hạn)", "number", false)}{input("usageLimit", "Tổng lượt sử dụng", "number", false)}{input("perUserLimit", "Lượt dùng mỗi khách", "number")}{input("startsAt", "Bắt đầu", "datetime-local")}{input("expiresAt", "Kết thúc", "datetime-local")}<label className="switch"><input type="checkbox" checked={form.active} onChange={(e) => setForm({ ...form, active: e.target.checked })} /><span /> Kích hoạt</label></div><div className="modal__actions"><button type="button" className="button button--ghost" onClick={() => setEditor(null)}>Hủy</button><button className="button button--primary" disabled={saving}>{saving ? "Đang lưu..." : "Lưu mã"}</button></div></form></div><button className="modal-backdrop" onClick={() => setEditor(null)} aria-label="Đóng" /></>}
    </>
  );
}
