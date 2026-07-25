import { Eye, Search, X } from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import toast from "react-hot-toast";
import { adminApi } from "../../api/api";
import AdminPageHeader from "../../components/admin/AdminPageHeader";
import Loader from "../../components/common/Loader";
import Pagination from "../../components/common/Pagination";
import StatusBadge from "../../components/common/StatusBadge";
import { formatCurrency, formatDateTime, getErrorMessage } from "../../utils/format";

const statuses = [
  "PENDING_PAYMENT",
  "PENDING",
  "CONFIRMED",
  "PROCESSING",
  "SHIPPING",
  "DELIVERED",
  "CANCELLED",
  "RETURNED",
];

const statusLabels = {
  PENDING_PAYMENT: "Chờ thanh toán",
  PENDING: "Chờ xác nhận",
  CONFIRMED: "Đã xác nhận",
  PROCESSING: "Đang chuẩn bị",
  SHIPPING: "Đang giao",
  DELIVERED: "Đã giao",
  CANCELLED: "Đã hủy",
  RETURNED: "Đã hoàn",
};

const adminTransitions = {
  PENDING_PAYMENT: ["CANCELLED"],
  PENDING: ["CONFIRMED", "CANCELLED"],
  CONFIRMED: ["PROCESSING", "CANCELLED"],
  PROCESSING: ["SHIPPING", "CANCELLED"],
  SHIPPING: ["DELIVERED"],
  DELIVERED: ["RETURNED"],
};

function getAllowedStatuses(order) {
  const nextStatuses = adminTransitions[order.status] ?? [];
  const allowedNextStatuses = nextStatuses.filter((status) => {
    if (status === "CANCELLED" && order.paymentStatus === "PAID") {
      return false;
    }
    if (
      status === "RETURNED"
      && order.paymentMethod === "VNPAY"
      && order.paymentStatus === "PAID"
    ) {
      return false;
    }
    return true;
  });

  return [order.status, ...allowedNextStatuses];
}

export default function AdminOrdersPage() {
  const [data, setData] = useState({
    content: [],
    number: 0,
    totalPages: 0,
    totalElements: 0,
  });
  const [loading, setLoading] = useState(true);
  const [selected, setSelected] = useState(null);
  const [statusFilter, setStatusFilter] = useState("");
  const [searchTerm, setSearchTerm] = useState("");
  const [statusForm, setStatusForm] = useState({
    status: "",
    adminMessage: "",
  });
  const [saving, setSaving] = useState(false);

  const load = (page = 0) => {
    setLoading(true);
    adminApi
      .orders({ page, size: 10 })
      .then((response) => setData(response.data))
      .catch((error) => toast.error(getErrorMessage(error)))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    load();
  }, []);

  const visible = useMemo(() => {
    const keyword = searchTerm.trim().toLocaleLowerCase("vi");
    return data.content.filter((order) => {
      const matchesStatus = !statusFilter || order.status === statusFilter;
      const matchesSearch =
        !keyword
        || order.orderCode?.toLocaleLowerCase("vi").includes(keyword)
        || order.customerName?.toLocaleLowerCase("vi").includes(keyword)
        || order.recipientName?.toLocaleLowerCase("vi").includes(keyword)
        || order.recipientPhone?.includes(keyword);
      return matchesStatus && matchesSearch;
    });
  }, [data.content, searchTerm, statusFilter]);

  const open = (order) => {
    setSelected(order);
    setStatusForm({
      status: order.status,
      adminMessage: order.adminMessage || "",
    });
  };

  const saveStatus = async (event) => {
    event.preventDefault();
    if (statusForm.status === selected.status) {
      toast.error("Hãy chọn trạng thái xử lý tiếp theo");
      return;
    }

    setSaving(true);
    try {
      const response = await adminApi.updateOrderStatus(selected.id, statusForm);
      const updatedOrder = response.data;
      setSelected(updatedOrder);
      setStatusForm({
        status: updatedOrder.status,
        adminMessage: updatedOrder.adminMessage || "",
      });
      setData((current) => ({
        ...current,
        content: current.content.map((order) =>
          order.id === updatedOrder.id ? updatedOrder : order
        ),
      }));
      toast.success("Đã cập nhật trạng thái đơn hàng");
    } catch (error) {
      toast.error(getErrorMessage(error));
    } finally {
      setSaving(false);
    }
  };

  const allowedStatuses = selected ? getAllowedStatuses(selected) : [];
  const isPaidVnPay =
    selected?.paymentMethod === "VNPAY"
    && selected?.paymentStatus === "PAID";
  const hasNextStatus = allowedStatuses.length > 1;

  return (
    <>
      <AdminPageHeader
        title="Quản lý đơn hàng"
        description={`${data.totalElements || 0} đơn hàng trên hệ thống`}
      />

      <section className="admin-card">
        <div className="table-toolbar">
          <span className="admin-search">
            <Search />
            <input
              value={searchTerm}
              onChange={(event) => setSearchTerm(event.target.value)}
              placeholder="Tìm mã đơn, khách hàng, số điện thoại..."
            />
          </span>
          <select
            value={statusFilter}
            onChange={(event) => setStatusFilter(event.target.value)}
          >
            <option value="">Tất cả trạng thái</option>
            {statuses.map((status) => (
              <option key={status} value={status}>
                {statusLabels[status]}
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
                  <th>Khách hàng</th>
                  <th>Ngày đặt</th>
                  <th>Thanh toán</th>
                  <th>Tổng tiền</th>
                  <th>Trạng thái</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {visible.map((order) => (
                  <tr key={order.id}>
                    <td><strong>#{order.orderCode}</strong></td>
                    <td>{order.customerName || `User #${order.userId}`}</td>
                    <td>{formatDateTime(order.createdAt)}</td>
                    <td>
                      <span>{order.paymentMethod}</span><br />
                      <StatusBadge status={order.paymentStatus} />
                    </td>
                    <td><strong>{formatCurrency(order.total)}</strong></td>
                    <td><StatusBadge status={order.status} /></td>
                    <td>
                      <button
                        className="icon-button"
                        onClick={() => open(order)}
                        aria-label={`Xem đơn ${order.orderCode}`}
                      >
                        <Eye />
                      </button>
                    </td>
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

      {selected && (
        <>
          <aside className="detail-drawer">
            <header>
              <div>
                <span className="eyebrow">Chi tiết đơn hàng</span>
                <h2>#{selected.orderCode}</h2>
              </div>
              <button
                className="icon-button"
                onClick={() => setSelected(null)}
                aria-label="Đóng"
              >
                <X />
              </button>
            </header>

            <div className="detail-drawer__body">
              <div className="drawer-info">
                <span>
                  <small>Khách hàng</small>
                  <strong>{selected.customerName}</strong>
                </span>
                <span>
                  <small>Ngày đặt</small>
                  <strong>{formatDateTime(selected.createdAt)}</strong>
                </span>
                <span>
                  <small>Người nhận</small>
                  <strong>
                    {selected.recipientName} · {selected.recipientPhone}
                  </strong>
                </span>
                <span>
                  <small>Địa chỉ</small>
                  <strong>{selected.shippingAddress}</strong>
                </span>
              </div>

              <h3>Sản phẩm</h3>
              <div className="drawer-products">
                {selected.items.map((item) => (
                  <div key={item.id}>
                    <img
                      src={item.productImageUrl || "/product-placeholder.svg"}
                      alt=""
                    />
                    <span>
                      <strong>{item.productName}</strong>
                      <small>
                        {item.quantity} × {formatCurrency(item.unitPrice)}
                      </small>
                    </span>
                    <b>{formatCurrency(item.lineTotal)}</b>
                  </div>
                ))}
              </div>

              <div className="drawer-total">
                <span>Tổng thanh toán</span>
                <strong>{formatCurrency(selected.total)}</strong>
              </div>

              {isPaidVnPay && (
                <div className="notice">
                  <strong>Giao dịch VNPay đã thu tiền.</strong>{" "}
                  Không thể hủy hoặc đánh dấu hoàn trực tiếp. Cần có quy trình
                  hoàn tiền VNPay được xác nhận trước.
                </div>
              )}

              <form className="status-form" onSubmit={saveStatus}>
                <h3>Cập nhật xử lý</h3>
                <label>
                  Trạng thái
                  <select
                    value={statusForm.status}
                    onChange={(event) =>
                      setStatusForm({
                        ...statusForm,
                        status: event.target.value,
                      })
                    }
                  >
                    {allowedStatuses.map((status) => (
                      <option key={status} value={status}>
                        {statusLabels[status]}
                      </option>
                    ))}
                  </select>
                </label>
                <label>
                  Lời nhắn cho khách
                  <textarea
                    rows="3"
                    value={statusForm.adminMessage}
                    onChange={(event) =>
                      setStatusForm({
                        ...statusForm,
                        adminMessage: event.target.value,
                      })
                    }
                    placeholder="Ví dụ: Đơn hàng đã bàn giao cho đơn vị vận chuyển..."
                  />
                </label>
                <button
                  className="button button--primary button--full"
                  disabled={
                    saving
                    || !hasNextStatus
                    || statusForm.status === selected.status
                  }
                >
                  {saving ? "Đang lưu..." : "Cập nhật đơn hàng"}
                </button>
              </form>
            </div>
          </aside>
          <button
            className="drawer-backdrop drawer-backdrop--admin"
            onClick={() => setSelected(null)}
            aria-label="Đóng"
          />
        </>
      )}
    </>
  );
}