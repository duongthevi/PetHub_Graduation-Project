import { Search, ShieldCheck, UserCheck, UserX } from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import toast from "react-hot-toast";
import { adminApi } from "../../api/api";
import AdminPageHeader from "../../components/admin/AdminPageHeader";
import Loader from "../../components/common/Loader";
import { getErrorMessage } from "../../utils/format";

export default function AdminUsersPage() {
  const [users, setUsers] = useState([]);
  const [query, setQuery] = useState("");
  const [loading, setLoading] = useState(true);
  useEffect(() => { adminApi.users().then((response) => setUsers(response.userList ?? [])).finally(() => setLoading(false)); }, []);
  const visible = useMemo(() => users.filter((user) => `${user.name} ${user.email} ${user.phoneNumber}`.toLowerCase().includes(query.toLowerCase())), [users, query]);

  const setEnabled = async (user, enabled) => {
    const action = enabled ? "mở khóa" : "khóa";
    if (!window.confirm(`Bạn muốn ${action} tài khoản ${user.email}?`)) return;
    try {
      await adminApi.setUserEnabled(user.id, enabled);
      setUsers((current) => current.map((item) => item.id === user.id ? { ...item, enabled } : item));
      toast.success(`Đã ${action} tài khoản`);
    } catch (error) { toast.error(getErrorMessage(error)); }
  };

  return (
    <>
      <AdminPageHeader title="Quản lý người dùng" description={`${users.length} tài khoản đã đăng ký`} />
      <section className="admin-card"><div className="table-toolbar"><span className="admin-search"><Search /><input value={query} onChange={(e) => setQuery(e.target.value)} placeholder="Tìm tên, email, số điện thoại..." /></span></div>{loading ? <Loader /> : <div className="table-wrap"><table className="data-table"><thead><tr><th>Người dùng</th><th>Liên hệ</th><th>Vai trò</th><th>Trạng thái</th><th>Kiểm soát</th></tr></thead><tbody>{visible.map((user) => <tr key={user.id}><td><div className="table-user"><span className="avatar">{user.name?.charAt(0)}</span><span><strong>{user.name}</strong><small>ID #{user.id}</small></span></div></td><td>{user.email}<br /><small>{user.phoneNumber}</small></td><td><span className="role-badge"><ShieldCheck /> {user.role}</span></td><td><span className={`status ${user.enabled === false ? "status--inactive" : "status--active"}`}>{user.enabled === false ? "Đã khóa" : "Hoạt động"}</span></td><td>{user.role !== "ADMIN" && <div className="table-actions"><button className="button button--outline button--small button--danger" onClick={() => setEnabled(user, false)} disabled={user.enabled === false}><UserX /> Khóa</button><button className="button button--outline button--small" onClick={() => setEnabled(user, true)} disabled={user.enabled !== false}><UserCheck /> Mở</button></div>}</td></tr>)}</tbody></table></div>}</section>
    </>
  );
}
