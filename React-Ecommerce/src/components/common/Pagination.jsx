import { ChevronLeft, ChevronRight } from "lucide-react";

export default function Pagination({ page = 0, totalPages = 0, onChange }) {
  if (totalPages <= 1) return null;
  const pages = Array.from({ length: totalPages }, (_, index) => index)
    .filter((item) => item === 0 || item === totalPages - 1 || Math.abs(item - page) <= 1);

  return (
    <nav className="pagination" aria-label="Phân trang">
      <button
        className="icon-button"
        disabled={page === 0}
        onClick={() => onChange(page - 1)}
        aria-label="Trang trước"
      >
        <ChevronLeft size={18} />
      </button>
      {pages.map((item, index) => (
        <span key={item} className="pagination__item-wrap">
          {index > 0 && item - pages[index - 1] > 1 && (
            <span className="pagination__ellipsis">…</span>
          )}
          <button
            className={`pagination__page ${item === page ? "is-active" : ""}`}
            onClick={() => onChange(item)}
            aria-current={item === page ? "page" : undefined}
          >
            {item + 1}
          </button>
        </span>
      ))}
      <button
        className="icon-button"
        disabled={page >= totalPages - 1}
        onClick={() => onChange(page + 1)}
        aria-label="Trang sau"
      >
        <ChevronRight size={18} />
      </button>
    </nav>
  );
}
