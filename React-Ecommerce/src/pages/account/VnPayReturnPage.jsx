import { CheckCircle2, LoaderCircle, XCircle } from "lucide-react";
import { useEffect, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { orderApi } from "../../api/api";
import { getErrorMessage } from "../../utils/format";

export default function VnPayReturnPage() {
  const [params] = useSearchParams();
  const [result, setResult] = useState(null);

  useEffect(() => {
    const query = Object.fromEntries(params.entries());
    orderApi.verifyVnPayReturn(query)
      .then((response) => setResult(response.data))
      .catch((error) => setResult({ successful: false, message: getErrorMessage(error, "Không thể xác minh giao dịch") }));
  }, [params]);

  if (!result) return <div className="payment-result"><LoaderCircle className="spin" /><h1>Đang xác minh thanh toán</h1><p>Vui lòng không đóng trang này.</p></div>;
  if (result.pending) {
    return (
      <div className="payment-result">
        <LoaderCircle className="spin" />
        <span className="eyebrow">Kết quả VNPay</span>
        <h1>Đang chờ VNPay xác nhận</h1>
        <p>{result.message}</p>

        {result.orderCode && (
          <strong>Mã đơn hàng: #{result.orderCode}</strong>
        )}

        <div>
          <Link
            className="button button--primary"
            to={result.orderId ? `/orders/${result.orderId}` : "/orders"}
          >
            Xem đơn hàng
          </Link>
        </div>
      </div>
    );
  }
  return (
    <div className={`payment-result ${result.successful ? "is-success" : "is-failed"}`}>
      {result.successful ? <CheckCircle2 /> : <XCircle />}
      <span className="eyebrow">Kết quả VNPay</span>
      <h1>{result.successful ? "Thanh toán thành công" : "Thanh toán chưa hoàn tất"}</h1>
      <p>{result.message}</p>
      {result.orderCode && <strong>Mã đơn hàng: #{result.orderCode}</strong>}
      <div><Link className="button button--primary" to="/orders">Xem đơn hàng</Link><Link className="button button--outline" to="/">Về trang chủ</Link></div>
    </div>
  );
}
