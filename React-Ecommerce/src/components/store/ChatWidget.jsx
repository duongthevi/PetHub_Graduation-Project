import { useRef, useState } from "react";
import { Bot, ExternalLink, MessageCircle, Send, X } from "lucide-react";
import { Link } from "react-router-dom";
import { ragApi } from "../../api/api";
import { getErrorMessage } from "../../utils/format";

const welcome = {
  role: "assistant",
  content: "Chào bạn! Mình là trợ lý PetHub. Bạn cần tư vấn sản phẩm hay cách chăm sóc thú cưng?",
};

export default function ChatWidget() {
  const [open, setOpen] = useState(false);
  const [messages, setMessages] = useState([welcome]);
  const [question, setQuestion] = useState("");
  const [sending, setSending] = useState(false);
  const conversationId = useRef(crypto.randomUUID());

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
        products: data.recommendedProductIds,
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
              <div key={`${message.role}-${index}`} className={`chat-message chat-message--${message.role} ${message.error ? "is-error" : ""}`}>
                <p>{message.content}</p>
                {message.products?.length > 0 && (
                  <div className="chat-products">
                    {message.products.slice(0, 3).map((id) => (
                      <Link key={id} to={`/products/${id}`} onClick={() => setOpen(false)}>
                        Xem sản phẩm #{id} <ExternalLink size={13} />
                      </Link>
                    ))}
                  </div>
                )}
                {message.citations?.length > 0 && <small>Nguồn: {message.citations.map((item) => item.title).join(", ")}</small>}
              </div>
            ))}
            {sending && <div className="chat-message chat-message--assistant chat-typing"><i /><i /><i /></div>}
          </div>
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
