import { ArrowLeft } from "lucide-react";
import { Link } from "react-router-dom";

export default function NotFoundPage() {
  return <main className="not-found"><span>404</span><h1>Trang này không tồn tại</h1><p>Liên kết có thể đã thay đổi hoặc nội dung đã được gỡ bỏ.</p><Link className="button button--primary" to="/"><ArrowLeft /> Quay lại trang chủ</Link></main>;
}
