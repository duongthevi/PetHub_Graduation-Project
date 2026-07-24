import { ArrowRight, Clock3 } from "lucide-react";
import { Link } from "react-router-dom";
import StatusBadge from "../common/StatusBadge";
import { formatCurrency, formatDateTime } from "../../utils/format";

export default function OrderCard({ order }) {
  return (
    <article className="order-card">
      <header>
        <div><strong>#{order.orderCode}</strong><span><Clock3 /> {formatDateTime(order.createdAt)}</span></div>
        <div><StatusBadge status={order.status} /><StatusBadge status={order.paymentStatus} /></div>
      </header>
      <div className="order-card__items">
        {order.items?.slice(0, 2).map((item) => (
          <div key={item.id}>
            <img src={item.productImageUrl || "/product-placeholder.svg"} alt={item.productName} />
            <span><strong>{item.productName}</strong><small>{item.quantity} × {formatCurrency(item.unitPrice)}</small></span>
          </div>
        ))}
        {order.items?.length > 2 && <span className="order-card__more">+{order.items.length - 2} sản phẩm khác</span>}
      </div>
      <footer>
        <span>Tổng thanh toán <strong>{formatCurrency(order.total)}</strong></span>
        <Link to={`/orders/${order.id}`} className="button button--outline button--small">Chi tiết <ArrowRight /></Link>
      </footer>
    </article>
  );
}
