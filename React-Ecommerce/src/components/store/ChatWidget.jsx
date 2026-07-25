import { useRef, useState } from "react";
import {
  Bot,
  CreditCard,
  ExternalLink,
  MessageCircle,
  Minus,
  Plus,
  Send,
  ShoppingCart,
  X,
} from "lucide-react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import toast from "react-hot-toast";
import { ragApi } from "../../api/api";
import { useAuth } from "../../context/AuthContext";
import { useCart } from "../../context/CartContext";
import { formatCurrency, getErrorMessage } from "../../utils/format";

const welcome = {
  role: "assistant",
  content: "Chào bạn! Mình là trợ lý PetHub. Bạn cần tư vấn sản phẩm hay cách chăm sóc thú cưng?",
};

export default function ChatWidget() {
  const [open, setOpen] = useState(false);
  const [messages, setMessages] = useState([welcome]);
  const [question, setQuestion] = useState("");
  const [sending, setSending] = useState(false);
  const [quantities, setQuantities] = useState({});
  const [pendingAction, setPendingAction] = useState(null);

  const { isAuthenticated } = useAuth();
  const { addItem, cartCount } = useCart();
  const navigate = useNavigate();
  const location = useLocation();
  const conversationId = useRef(null);

  const send = async (event) => {
    event.preventDefault();
    if (!question.trim() || sending) return;
    const content = question.trim();
    setQuestion("");
    setMessages((current) => [...current, { role: "user", content }]);
    setSending(true);
    try {
      const response = await ragApi.chat({ conversationId: conversationId.current, question: content });
      const data = response.data;
      conversationId.current = data.conversationId;
      setMessages((current) => [...current, {
        role: "assistant",
        content: data.answer,
        citations: data.citations,
        products: data.recommendedProducts?.length
          ? data.recommendedProducts
          : (data.recommendedProductIds ?? []).map((id) => ({ id })),
      }]);
    } catch (error) {
      setMessages((current) => [...current, {
        role: "assistant",
        error: true,
        content: getErrorMessage(error, "Trợ lý đang bận. Bạn vui lòng thử lại sau."),
      }]);
    } finally {
      setSending(false);
    }
  };

  const quantityFor = (product) => quantities[product.id] ?? 1;

const changeQuantity = (product, difference) => {
  const maximum = Math.max(1, Math.min(Number(product.stock ?? 99), 99));

  setQuantities((current) => ({
    ...current,
    [product.id]: Math.min(
      maximum,
      Math.max(1, quantityFor(product) + difference),
    ),
  }));
};

const requireLogin = () => {
  if (isAuthenticated) return true;

  toast.error("Vui lòng đăng nhập để thêm sản phẩm vào giỏ");
  setOpen(false);
  navigate("/login", {
    state: {
      from: location,
    },
  });
  return false;
};

const addProduct = async (product, goToCheckout = false) => {
  if (!requireLogin()) return;

  if (Number(product.stock ?? 1) <= 0) {
    toast.error("Sản phẩm hiện đã hết hàng");
    return;
  }

  const actionKey = `${product.id}:${goToCheckout ? "checkout" : "cart"}`;
  setPendingAction(actionKey);

  try {
    const quantity = quantityFor(product);
    await addItem(product.id, quantity);

    toast.success(`Đã thêm ${quantity} sản phẩm vào giỏ`);

    if (goToCheckout) {
      setOpen(false);
      navigate("/checkout");
    }
  } catch (error) {
    toast.error(
      getErrorMessage(error, "Không thể thêm sản phẩm vào giỏ"),
    );
  } finally {
    setPendingAction(null);
  }
};

const goToCheckout = () => {
  if (!requireLogin()) return;

  if (cartCount <= 0) {
    toast.error("Giỏ hàng chưa có sản phẩm");
    return;
  }

  setOpen(false);
  navigate("/checkout");
};

  return (
    <div className={`chat-widget ${open ? "is-open" : ""}`}>
      {open && (
        <section className="chat-panel">
          <header>
            <span><Bot /><span><strong>PetHub AI</strong><small>Trợ lý tư vấn sản phẩm</small></span></span>
            <button onClick={() => setOpen(false)} aria-label="Đóng"><X /></button>
          </header>
          <div className="chat-messages">
            {messages.map((message, index) => (
              <div
                key={`${message.role}-${index}`}
                className={`chat-message chat-message--${message.role} ${
                  message.error ? "is-error" : ""
                } ${message.products?.length ? "has-products" : ""}`}
              >
                <p>{message.content}</p>
                {message.products?.length > 0 && (
                  <div className="chat-products">
                    {message.products.slice(0, 3).map((product) => (
                      <article className="chat-product" key={product.id}>
                        <img
                          src={product.imageUrl || "/product-placeholder.svg"}
                          alt={product.name || `Sản phẩm #${product.id}`}
                        />

                        <div className="chat-product__content">
                          <Link
                            to={`/products/${product.id}`}
                            onClick={() => setOpen(false)}
                          >
                            {product.name || `Sản phẩm #${product.id}`}
                            <ExternalLink size={12} />
                          </Link>

                          {product.price != null && (
                            <strong>{formatCurrency(product.price)}</strong>
                          )}

                          {product.stock != null && (
                            <small>
                              {product.stock > 0
                                ? `Còn ${product.stock} sản phẩm`
                                : "Đã hết hàng"}
                            </small>
                          )}

                          <div className="chat-product__quantity">
                            <button
                              type="button"
                              onClick={() => changeQuantity(product, -1)}
                              disabled={quantityFor(product) <= 1}
                            >
                              <Minus />
                            </button>

                            <b>{quantityFor(product)}</b>

                            <button
                              type="button"
                              onClick={() => changeQuantity(product, 1)}
                              disabled={
                                quantityFor(product) >= Number(product.stock ?? 99)
                              }
                            >
                              <Plus />
                            </button>
                          </div>

                          <div className="chat-product__actions">
                            <button
                              type="button"
                              onClick={() => addProduct(product)}
                              disabled={
                                pendingAction !== null ||
                                Number(product.stock ?? 1) <= 0
                              }
                            >
                              <ShoppingCart />
                              Thêm vào giỏ
                            </button>

                            <button
                              type="button"
                              onClick={() => addProduct(product, true)}
                              disabled={
                                pendingAction !== null ||
                                Number(product.stock ?? 1) <= 0
                              }
                            >
                              <CreditCard />
                              {pendingAction === `${product.id}:checkout`
                                ? "Đang xử lý..."
                                : "Thêm và thanh toán"}
                            </button>
                          </div>
                        </div>
                      </article>
                    ))}
                  </div>
                )}
                {message.citations?.length > 0 && <small>Nguồn: {message.citations.map((item) => item.title).join(", ")}</small>}
              </div>
            ))}
            {sending && <div className="chat-message chat-message--assistant chat-typing"><i /><i /><i /></div>}
          </div>

          {cartCount > 0 && (
            <div className="chat-cart-summary">
              <span>
                <ShoppingCart />
                Giỏ hàng có <strong>{cartCount}</strong> sản phẩm
              </span>

              <button type="button" onClick={goToCheckout}>
                Thanh toán
              </button>
            </div>
          )}

          <form onSubmit={send}>
            <input value={question} onChange={(e) => setQuestion(e.target.value)} placeholder="Nhập câu hỏi của bạn..." />
            <button disabled={!question.trim() || sending} aria-label="Gửi"><Send /></button>
          </form>
          <small className="chat-disclaimer">AI có thể nhầm lẫn. Hãy tham khảo bác sĩ thú y khi cần.</small>
        </section>
      )}
      <button className="chat-fab" onClick={() => setOpen((value) => !value)} aria-label="Mở trợ lý PetHub">
        {open ? <X /> : <MessageCircle />}
        {!open && <span>Hỏi PetHub AI</span>}
      </button>
    </div>
  );
}
