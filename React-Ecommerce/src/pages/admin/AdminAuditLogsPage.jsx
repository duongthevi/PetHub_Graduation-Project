import { useCallback, useEffect, useState } from "react";
import toast from "react-hot-toast";
import { adminAuditApi } from "../../api/api";
import AdminPageHeader from "../../components/admin/AdminPageHeader";
import Loader from "../../components/common/Loader";
import Pagination from "../../components/common/Pagination";
import { formatDateTime, getErrorMessage } from "../../utils/format";

export default function AdminAuditLogsPage() {
  const [data, setData] = useState({
    content: [],
    number: 0,
    totalPages: 0,
    totalElements: 0,
  });
  const [loading, setLoading] = useState(true);

  const load = useCallback((page = 0) => {
    setLoading(true);

    adminAuditApi
      .list({ page, size: 20 })
      .then((response) => {
        setData(response.data);
      })
      .catch((error) => {
        toast.error(getErrorMessage(error, "Không thể tải nhật ký quản trị"));
      })
      .finally(() => {
        setLoading(false);
      });
  }, []);

  useEffect(() => {
    load(0);
  }, [load]);

  return (
    <>
      <AdminPageHeader
        title="Nhật ký quản trị"
        description={`${data.totalElements || 0} bản ghi hành động quản trị`}
      />

      <section className="admin-card">
        {loading ? (
          <Loader />
        ) : (
          <div className="table-wrap">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Thời gian</th>
                  <th>Người dùng</th>
                  <th>Email</th>
                  <th>Method</th>
                  <th>Path</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                {data.content.map((log) => (
                  <tr key={log.id}>
                    <td>{formatDateTime(log.createdAt)}</td>
                    <td>{log.actorUserId || "—"}</td>
                    <td>{log.actorEmail || "—"}</td>
                    <td>{log.httpMethod || "—"}</td>
                    <td>{log.requestPath || "—"}</td>
                    <td>{log.responseStatus || "—"}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        <Pagination
          page={data.number}
          totalPages={data.totalPages}
          onChange={load}
        />
      </section>
    </>
  );
}
