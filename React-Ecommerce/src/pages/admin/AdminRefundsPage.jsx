import { Check, Play, Search, X } from "lucide-react";
import { useCallback, useEffect, useState } from "react";
import toast from "react-hot-toast";
import { adminRefundApi } from "../../api/api";
import AdminPageHeader from "../../components/admin/AdminPageHeader";
import Loader from "../../components/common/Loader";
import Pagination from "../../components/common/Pagination";
import StatusBadge from "../../components/common/StatusBadge";
import { formatCurrency, formatDateTime, getErrorMessage } from "../../utils/format";

const statusOptions = ["REQUESTED", "APPROVED", "PROCESSING", "COMPLETED", "REJECTED"];

export default function AdminRefundsPage() {
  const [data, setData] = useState({
    content: [],
    number: 0,
    totalPages: 0,
    totalElements: 0,
  });
  const [loading, setLoading] = useState(true);
  const [savingId, setSavingId] = useState(null);
  const [status, setStatus] = useState("");
  const [query, setQuery] = useState("");

  const load = useCallback(
    (page = 0) => {
      setLoading(true);

      adminRefundApi
        .list({
          page,
          size: 10,
          status: status || undefined,
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
    [status, query]
  );

  useEffect(() => {
    const timeoutId = window.setTimeout(() => {
      load(0);
    }, 250);

    return () => {
      window.clearTimeout(timeoutId);
    };
  }, [load]);

  const askForNote = (title) =>
    window.prompt(`${title}\n\nNhập ghi chú (có thể để trống):`, "") || "";

  const approve = async (refund) => {
    const adminNote = askForNote(`Duyệt yêu cầu #${refund.id}`);
    setSavingId(refund.id);
    try {
      await adminRefundApi.approve(refund.id, adminNote);
      toast.success("Đã duyệt yêu cầu hoàn tiền");
      load(data.number);
    } catch (error) {
      toast.error(getErrorMessage(error, "Không thể duyệt yêu cầu"));
    } finally {
      setSavingId(null);
    }
  };

  const reject = async (refund) => {
    const adminNote = askForNote(`Từ chối yêu cầu #${refund.id}`);
    setSavingId(refund.id);
    try {
      await adminRefundApi.reject(refund.id, adminNote);
      toast.success("Đã từ chối yêu cầu hoàn tiền");
      load(data.number);
    } catch (error) {
      toast.error(getErrorMessage(error, "Không thể từ chối yêu cầu"));
    } finally {
      setSavingId(null);
    }
  };

  const processRefund = async (refund) => {
    if (!window.confirm(`Bắt đầu xử lý hoàn tiền cho đơn #${refund.orderCode}?`)) {
      return;
    }

    setSavingId(refund.id);
    try {
      await adminRefundApi.process(refund.id);
      toast.success("Đã cập nhật kết quả hoàn tiền");
      load(data.number);
    } catch (error) {
      toast.error(getErrorMessage(error, "Không thể xử lý hoàn tiền"));
    } finally {
      setSavingId(null);
    }
  };

  return (
    <>
      <AdminPageHeader
        title="Quản lý hoàn tiền"
        description={`${data.totalElements || 0} yêu cầu hoàn tiền`}
      />

      <section className="admin-card">
        <div className="table-toolbar">
          <span className="admin-search">
            <Search />
            <input
              value={query}
              onChange={(event) => setQuery(event.target.value)}
              placeholder="Tìm theo mã đơn, khách hàng, lý do..."
            />
          </span>
          <select
            value={status}
            onChange={(event) => setStatus(event.target.value)}
          >
            <option value="">Tất cả trạng thái</option>
            {statusOptions.map((item) => (
              <option key={item} value={item}>
                {item}
              </option>
            ))}
          </select>
        </div>

        {loading ? (
          <Loader />
        ) : (
          <div className="table-wrap">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Mã đơn</th>
                  <th>Khách yêu cầu</th>
                  <th>Số tiền</th>
                  <th>Lý do</th>
                  <th>Trạng thái</th>
                  <th>Provider reference</th>
                  <th>Ngày yêu cầu</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {data.content.map((refund) => {
                  const busy = savingId === refund.id;
                  const canApprove = refund.status === "REQUESTED";
                  const canReject = refund.status === "REQUESTED" || refund.status === "APPROVED";
                  const canProcess = refund.status === "APPROVED";

                  return (
                    <tr key={refund.id}>
                      <td><strong>#{refund.orderCode}</strong></td>
                      <td>{refund.requestedByName || `User #${refund.requestedById}`}</td>
                      <td><strong>{formatCurrency(refund.amount)}</strong></td>
                      <td style={{ maxWidth: 280 }}>{refund.reason}</td>
                      <td><StatusBadge status={refund.status} /></td>
                      <td>{refund.providerRefundRef || "—"}</td>
                      <td>{formatDateTime(refund.createdAt)}</td>
                      <td>
                        <div style={{ display: "flex", gap: 8 }}>
                          {canApprove && (
                            <button
                              className="icon-button"
                              onClick={() => approve(refund)}
                              disabled={busy}
                              aria-label={`Duyệt refund ${refund.id}`}
                            >
                              <Check />
                            </button>
                          )}
                          {canReject && (
                            <button
                              className="icon-button"
                              onClick={() => reject(refund)}
                              disabled={busy}
                              aria-label={`Từ chối refund ${refund.id}`}
                            >
                              <X />
                            </button>
                          )}
                          {canProcess && (
                            <button
                              className="icon-button"
                              onClick={() => processRefund(refund)}
                              disabled={busy}
                              aria-label={`Xử lý refund ${refund.id}`}
                            >
                              <Play />
                            </button>
                          )}
                        </div>
                      </td>
                    </tr>
                  );
                })}
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
