import { KeyRound, LogOut, MapPin, Save, UserRound } from "lucide-react";
import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import toast from "react-hot-toast";
import { profileApi } from "../../api/api";
import AccountNav from "../../components/account/AccountNav";
import Loader from "../../components/common/Loader";
import { useAuth } from "../../context/AuthContext";
import { formatDateTime, getErrorMessage } from "../../utils/format";

const emptyAddress = { recipientName: "", recipientPhone: "", street: "", ward: "", district: "", city: "", country: "Việt Nam" };

export default function ProfilePage() {
  const [profile, setProfile] = useState(null);
  const [basic, setBasic] = useState({ name: "", phoneNumber: "" });
  const [address, setAddress] = useState(emptyAddress);
  const [password, setPassword] = useState({ currentPassword: "", newPassword: "", confirmPassword: "" });
  const [saving, setSaving] = useState("");
  const { logout } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    profileApi.get().then((response) => {
      setProfile(response.data);
      setBasic({ name: response.data.name || "", phoneNumber: response.data.phoneNumber || "" });
      setAddress({ ...emptyAddress, ...(response.data.address || {}) });
    });
  }, []);

  const run = async (name, request, success) => {
    setSaving(name);
    try {
      await request();
      toast.success(success);
      if (name === "password") {
        setPassword({ currentPassword: "", newPassword: "", confirmPassword: "" });
        try {
          await logout();
        } catch {
          // logout() still clears the local session in its finally block.
        }
        navigate("/login", { replace: true });
      }
      return true;
    }
    catch (error) { toast.error(getErrorMessage(error)); return false; }
    finally { setSaving(""); }
  };

  const updateProfile = (event) => { event.preventDefault(); run("profile", () => profileApi.update(basic), "Đã cập nhật thông tin"); };
  const updateAddress = (event) => { event.preventDefault(); run("address", () => profileApi.saveAddress(address), "Đã lưu địa chỉ nhận hàng"); };
  const changePassword = async (event) => {
    event.preventDefault();
    if (password.newPassword !== password.confirmPassword) return toast.error("Mật khẩu xác nhận chưa trùng khớp");
    await run("password", () => profileApi.changePassword({ currentPassword: password.currentPassword, newPassword: password.newPassword }), "Đổi mật khẩu thành công. Vui lòng đăng nhập lại");
  };
  const logoutAll = async () => { if (window.confirm("Đăng xuất tài khoản này trên tất cả thiết bị?")) { await logout(true); navigate("/login"); } };

  if (!profile) return <Loader fullPage />;
  const field = (object, setter, name, label, type = "text", required = true) => <label>{label}<input type={type} required={required} value={object[name] || ""} onChange={(e) => setter({ ...object, [name]: e.target.value })} /></label>;

  return (
    <div className="container account-layout">
      <AccountNav />
      <section className="account-content">
        <div className="page-heading"><div><span className="eyebrow">Thành viên từ {formatDateTime(profile.createdAt)}</span><h1>Hồ sơ cá nhân</h1><p>Quản lý thông tin, địa chỉ và bảo mật tài khoản.</p></div></div>
        <form className="card profile-block" onSubmit={updateProfile}><h2><UserRound /> Thông tin cơ bản</h2><div className="form-grid">{field(basic, setBasic, "name", "Họ và tên")}<label>Email<input value={profile.email} disabled /></label>{field(basic, setBasic, "phoneNumber", "Số điện thoại", "tel")}</div><button className="button button--primary" disabled={saving === "profile"}><Save /> Lưu thông tin</button></form>
        <form className="card profile-block" onSubmit={updateAddress}><h2><MapPin /> Địa chỉ mặc định</h2><div className="form-grid">{field(address, setAddress, "recipientName", "Người nhận")}{field(address, setAddress, "recipientPhone", "Số điện thoại", "tel")}<span className="form-span-2">{field(address, setAddress, "street", "Số nhà, tên đường")}</span>{field(address, setAddress, "ward", "Phường / Xã", "text", false)}{field(address, setAddress, "district", "Quận / Huyện")}{field(address, setAddress, "city", "Tỉnh / Thành phố")}{field(address, setAddress, "country", "Quốc gia")}</div><button className="button button--primary" disabled={saving === "address"}><Save /> Lưu địa chỉ</button></form>
        <form className="card profile-block" onSubmit={changePassword}><h2><KeyRound /> Đổi mật khẩu</h2><div className="form-grid">{field(password, setPassword, "currentPassword", "Mật khẩu hiện tại", "password")}{field(password, setPassword, "newPassword", "Mật khẩu mới", "password")}{field(password, setPassword, "confirmPassword", "Xác nhận mật khẩu", "password")}</div><button className="button button--primary" disabled={saving === "password"}><KeyRound /> Đổi mật khẩu</button></form>
        <div className="card danger-zone"><div><h2>Phiên đăng nhập</h2><p>Thu hồi toàn bộ refresh token đang hoạt động của tài khoản.</p></div><button className="button button--outline button--danger" onClick={logoutAll}><LogOut /> Đăng xuất mọi thiết bị</button></div>
      </section>
    </div>
  );
}
