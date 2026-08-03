import { BadgeCheck, Banknote, CreditCard, MapPin, PackageCheck } from "lucide-react";
import { useEffect, useMemo, useRef, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import toast from "react-hot-toast";
import { couponApi, orderApi, profileApi } from "../../api/api";
import Loader from "../../components/common/Loader";
import { useCart } from "../../context/CartContext";
import { formatCurrency, getErrorMessage } from "../../utils/format";

const emptyForm = {
  recipientName: "", recipientPhone: "", street: "", ward: "",
  district: "", city: "", country: "Việt Nam", couponCode: "",
  paymentMethod: "COD", note: "",
};
const SHIPPING_FEE = Number(import.meta.env.VITE_SHIPPING_FEE ?? 30_000);
const FREE_SHIPPING_THRESHOLD = Number(import.meta.env.VITE_FREE_SHIPPING_THRESHOLD ?? 500_000);

export default function CheckoutPage() {
  const idempotencyKeyRef = useRef(crypto.randomUUID());
  const { cart, loading: cartLoading, refreshCart } = useCart();
  const [form, setForm] = useState(emptyForm);
  const [coupon, setCoupon] = useState(null);
  const [checkingCoupon, setCheckingCoupon] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const navigate = useNavigate();

  useEffect(() => {
    profileApi.get().then((response) => {
      const profile = response.data;
      const address = profile.address || {};
      setForm((current) => ({
        ...current,
        recipientName: address.recipientName || profile.name || "",
        recipientPhone: address.recipientPhone || profile.phoneNumber || "",
        street: address.street || "", ward: address.ward || "",
        district: address.district || "", city: address.city || "",
        country: address.country || "Việt Nam",
      }));
    });
  }, []);

  const shippingFee = useMemo(
    () => Number(cart.subtotal ?? 0) >= FREE_SHIPPING_THRESHOLD ? 0 : SHIPPING_FEE,
    [cart.subtotal],
  );
  const total = useMemo(
    () => Number(coupon?.total ?? cart.subtotal ?? 0) + shippingFee,
    [coupon, cart.subtotal, shippingFee],
  );

  const applyCoupon = async () => {
    if (!form.couponCode.trim()) return;
    setCheckingCoupon(true);
    try {
      const response = await couponApi.validate(form.couponCode.trim(), cart.subtotal);
      setCoupon(response.data);
      toast.success(`Đã áp dụng mã ${response.data.code}`);
    } catch (error) {
      setCoupon(null);
      toast.error(getErrorMessage(error, "Mã giảm giá không hợp lệ"));
    } finally { setCheckingCoupon(false); }
  };

  const submit = async (event) => {
  event.preventDefault();

  if (submitting) return;
  setSubmitting(true);

  let createdOrder = null;

  try {
    const response = await orderApi.checkout(
      {
        ...form,
        couponCode:
          form.couponCode.trim() || null,
      },
      idempotencyKeyRef.current,
    );

    const checkout = response.data;
      createdOrder = checkout.order;

      idempotencyKeyRef.current =
        crypto.randomUUID();

      await refreshCart();

      if (!checkout.requiresOnlinePayment) {
        toast.success("Đặt hàng thành công");

        navigate(
          `/orders/${createdOrder.id}`,
          { replace: true },
        );

        return;
      }

      try {
        const paymentResponse =
          await orderApi.createVnPayUrl(
            createdOrder.id,
          );

        window.location.assign(
          paymentResponse.data.paymentUrl,
        );
      } catch {
        toast.error(
          "Đơn hàng đã được tạo nhưng chưa mở được VNPay. Hãy thanh toán lại trong chi tiết đơn.",
        );

        navigate(
          `/orders/${createdOrder.id}`,
          { replace: true },
        );
      }
    } catch (error) {
      toast.error(
        getErrorMessage(
          error,
          "Không thể tạo đơn hàng",
        ),
      );

      setSubmitting(false);
    }
  };

  if (cartLoading) return <Loader fullPage />;
  if (!cart.items?.length) return <div className="container section"><div className="empty-state"><PackageCheck /><h2>Không có sản phẩm để thanh toán</h2><Link className="button button--primary" to="/products">Tiếp tục mua sắm</Link></div></div>;

  const input = (name, label, required = true, type = "text") => <label>{label}<input type={type} required={required} value={form[name]} onChange={(e) => setForm({ ...form, [name]: e.target.value })} /></label>;

  return (
    <form className="container checkout-shell" onSubmit={submit}>
      <section className="checkout-form">
        <div className="page-heading"><div><span className="eyebrow">Thanh toán an toàn</span><h1>Hoàn tất đơn hàng</h1></div></div>
        <div className="card checkout-block">
          <h2><MapPin /> Thông tin nhận hàng</h2>
          <div className="form-grid">
            {input("recipientName", "Người nhận")}
            {input("recipientPhone", "Số điện thoại", true, "tel")}
            <span className="form-span-2">{input("street", "Số nhà, tên đường")}</span>
            {input("ward", "Phường / Xã", false)}
            {input("district", "Quận / Huyện")}
            {input("city", "Tỉnh / Thành phố")}
            {input("country", "Quốc gia")}
          </div>
        </div>
        <div className="card checkout-block">
          <h2><CreditCard /> Phương thức thanh toán</h2>
          <div className="payment-options">
            <label className={form.paymentMethod === "COD" ? "is-selected" : ""}><input type="radio" name="payment" value="COD" checked={form.paymentMethod === "COD"} onChange={(e) => setForm({ ...form, paymentMethod: e.target.value })} /><Banknote /><span><strong>Thanh toán khi nhận hàng</strong><small>Trả tiền mặt cho đơn vị vận chuyển</small></span></label>
            <label className={form.paymentMethod === "VNPAY" ? "is-selected" : ""}><input type="radio" name="payment" value="VNPAY" checked={form.paymentMethod === "VNPAY"} onChange={(e) => setForm({ ...form, paymentMethod: e.target.value })} /><CreditCard /><span><strong>Thanh toán qua VNPay</strong><small>Thẻ ATM, Visa, MasterCard hoặc QR</small></span></label>
          </div>
        </div>
        <div className="card checkout-block"><h2>Ghi chú</h2><textarea rows="3" maxLength="1000" value={form.note} onChange={(e) => setForm({ ...form, note: e.target.value })} placeholder="Ví dụ: Giao hàng trong giờ hành chính..." /></div>
      </section>
      <aside className="order-summary card checkout-summary">
        <h2>Đơn hàng ({cart.totalItems})</h2>
        <div className="summary-products">{cart.items.map((item) => <div key={item.id}><span><img src={item.product.imageUrl || "/product-placeholder.svg"} alt="" /><i>{item.quantity}</i></span><p>{item.product.name}</p><strong>{formatCurrency(item.lineTotal)}</strong></div>)}</div>
        <div className="coupon-box"><input value={form.couponCode} onChange={(e) => { setForm({ ...form, couponCode: e.target.value.toUpperCase() }); setCoupon(null); }} placeholder="Mã giảm giá" /><button type="button" onClick={applyCoupon} disabled={checkingCoupon}>{checkingCoupon ? "..." : "Áp dụng"}</button></div>
        {coupon && <p className="coupon-success"><BadgeCheck /> Tiết kiệm {formatCurrency(coupon.discount)}</p>}
        <dl><dt>Tạm tính</dt><dd>{formatCurrency(cart.subtotal)}</dd><dt>Phí vận chuyển</dt><dd>{shippingFee === 0 ? "Miễn phí" : formatCurrency(shippingFee)}</dd>{coupon && <><dt>Giảm giá</dt><dd className="text-success">−{formatCurrency(coupon.discount)}</dd></>}</dl>
        <div className="order-summary__total"><span>Tổng thanh toán</span><strong>{formatCurrency(total)}</strong></div>
        <button className="button button--primary button--full" disabled={submitting}>{submitting ? "Đang xử lý..." : form.paymentMethod === "VNPAY" ? "Thanh toán với VNPay" : "Đặt hàng COD"}</button>
        <small className="checkout-terms">Bằng việc đặt hàng, bạn đồng ý với điều khoản của PetHub.</small>
      </aside>
    </form>
  );
}
