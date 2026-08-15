import { MessageCircleReply, Star, Trash2 } from "lucide-react";
import { useEffect, useState } from "react";
import toast from "react-hot-toast";
import { Link } from "react-router-dom";
import { adminReviewApi } from "../../api/api";
import AdminPageHeader from "../../components/admin/AdminPageHeader";
import Loader from "../../components/common/Loader";
import Pagination from "../../components/common/Pagination";
import { formatDateTime, getErrorMessage } from "../../utils/format";

export default function AdminReviewsPage() {
  const [data, setData] = useState({ content: [], number: 0, totalPages: 0 });
  const [statistics, setStatistics] = useState(null);
  const [loading, setLoading] = useState(true);
  const [editingId, setEditingId] = useState(null);
  const [reply, setReply] = useState("");
  const [saving, setSaving] = useState(false);

  const load = async (page = 0) => {
    setLoading(true);
    try {
      const [reviewsResponse, statisticsResponse] = await Promise.all([
        adminReviewApi.list({ page, size: 10 }),
        adminReviewApi.statistics(),
      ]);
      setData(reviewsResponse.data);
      setStatistics(statisticsResponse.data);
    } catch (error) { toast.error(getErrorMessage(error)); }
    finally { setLoading(false); }
  };

  useEffect(() => { load(); }, []);

  const saveReply = async (reviewId) => {
    if (!reply.trim()) return toast.error("Vui lòng nhập nội dung phản hồi");
    setSaving(true);
    try {
      await adminReviewApi.reply(reviewId, reply.trim());
      toast.success("Đã lưu phản hồi");
      setEditingId(null);
      await load(data.number);
    } catch (error) { toast.error(getErrorMessage(error)); }
    finally { setSaving(false); }
  };

  const remove = async (review) => {
    if (!window.confirm(`Xóa đánh giá của ${review.userName}?`)) return;
    try {
      await adminReviewApi.remove(review.id);
      toast.success("Đã xóa đánh giá");
      await load(data.number);
    } catch (error) { toast.error(getErrorMessage(error)); }
  };

  return <>
    <AdminPageHeader title="Đánh giá khách hàng" description="Theo dõi mức độ hài lòng và phản hồi trực tiếp tới khách hàng." />
    {statistics && <div className="review-stat-grid">
      <article><small>Tổng đánh giá</small><strong>{statistics.totalReviews}</strong></article>
      <article><small>Điểm trung bình</small><strong><Star fill="currentColor" /> {Number(statistics.averageRating).toFixed(1)}</strong></article>
      <article><small>Chưa phản hồi</small><strong>{statistics.pendingReplies}</strong></article>
      <article><small>Đã phản hồi</small><strong>{statistics.repliedReviews}</strong></article>
    </div>}
    {statistics && <section className="admin-card rating-distribution"><h2>Phân bố số sao</h2>{[5, 4, 3, 2, 1].map((rating) => {
      const count = statistics.ratingDistribution?.[rating] || 0;
      const width = statistics.totalReviews ? count / statistics.totalReviews * 100 : 0;
      return <div key={rating}><span>{rating} <Star fill="currentColor" /></span><i><em style={{ width: `${width}%` }} /></i><strong>{count}</strong></div>;
    })}</section>}
    <section className="admin-card admin-review-list">
      {loading ? <Loader /> : data.content.length ? data.content.map((review) => <article key={review.id}>
        <header><div><Link to={`/products/${review.productId}`}><strong>{review.productName}</strong></Link><span>{review.userName} · {formatDateTime(review.createdAt)}</span></div><span className="review-stars">{Array.from({ length: 5 }, (_, index) => <Star key={index} fill={index < review.rating ? "currentColor" : "none"} />)}</span></header>
        <p>{review.comment}</p>
        {review.adminReply && editingId !== review.id && <div className="admin-reply"><strong>Phản hồi của {review.repliedByName || "quản trị viên"}</strong><p>{review.adminReply}</p><small>{formatDateTime(review.repliedAt)}</small></div>}
        {editingId === review.id && <div className="reply-editor"><textarea rows="3" maxLength="2000" value={reply} onChange={(event) => setReply(event.target.value)} placeholder="Nhập phản hồi cho khách hàng..." /><div><button className="button button--outline button--small" onClick={() => setEditingId(null)}>Hủy</button><button className="button button--primary button--small" disabled={saving} onClick={() => saveReply(review.id)}>{saving ? "Đang lưu..." : "Gửi phản hồi"}</button></div></div>}
        <footer><button className="button button--outline button--small" onClick={() => { setEditingId(review.id); setReply(review.adminReply || ""); }}><MessageCircleReply /> {review.adminReply ? "Sửa phản hồi" : "Phản hồi"}</button><button className="icon-button icon-button--danger" onClick={() => remove(review)} title="Xóa đánh giá"><Trash2 /></button></footer>
      </article>) : <p className="muted">Chưa có đánh giá nào.</p>}
      <Pagination page={data.number} totalPages={data.totalPages} onChange={load} />
    </section>
  </>;
}
