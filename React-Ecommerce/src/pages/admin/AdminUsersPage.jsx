import { useCallback, useEffect, useState } from "react";
import toast from "react-hot-toast";
import { adminApi } from "../../api/api";
import AdminPageHeader from "../../components/admin/AdminPageHeader";
import Loader from "../../components/common/Loader";
import { getErrorMessage } from "../../utils/format";
import Pagination from "../../components/common/Pagination";

export default function AdminUsersPage() {
  const [data, setData] = useState({
      content: [],
      number: 0,
      totalPages: 0,
      totalElements: 0,
    });
    const [query, setQuery] = useState("");
    const [loading, setLoading] = useState(true);
    
    const load = useCallback(
    (page = 0) => {
      setLoading(true);

      adminApi
        .users({
          page,
          size: 10,
          q: query.trim() || undefined,
        })
        .then((response) => {
          setData(response.data);
        })
        .catch((error) => {
          toast.error(getErrorMessage(error));
        })
        .finally(() => {
          setLoading(false);
        });
    },
    [query]
  );

  useEffect(() => {
    const timeoutId = window.setTimeout(() => {
      load(0);
    }, 350);

    return () => {
      window.clearTimeout(timeoutId);
    };
  }, [load]);

  const toggleEnabled = async (user) => {
    try {
      await adminApi.setUserEnabled(user.id, !user.enabled);
      toast.success("Đã cập nhật trạng thái tài khoản");
      load(data.number);
    } catch (error) {
      toast.error(getErrorMessage(error));
    }
  };

  return (
    <>
      <AdminPageHeader
        title="Quản lý người dùng"
        description={`${data.totalElements || 0} tài khoản đã đăng ký`}
      />
      <section className="admin-card">
        <div className="table-toolbar">
          <input
            type="search"
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            placeholder="Tìm theo tên, email, số điện thoại"
          />
        </div>

        {loading ? (
          <Loader />
        ) : (
          <div className="table-wrap">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Tên</th>
                  <th>Email</th>
                  <th>Điện thoại</th>
                  <th>Vai trò</th>
                  <th>Trạng thái</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {data.content.map((user) => (
                  <tr key={user.id}>
                    <td>{user.name}</td>
                    <td>{user.email}</td>
                    <td>{user.phoneNumber || "—"}</td>
                    <td>{user.role}</td>
                    <td>{user.enabled ? "Hoạt động" : "Bị khóa"}</td>
                    <td>
                      <button
                        className="button button--outline"
                        onClick={() => toggleEnabled(user)}
                      >
                        {user.enabled ? "Khóa" : "Mở khóa"}
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {!loading && (
          <Pagination
            page={data.number}
            totalPages={data.totalPages}
            onChange={load}
          />
        )}
      </section>
    </>
  );
}

