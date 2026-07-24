import { useState } from "react";
import { LockKeyhole } from "lucide-react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import toast from "react-hot-toast";
import AuthShell from "../../components/auth/AuthShell";
import { authApi } from "../../api/api";
import { getErrorMessage } from "../../utils/format";

export default function ResetPasswordPage() {
  const [params] = useSearchParams();
  const [form, setForm] = useState({ password: "", confirm: "" });
  const [submitting, setSubmitting] = useState(false);
  const navigate = useNavigate();
  const token = params.get("token") || "";

  const submit = async (event) => {
    event.preventDefault();
    if (form.password !== form.confirm) return toast.error("Mật khẩu xác nhận chưa trùng khớp");
    setSubmitting(true);
    try {
      await authApi.resetPassword({ token, newPassword: form.password });
      toast.success("Đặt lại mật khẩu thành công");
      navigate("/login", { replace: true });
    } catch (error) {
      toast.error(getErrorMessage(error, "Liên kết không hợp lệ hoặc đã hết hạn"));
    } finally {
      setSubmitting(false);
    }
  };

  return <AuthShell title="Đặt mật khẩu mới" subtitle="Chọn mật khẩu mạnh và không dùng lại ở dịch vụ khác." footer={<p><Link to="/login">← Quay lại đăng nhập</Link></p>}><form className="form-stack" onSubmit={submit}><label>Mật khẩu mới<span className="input-with-icon"><LockKeyhole /><input type="password" required minLength="8" value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} /></span></label><label>Xác nhận mật khẩu<span className="input-with-icon"><LockKeyhole /><input type="password" required minLength="8" value={form.confirm} onChange={(e) => setForm({ ...form, confirm: e.target.value })} /></span></label><button className="button button--primary button--full" disabled={!token || submitting}>{submitting ? "Đang cập nhật..." : "Cập nhật mật khẩu"}</button>{!token && <p className="form-error">Liên kết thiếu mã xác thực.</p>}</form></AuthShell>;
}
