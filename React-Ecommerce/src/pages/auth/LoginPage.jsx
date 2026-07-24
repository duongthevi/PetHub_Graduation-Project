import { useState } from "react";
import { Eye, EyeOff, LockKeyhole, Mail } from "lucide-react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import toast from "react-hot-toast";
import AuthShell from "../../components/auth/AuthShell";
import { useAuth } from "../../context/AuthContext";
import { getErrorMessage } from "../../utils/format";

export default function LoginPage() {
  const [form, setForm] = useState({ email: "", password: "" });
  const [showPassword, setShowPassword] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const submit = async (event) => {
    event.preventDefault();
    setSubmitting(true);
    try {
      const user = await login(form);
      toast.success(`Chào mừng ${user.name} quay lại`);
      const fallback = user.role === "ADMIN" ? "/admin/dashboard" : "/";
      navigate(location.state?.from?.pathname || fallback, { replace: true });
    } catch (error) {
      toast.error(getErrorMessage(error, "Email hoặc mật khẩu chưa chính xác"));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <AuthShell
      title="Đăng nhập"
      subtitle="Tiếp tục hành trình chăm sóc người bạn nhỏ."
      footer={<p>Chưa có tài khoản? <Link to="/register">Đăng ký miễn phí</Link></p>}
    >
      <form className="form-stack" onSubmit={submit}>
        <label>Email
          <span className="input-with-icon"><Mail /><input type="email" required autoComplete="email" placeholder="ban@example.com" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} /></span>
        </label>
        <label>Mật khẩu
          <span className="input-with-icon"><LockKeyhole /><input type={showPassword ? "text" : "password"} required autoComplete="current-password" placeholder="Nhập mật khẩu" value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} /><button type="button" onClick={() => setShowPassword((v) => !v)}>{showPassword ? <EyeOff /> : <Eye />}</button></span>
        </label>
        <div className="form-helper"><span /><Link to="/forgot-password">Quên mật khẩu?</Link></div>
        <button className="button button--primary button--full" disabled={submitting}>{submitting ? "Đang đăng nhập..." : "Đăng nhập"}</button>
      </form>
    </AuthShell>
  );
}
