import { ArrowLeft, CreditCard, MapPin, PackageCheck, Phone, RotateCcw } from "lucide-react";
import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import toast from "react-hot-toast";
import { orderApi, refundApi } from "../../api/api";
import Loader from "../../components/common/Loader";
import EmptyState from "../../components/common/EmptyState";
import StatusBadge from "../../components/common/StatusBadge";
import { formatCurrency, formatDateTime, getErrorMessage } from "../../utils/format";
import { REFUND_ENABLED } from "../../config/features";

export default function OrderDetailPage() {
  const { orderId } = useParams();
  const [order, setOrder] = useState(null);
  const [loading, setLoading] = useState(true);
  const [processing, setProcessing] = useState(false);
  const [refundReason, setRefundReason] = useState("");
  const [showRefundForm, setShowRefundForm] = useState(false);

  useEffect(() => {
    setLoading(true);
    orderApi.get(orderId)
      .then((response) => setOrder(response.data))
      .catch(() => setOrder(null))
      .finally(() => setLoading(false));
  }, [orderId]);

  const cancelOrder = async () => {
    if (!window.confirm("Bạn chắc chắn muốn hủy đơn hàng này?")) return;
    setProcessing(true);
    try {
      const response = await orderApi.cancel(order.id);
      setOrder(response.data);
      toast.success("Đã hủy đơn hàng");
    } catch (error) { toast.error(getErrorMessage(error)); }
    finally { setProcessing(false); }
  };

  const payAgain = async () => {
    setProcessing(true);
    try {
      const response = await orderApi.createVnPayUrl(order.id);
      window.location.assign(response.data.paymentUrl);
    } catch (error) {
      toast.error(getErrorMessage(error, "Không thể mở cổng thanh toán"));
      setProcessing(false);
    }
  };

  if (loading) return <Loader fullPage />;
  if (!order) return <div className="container section"><EmptyState title="Không tìm thấy đơn hàng" action={<Link to="/orders" className="button button--primary">Danh sách đơn hàng</Link>} /></div>;

  const canPay =
    order.paymentMethod === "VNPAY"
    && order.status === "PENDING_PAYMENT"
    && order.paymentStatus === "PENDING";
  const canCancel =
    order.status === "PENDING"
    && order.paymentStatus !== "PAID";
  const canRequestRefund =
    REFUND_ENABLED
    && order.status === "DELIVERED"
    && order.paymentStatus === "PAID"
    && !order.refundStatus;

  const requestRefund = async (event) => {
    event.preventDefault();

    if (!refundReason.trim()) {
      toast.error("Vui lòng nhập lý do hoàn hàng");
      return;
    }

    setProcessing(true);

    try {
      const response = await refundApi.request(order.id, refundReason.trim());
      setOrder((current) => ({
        ...current,
        refundStatus: response.data.status,
      }));
      toast.success("Đã gửi yêu cầu hoàn hàng");
      setShowRefundForm(false);
      setRefundReason("");
    } catch (error) {
      toast.error(
        getErrorMessage(
          error,
          "Không thể gửi yêu cầu hoàn hàng"
        )
      );
    } finally {
      setProcessing(false);
    }
  };

  return (
    <div className="container order-detail-page">
      <Link to="/orders" className="back-link"><ArrowLeft /> Đơn hàng của tôi</Link>
      <div className="order-detail__heading">
        <div><span className="eyebrow">Chi tiết đơn hàng</span><h1>#{order.orderCode}</h1><p>Đặt lúc {formatDateTime(order.createdAt)}</p></div>
        <div><StatusBadge status={order.status} /><StatusBadge status={order.paymentStatus} /></div>
      </div>
      <section className="order-progress">
        {["PENDING", "CONFIRMED", "PROCESSING", "SHIPPING", "DELIVERED"].map((status, index) => {
          const current = ["PENDING_PAYMENT", "PENDING", "CONFIRMED", "PROCESSING", "SHIPPING", "DELIVERED"].indexOf(order.status);
          const step = ["PENDING", "CONFIRMED", "PROCESSING", "SHIPPING", "DELIVERED"].indexOf(status);
          return <div key={status} className={current >= step + 1 ? "is-done" : ""}><span>{current > step + 1 ? "✓" : index + 1}</span><small>{{ PENDING: "Đã đặt", CONFIRMED: "Xác nhận", PROCESSING: "Chuẩn bị", SHIPPING: "Vận chuyển", DELIVERED: "Đã giao" }[status]}</small></div>;
        })}
      </section>
      {order.adminMessage && <div className="notice"><strong>Cập nhật từ cửa hàng:</strong> {order.adminMessage}</div>}
      {order.paymentMethod === "VNPAY"
        && order.paymentStatus === "PAID" && (
          <div className="notice">
            <strong>VNPay đã xác nhận thanh toán.</strong>{" "}
            Đơn đã thanh toán không thể hủy trực tiếp.
            Vui lòng liên hệ cửa hàng nếu cần hỗ trợ đổi trả.
          </div>
      )}
      <div className="order-detail__grid">
        <section className="card order-products">
          <h2>Sản phẩm</h2>
          {order.items.map((item) => <div key={item.id}><img src={item.productImageUrl || "/product-placeholder.svg"} alt={item.productName} /><div><Link to={`/products/${item.productId}`}>{item.productName}</Link><small>SKU: {item.productSku || "—"} · Số lượng: {item.quantity}</small></div><strong>{formatCurrency(item.lineTotal)}</strong></div>)}
        </section>
        <aside>
          <div className="card order-info"><h2><MapPin /> Thông tin giao hàng</h2><strong>{order.recipientName}</strong><span><Phone /> {order.recipientPhone}</span><p>{order.shippingAddress}</p></div>
          <div className="card order-info"><h2><CreditCard /> Thanh toán</h2><p>{order.paymentMethod === "VNPAY" ? "VNPay" : "Thanh toán khi nhận hàng (COD)"}</p><StatusBadge status={order.paymentStatus} /></div>
        </aside>
      </div>
      <div className="card order-totals"><dl><dt>Tạm tính</dt><dd>{formatCurrency(order.subtotal)}</dd><dt>Phí giao hàng</dt><dd>{formatCurrency(order.shippingFee)}</dd><dt>Giảm giá {order.couponCode && `(${order.couponCode})`}</dt><dd>−{formatCurrency(order.discountAmount)}</dd><dt>Tổng thanh toán</dt><dd>{formatCurrency(order.total)}</dd></dl></div>
      <div className="order-actions">
        {canPay && <button className="button button--primary" onClick={payAgain} disabled={processing}><CreditCard /> Thanh toán ngay</button>}
        {canCancel && <button className="button button--outline button--danger" onClick={cancelOrder} disabled={processing}><RotateCcw /> Hủy đơn hàng</button>}
        {canRequestRefund && (
          <button
            className="button button--outline"
            onClick={() => setShowRefundForm(true)}
            disabled={processing}
          >
            Yêu cầu hoàn hàng
          </button>
        )}
        {REFUND_ENABLED && order.refundStatus && (
          <StatusBadge status={order.refundStatus} />
        )}
        {order.status === "DELIVERED" && <Link className="button button--primary" to={`/products/${order.items[0]?.productId}#reviews`}><PackageCheck /> Đánh giá sản phẩm</Link>}
      </div>
      {showRefundForm && (
        <section className="card" style={{ marginTop: 16 }}>
          <h2>Yêu cầu hoàn hàng</h2>
          <form onSubmit={requestRefund}>
            <label>
              Lý do hoàn hàng
              <textarea
                rows="4"
                value={refundReason}
                onChange={(event) => setRefundReason(event.target.value)}
                placeholder="Ví dụ: Sản phẩm không đúng mô tả..."
                disabled={processing}
              />
            </label>
            <div className="order-actions">
              <button className="button button--primary" disabled={processing}>
                {processing ? "Đang gửi..." : "Gửi yêu cầu"}
              </button>
              <button
                type="button"
                className="button button--outline"
                onClick={() => setShowRefundForm(false)}
                disabled={processing}
              >
                Hủy
              </button>
            </div>
          </form>
        </section>
      )}
    </div>
  );
}
