import { Package } from "lucide-react";
import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { orderApi } from "../../api/api";
import AccountNav from "../../components/account/AccountNav";
import OrderCard from "../../components/account/OrderCard";
import EmptyState from "../../components/common/EmptyState";
import Loader from "../../components/common/Loader";
import Pagination from "../../components/common/Pagination";
import { getErrorMessage } from "../../utils/format";

const initial = { content: [], number: 0, totalPages: 0 };

export default function OrdersPage() {
  const [data, setData] = useState(initial);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const load = (page = 0) => {
    setLoading(true);
    orderApi.mine({ page, size: 8 })
      .then((response) => setData(response.data ?? initial))
      .catch((requestError) => setError(getErrorMessage(requestError)))
      .finally(() => setLoading(false));
  };
  useEffect(() => { load(); }, []);

  return (
    <div className="container account-layout">
      <AccountNav />
      <section className="account-content">
        <div className="page-heading"><div><span className="eyebrow">Lịch sử mua sắm</span><h1>Đơn hàng của tôi</h1><p>Theo dõi trạng thái và xem lại các sản phẩm đã mua.</p></div></div>
        {loading ? <Loader /> : error ? <EmptyState title="Không tải được đơn hàng" description={error} /> : !data.content.length ? (
          <EmptyState icon={Package} title="Bạn chưa có đơn hàng nào" description="Hãy bắt đầu với một sản phẩm thật phù hợp cho thú cưng." action={<Link to="/products" className="button button--primary">Khám phá sản phẩm</Link>} />
        ) : (
          <div className="orders-list">{data.content.map((order) => <OrderCard key={order.id} order={order} />)}<Pagination page={data.number} totalPages={data.totalPages} onChange={load} /></div>
        )}
      </section>
    </div>
  );
}
