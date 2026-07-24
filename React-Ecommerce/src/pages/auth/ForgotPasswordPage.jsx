import { useState } from "react";
import { Mail } from "lucide-react";
import { Link } from "react-router-dom";
import toast from "react-hot-toast";
import AuthShell from "../../components/auth/AuthShell";
import { authApi } from "../../api/api";
import { getErrorMessage } from "../../utils/format";

export default function ForgotPasswordPage() {
  const [email, setEmail] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const submit = async (event) => {
    event.preventDefault();
    setSubmitting(true);
    try {
      await authApi.forgotPassword(email);
      toast.success("Nếu email tồn tại, hướng dẫn đặt lại mật khẩu đã được gửi.");
      setEmail("");
    } catch (error) {
      toast.error(getErrorMessage(error));
    } finally {
      setSubmitting(false);
    }
  };

  return <AuthShell title="Quên mật khẩu?" subtitle="Nhập email để nhận liên kết khôi phục." footer={<p><Link to="/login">← Quay lại đăng nhập</Link></p>}><form className="form-stack" onSubmit={submit}><label>Email tài khoản<span className="input-with-icon"><Mail /><input type="email" required value={email} onChange={(e) => setEmail(e.target.value)} placeholder="ban@example.com" /></span></label><button className="button button--primary button--full" disabled={submitting}>{submitting ? "Đang gửi..." : "Gửi hướng dẫn"}</button></form></AuthShell>;
}
