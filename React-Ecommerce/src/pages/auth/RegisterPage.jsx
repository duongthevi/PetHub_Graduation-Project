import { useState } from "react";
import { LockKeyhole, Mail, Phone, UserRound } from "lucide-react";
import { Link, useNavigate } from "react-router-dom";
import toast from "react-hot-toast";
import AuthShell from "../../components/auth/AuthShell";
import { useAuth } from "../../context/AuthContext";
import { getErrorMessage } from "../../utils/format";

export default function RegisterPage() {
  const [form, setForm] = useState({ name: "", email: "", phoneNumber: "", password: "", confirmPassword: "" });
  const [submitting, setSubmitting] = useState(false);
  const { register } = useAuth();
  const navigate = useNavigate();

  const submit = async (event) => {
    event.preventDefault();
    if (form.password !== form.confirmPassword) {
      toast.error("Mật khẩu xác nhận chưa trùng khớp");
      return;
    }
    setSubmitting(true);
    try {
      const payload = {
        name: form.name,
        email: form.email,
        phoneNumber: form.phoneNumber,
        password: form.password,
      };
      await register(payload);
      toast.success("Tạo tài khoản thành công. Bạn có thể đăng nhập.");
      navigate("/login", { replace: true });
    } catch (error) {
      toast.error(getErrorMessage(error, "Không thể tạo tài khoản"));
    } finally {
      setSubmitting(false);
    }
  };

  const field = (name, label, icon, type = "text", placeholder = "") => {
    const Icon = icon;
    return <label>{label}<span className="input-with-icon"><Icon /><input type={type} required value={form[name]} placeholder={placeholder} onChange={(e) => setForm({ ...form, [name]: e.target.value })} /></span></label>;
  };

  return (
    <AuthShell title="Tạo tài khoản" subtitle="Chỉ mất một phút để bắt đầu mua sắm." footer={<p>Đã có tài khoản? <Link to="/login">Đăng nhập</Link></p>}>
      <form className="form-stack" onSubmit={submit}>
        {field("name", "Họ và tên", UserRound, "text", "Nguyễn Văn A")}
        <div className="form-row">
          {field("email", "Email", Mail, "email", "ban@example.com")}
          {field("phoneNumber", "Số điện thoại", Phone, "tel", "09xxxxxxxx")}
        </div>
        <div className="form-row">
          {field("password", "Mật khẩu", LockKeyhole, "password", "Tối thiểu 8 ký tự")}
          {field("confirmPassword", "Xác nhận mật khẩu", LockKeyhole, "password", "Nhập lại mật khẩu")}
        </div>
        <label className="checkbox"><input type="checkbox" required /> <span>Tôi đồng ý với điều khoản sử dụng và chính sách bảo mật.</span></label>
        <button className="button button--primary button--full" disabled={submitting}>{submitting ? "Đang tạo tài khoản..." : "Đăng ký"}</button>
      </form>
    </AuthShell>
  );
}
