import { useCallback, useEffect, useState } from "react";
import toast from "react-hot-toast";
import { adminInventoryApi } from "../../api/api";
import AdminPageHeader from "../../components/admin/AdminPageHeader";
import Loader from "../../components/common/Loader";
import Pagination from "../../components/common/Pagination";
import StatusBadge from "../../components/common/StatusBadge";
import { formatDateTime, getErrorMessage } from "../../utils/format";

export default function AdminInventoryPage() {
  const [data, setData] = useState({
    content: [],
    number: 0,
    totalPages: 0,
    totalElements: 0,
  });
  const [loading, setLoading] = useState(true);

  const load = useCallback((page = 0) => {
    setLoading(true);

    adminInventoryApi
      .list({ page, size: 20 })
      .then((response) => {
        setData(response.data);
      })
      .catch((error) => {
        toast.error(getErrorMessage(error, "Không thể tải lịch sử tồn kho"));
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
        title="Lịch sử tồn kho"
        description={`${data.totalElements || 0} bản ghi giao dịch tồn kho`}
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
                  <th>Sản phẩm</th>
                  <th>Mã đơn / Ref</th>
                  <th>Loại giao dịch</th>
                  <th>Thay đổi</th>
                  <th>Tồn sau</th>
                  <th>Người tạo</th>
                </tr>
              </thead>
              <tbody>
                {data.content.map((tx) => (
                  <tr key={tx.id}>
                    <td>{formatDateTime(tx.createdAt)}</td>
                    <td>{tx.productName || `#${tx.productId || "-"}`}</td>
                    <td>{tx.orderCode || tx.referenceNumber || "—"}</td>
                    <td><StatusBadge status={tx.transactionType} /></td>
                    <td>{tx.quantityChanged > 0 ? `+${tx.quantityChanged}` : tx.quantityChanged}</td>
                    <td>{tx.balanceAfter ?? "—"}</td>
                    <td>{tx.createdByName || "System"}</td>
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
