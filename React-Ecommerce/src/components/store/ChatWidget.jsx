import { useEffect, useRef, useState } from "react";
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
  content:
    "Chào bạn! Mình là trợ lý PetHub. Bạn cần tư vấn sản phẩm, chăm sóc thú cưng hay hỗ trợ đặt hàng?",
};

const getChatActionKey = (action) =>
  `chat:${action?.type ?? "UNKNOWN"}:${action?.productId ?? "cart"}`;

const getChatActionLabel = (action) => {
  if (action?.type === "ADD_TO_CART") {
    return "Xác nhận thêm vào giỏ";
  }

  if (action?.type === "GO_TO_CHECKOUT") {
    return "Mở trang thanh toán";
  }

  return "Xác nhận và thanh toán";
};

const normalizeCommand = (value = "") =>
  value
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .replace(/đ/g, "d")
    .replace(/Đ/g, "D")
    .toLowerCase()
    .trim();

const isAddToCartCommand = (value) => {
  const normalized = normalizeCommand(value);

  return (
    (normalized.includes("them") || normalized.includes("bo")) &&
    normalized.includes("gio")
  );
};

const isCheckoutCommand = (value) => {
  const normalized = normalizeCommand(value);

  return (
    [
      "tien hanh thanh toan",
      "thanh toan gio hang",
      "mo trang thanh toan",
      "den trang thanh toan",
    ].some((keyword) => normalized.includes(keyword)) ||
    (normalized.includes("thanh toan") && normalized.includes("gio"))
  );
};

const acceptsAnyProduct = (value) => {
  const normalized = normalizeCommand(value);

  return [
    "bat ky",
    "tuy ban",
    "loai nao cung duoc",
    "san pham nao cung duoc",
  ].some((keyword) => normalized.includes(keyword));
};

const parseCommandQuantity = (value) => {
  const quantityMatch = normalizeCommand(value).match(/\b(\d{1,2})\b/);

  if (!quantityMatch) return 1;

  return Math.max(1, Math.min(Number(quantityMatch[1]), 99));
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
  const messagesEndRef = useRef(null);

  useEffect(() => {
    if (!open) return;

    messagesEndRef.current?.scrollIntoView({
      behavior: "smooth",
      block: "end",
    });
  }, [messages, open, sending]);

  const send = async (event) => {
    event.preventDefault();

    const content = question.trim();
    if (!content || sending) return;

    setQuestion("");
    setMessages((current) => [
      ...current,
      {
        role: "user",
        content,
      },
    ]);
    setSending(true);

    try {
      const response = await ragApi.chat({
        conversationId: conversationId.current,
        question: content,
      });

      const data = response.data ?? {};
      conversationId.current =
        data.conversationId ?? conversationId.current;

      const responseProducts = data.recommendedProducts?.length
        ? data.recommendedProducts
        : (data.recommendedProductIds ?? []).map((id) => ({ id }));

      const previousProductMessage = [...messages]
        .reverse()
        .find((message) => message.products?.length);
      const previousProducts = previousProductMessage?.products ?? [];

      const addToCartRequested = isAddToCartCommand(content);
      const checkoutRequested = isCheckoutCommand(content);

      const candidateProducts =
        responseProducts.length > 0
          ? responseProducts
          : addToCartRequested
            ? previousProducts
            : [];

      let action = data.action ?? null;

      /*
       * Phòng trường hợp Backend chưa nhận diện được một biến thể câu lệnh
       * thanh toán, nhưng người dùng đã yêu cầu rõ ràng.
       */
      if (checkoutRequested && action?.type !== "GO_TO_CHECKOUT") {
        action = {
          type: "GO_TO_CHECKOUT",
          productId: null,
          productName: null,
          quantity: null,
          confirmationMessage: "Mở trang thanh toán cho giỏ hàng hiện tại?",
        };
      }

      /*
       * "Tiến hành thanh toán" là một mệnh lệnh rõ ràng nên chuyển trang ngay.
       * Không gắn sản phẩm cũ vào câu trả lời thanh toán.
       */
      if (action?.type === "GO_TO_CHECKOUT") {
        const checkoutOpened = await executeChatAction(action);

        if (!checkoutOpened && isAuthenticated && cartCount <= 0) {
          setMessages((current) => [
            ...current,
            {
              role: "assistant",
              content:
                "Giỏ hàng của bạn đang trống. Vui lòng thêm sản phẩm trước khi thanh toán.",
              citations: [],
              products: [],
              action: null,
            },
          ]);
        }

        return;
      }

      /*
       * Fallback khi Backend trả action=null:
       * - dùng sản phẩm có tên xuất hiện trong câu;
       * - hoặc sản phẩm duy nhất ở lượt tư vấn gần nhất;
       * - hoặc sản phẩm đầu tiên khi khách nói "bất kỳ".
       */
      if (!action && addToCartRequested && candidateProducts.length > 0) {
        const normalizedQuestion = normalizeCommand(content);
        const nameMatches = candidateProducts.filter((product) => {
          const normalizedName = normalizeCommand(product.name);

          return (
            normalizedName &&
            normalizedQuestion.includes(normalizedName)
          );
        });

        let selectedProduct = null;

        if (nameMatches.length === 1) {
          [selectedProduct] = nameMatches;
        } else if (candidateProducts.length === 1) {
          [selectedProduct] = candidateProducts;
        } else if (acceptsAnyProduct(content)) {
          [selectedProduct] = candidateProducts;
        }

        if (selectedProduct?.id) {
          const quantity = parseCommandQuantity(content);

          action = {
            type: "ADD_TO_CART",
            productId: selectedProduct.id,
            productName:
              selectedProduct.name || `Sản phẩm #${selectedProduct.id}`,
            quantity,
            confirmationMessage: `Thêm ${quantity} × ${
              selectedProduct.name || "sản phẩm"
            } vào giỏ hàng?`,
          };
        }
      }

      let autoAddSucceeded = false;

      if (action?.type === "ADD_TO_CART") {
        autoAddSucceeded = await executeChatAction(action);
      }

      setMessages((current) => [
        ...current,
        {
          role: "assistant",
          content: autoAddSucceeded
            ? `Đã thêm ${action.quantity ?? 1} × ${
                action.productName || "sản phẩm"
              } vào giỏ hàng của bạn.`
            : data.answer ||
            "Mình chưa thể tạo câu trả lời phù hợp. Bạn vui lòng thử lại nhé.",
          citations: data.citations ?? [],
          products: autoAddSucceeded
            ? []
            : addToCartRequested
              ? candidateProducts
              : responseProducts,
          action: autoAddSucceeded ? null : action,
        },
      ]);
    } catch (error) {
      setMessages((current) => [
        ...current,
        {
          role: "assistant",
          error: true,
          content: getErrorMessage(
            error,
            "Trợ lý đang bận. Bạn vui lòng thử lại sau.",
          ),
        },
      ]);
    } finally {
      setSending(false);
    }
  };

  const stockFor = (product) => {
    const stock = Number(product?.stock ?? 99);
    return Number.isFinite(stock) ? Math.max(0, stock) : 99;
  };

  const quantityFor = (product) => {
    const quantity = Number(quantities[product.id] ?? 1);
    return Number.isFinite(quantity) ? Math.max(1, quantity) : 1;
  };

  const changeQuantity = (product, difference) => {
    const maximum = Math.max(1, Math.min(stockFor(product), 99));

    setQuantities((current) => {
      const previous = Number(current[product.id] ?? 1);
      const next = Math.min(
        maximum,
        Math.max(1, previous + difference),
      );

      return {
        ...current,
        [product.id]: next,
      };
    });
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
    if (!product?.id) {
      toast.error("Không xác định được sản phẩm");
      return;
    }

    if (!requireLogin()) return;

    const stock = stockFor(product);
    if (stock <= 0) {
      toast.error("Sản phẩm hiện đã hết hàng");
      return;
    }

    const quantity = quantityFor(product);
    if (quantity > stock) {
      toast.error(`Sản phẩm chỉ còn ${stock} trong kho`);
      return;
    }

    const actionKey = `${product.id}:${
      goToCheckout ? "checkout" : "cart"
    }`;
    setPendingAction(actionKey);

    try {
      await addItem(product.id, quantity);

      toast.success(
        `Đã thêm ${quantity} × ${
          product.name || "sản phẩm"
        } vào giỏ`,
      );

      if (goToCheckout) {
        setOpen(false);
        navigate("/checkout");
      }
    } catch (error) {
      toast.error(
        getErrorMessage(
          error,
          "Không thể thêm sản phẩm vào giỏ",
        ),
      );
    } finally {
      setPendingAction(null);
    }
  };

  const goToCheckout = () => {
    if (!requireLogin()) return false;

    if (cartCount <= 0) {
      toast.error("Giỏ hàng chưa có sản phẩm");
      return false;
    }

    setOpen(false);
    navigate("/checkout");
    return true;
  };

  const executeChatAction = async (action) => {
    if (!action?.type) {
      toast.error("Hành động đặt hàng không hợp lệ");
      return false;
    }

    if (action.type === "GO_TO_CHECKOUT") {
      return goToCheckout();
    }

    if (!action.productId) {
      toast.error("Chatbot chưa xác định được sản phẩm");
      return false;
    }

    if (!requireLogin()) return false;

    const quantity = Math.max(
      1,
      Math.min(Number(action.quantity ?? 1), 99),
    );
    const actionKey = getChatActionKey(action);
    setPendingAction(actionKey);

    try {
      await addItem(action.productId, quantity);

      toast.success(
        `Đã thêm ${quantity} × ${
          action.productName || "sản phẩm"
        } vào giỏ`,
      );

      if (action.type === "ADD_TO_CART_AND_CHECKOUT") {
        setOpen(false);
        navigate("/checkout");
      }

      return true;
    } catch (error) {
      toast.error(
        getErrorMessage(
          error,
          "Không thể thực hiện yêu cầu đặt hàng",
        ),
      );

      return false;
    } finally {
      setPendingAction(null);
    }
  };

  return (
    <div className={`chat-widget ${open ? "is-open" : ""}`}>
      {open && (
        <section
          className="chat-panel"
          aria-label="Trợ lý PetHub AI"
        >
          <header>
            <span>
              <Bot />
              <span>
                <strong>PetHub AI</strong>
                <small>Tư vấn và hỗ trợ đặt hàng</small>
              </span>
            </span>

            <button
              type="button"
              onClick={() => setOpen(false)}
              aria-label="Đóng chatbot"
            >
              <X />
            </button>
          </header>

          <div className="chat-messages" aria-live="polite">
            {messages.map((message, index) => {
              const citationTitles = [
                ...new Set(
                  (message.citations ?? [])
                    .map((item) => item.title)
                    .filter(Boolean),
                ),
              ];

              return (
                <div
                  key={`${message.role}-${index}`}
                  className={`chat-message chat-message--${message.role} ${
                    message.error ? "is-error" : ""
                  } ${
                    message.products?.length || message.action
                      ? "has-products"
                      : ""
                  }`}
                >
                  <p>{message.content}</p>

                  {message.products?.length > 0 && (
                    <div className="chat-products">
                      {message.products
                        .slice(0, 3)
                        .map((product) => {
                          const stock = stockFor(product);
                          const quantity = quantityFor(product);
                          const cartActionKey =
                            `${product.id}:cart`;
                          const checkoutActionKey =
                            `${product.id}:checkout`;

                          return (
                            <article
                              className="chat-product"
                              key={product.id}
                            >
                              <img
                                src={
                                  product.imageUrl ||
                                  "/product-placeholder.svg"
                                }
                                alt={
                                  product.name ||
                                  `Sản phẩm #${product.id}`
                                }
                              />

                              <div className="chat-product__content">
                                <Link
                                  to={`/products/${product.id}`}
                                  onClick={() => setOpen(false)}
                                >
                                  {product.name ||
                                    `Sản phẩm #${product.id}`}
                                  <ExternalLink size={12} />
                                </Link>

                                {product.price != null && (
                                  <strong>
                                    {formatCurrency(product.price)}
                                  </strong>
                                )}

                                {product.stock != null && (
                                  <small>
                                    {stock > 0
                                      ? `Còn ${stock} sản phẩm`
                                      : "Đã hết hàng"}
                                  </small>
                                )}

                                <div className="chat-product__quantity">
                                  <button
                                    type="button"
                                    onClick={() =>
                                      changeQuantity(product, -1)
                                    }
                                    disabled={
                                      quantity <= 1 ||
                                      pendingAction !== null
                                    }
                                    aria-label="Giảm số lượng"
                                  >
                                    <Minus />
                                  </button>

                                  <b>{quantity}</b>

                                  <button
                                    type="button"
                                    onClick={() =>
                                      changeQuantity(product, 1)
                                    }
                                    disabled={
                                      quantity >= stock ||
                                      pendingAction !== null
                                    }
                                    aria-label="Tăng số lượng"
                                  >
                                    <Plus />
                                  </button>
                                </div>

                                <div className="chat-product__actions">
                                  <button
                                    type="button"
                                    onClick={() =>
                                      addProduct(product)
                                    }
                                    disabled={
                                      pendingAction !== null ||
                                      stock <= 0
                                    }
                                  >
                                    <ShoppingCart />
                                    {pendingAction === cartActionKey
                                      ? "Đang thêm..."
                                      : "Thêm vào giỏ"}
                                  </button>

                                  <button
                                    type="button"
                                    onClick={() =>
                                      addProduct(product, true)
                                    }
                                    disabled={
                                      pendingAction !== null ||
                                      stock <= 0
                                    }
                                  >
                                    <CreditCard />
                                    {pendingAction ===
                                    checkoutActionKey
                                      ? "Đang xử lý..."
                                      : "Thêm và thanh toán"}
                                  </button>
                                </div>
                              </div>
                            </article>
                          );
                        })}
                    </div>
                  )}

                  {message.action && (
                    <div className="chat-commerce-action">
                      <strong>
                        {message.action.confirmationMessage ||
                          "Bạn có muốn thực hiện yêu cầu này?"}
                      </strong>

                      <button
                        type="button"
                        disabled={pendingAction !== null}
                        onClick={() =>
                          executeChatAction(message.action)
                        }
                      >
                        {message.action.type ===
                        "ADD_TO_CART" ? (
                          <ShoppingCart />
                        ) : (
                          <CreditCard />
                        )}

                        {pendingAction ===
                        getChatActionKey(message.action)
                          ? "Đang xử lý..."
                          : getChatActionLabel(
                              message.action,
                            )}
                      </button>
                    </div>
                  )}

                  {citationTitles.length > 0 && (
                    <small>
                      Nguồn: {citationTitles.join(", ")}
                    </small>
                  )}
                </div>
              );
            })}

            {sending && (
              <div className="chat-message chat-message--assistant chat-typing">
                <i />
                <i />
                <i />
              </div>
            )}

            <div ref={messagesEndRef} />
          </div>

          {cartCount > 0 && (
            <div className="chat-cart-summary">
              <span>
                <ShoppingCart />
                Giỏ hàng có <strong>{cartCount}</strong> sản phẩm
              </span>

              <button
                type="button"
                onClick={goToCheckout}
                disabled={pendingAction !== null}
              >
                Thanh toán
              </button>
            </div>
          )}

          <form onSubmit={send}>
            <input
              value={question}
              onChange={(event) =>
                setQuestion(event.target.value)
              }
              placeholder="Nhập câu hỏi của bạn..."
              disabled={sending}
              aria-label="Nội dung trò chuyện"
            />

            <button
              type="submit"
              disabled={!question.trim() || sending}
              aria-label="Gửi tin nhắn"
            >
              <Send />
            </button>
          </form>

          <small className="chat-disclaimer">
            AI có thể nhầm lẫn. Hãy tham khảo bác sĩ thú y khi cần.
          </small>
        </section>
      )}

      <button
        type="button"
        className="chat-fab"
        onClick={() => setOpen((value) => !value)}
        aria-label={open ? "Đóng trợ lý PetHub" : "Mở trợ lý PetHub"}
      >
        {open ? <X /> : <MessageCircle />}
        {!open && <span>Hỏi PetHub AI</span>}
      </button>
    </div>
  );
}