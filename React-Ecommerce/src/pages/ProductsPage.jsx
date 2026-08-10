import { useEffect, useMemo, useState } from "react";
import { Filter, Search, SlidersHorizontal, X } from "lucide-react";
import { useSearchParams } from "react-router-dom";
import { catalogApi } from "../api/api";
import ProductCard from "../components/store/ProductCard";
import Pagination from "../components/common/Pagination";
import Loader from "../components/common/Loader";
import EmptyState from "../components/common/EmptyState";
import { getErrorMessage } from "../utils/format";

const INITIAL_PAGE = { content: [], number: 0, totalPages: 0, totalElements: 0 };

export default function ProductsPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const committedQuery = searchParams.get("q") || "";
  const [pageData, setPageData] = useState(INITIAL_PAGE);
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [filterOpen, setFilterOpen] = useState(false);
  const [draftQuery, setDraftQuery] = useState(committedQuery);

  useEffect(() => {
    setDraftQuery(committedQuery);
  }, [committedQuery]);

  const params = useMemo(
    () => ({
      q: committedQuery || undefined,
      categoryId: searchParams.get("categoryId") || undefined,
      petType: searchParams.get("petType") || undefined,
      minPrice: searchParams.get("minPrice") || undefined,
      maxPrice: searchParams.get("maxPrice") || undefined,
      page: Number(searchParams.get("page") || 0),
      size: 12,
      sortBy: searchParams.get("sortBy") || "id",
      direction: searchParams.get("direction") || "desc",
    }),
    [searchParams, committedQuery],
  );

  useEffect(() => {
    catalogApi.categories().then((response) => setCategories(response.categoryList ?? []));
  }, []);

  useEffect(() => {
    setLoading(true);
    setError("");
    catalogApi.search(params)
      .then((response) => setPageData(response.data ?? INITIAL_PAGE))
      .catch((requestError) => setError(getErrorMessage(requestError, "Không tải được sản phẩm")))
      .finally(() => setLoading(false));
  }, [params]);

  const updateParam = (name, value) => {
    const next = new URLSearchParams(searchParams);
    if (value === "" || value == null) next.delete(name);
    else next.set(name, value);
    if (name !== "page") next.delete("page");
    setSearchParams(next);
  };

  const submitSearch = (event) => {
    event.preventDefault();
    updateParam("q", draftQuery.trim());
  };

  const clearFilters = () => {
    setDraftQuery("");
    setSearchParams({});
  };

  return (
    <div className="container catalog-page">
      <div className="page-hero page-hero--compact">
        <span className="eyebrow">PetHub Collection</span>
        <h1>Sản phẩm cho thú cưng</h1>
        <p>Lọc nhanh theo nhu cầu, ngân sách và người bạn đồng hành của bạn.</p>
      </div>

      <form className="catalog-search" onSubmit={submitSearch}>
        <Search />
        <input
          value={draftQuery}
          onChange={(event) => setDraftQuery(event.target.value)}
          placeholder="Bạn đang tìm sản phẩm gì?"
        />
        <button className="button button--primary" type="submit">Tìm kiếm</button>
        <button className="button button--outline filter-toggle" type="button" onClick={() => setFilterOpen(true)}>
          <Filter size={18} /> Bộ lọc
        </button>
      </form>

      <div className="catalog-layout">
        <aside className={`filters ${filterOpen ? "is-open" : ""}`}>
          <div className="filters__header">
            <h2><SlidersHorizontal size={20} /> Bộ lọc</h2>
            <button className="icon-button filters__close" onClick={() => setFilterOpen(false)}><X /></button>
          </div>
          <label>Danh mục
            <select value={params.categoryId || ""} onChange={(e) => updateParam("categoryId", e.target.value)}>
              <option value="">Tất cả danh mục</option>
              {categories.map((category) => <option key={category.id} value={category.id}>{category.name}</option>)}
            </select>
          </label>
          <label>Thú cưng
            <select value={params.petType || ""} onChange={(e) => updateParam("petType", e.target.value)}>
              <option value="">Tất cả</option>
              <option value="DOG">Chó</option>
              <option value="CAT">Mèo</option>
              <option value="OTHER">Khác</option>
            </select>
          </label>
          <div className="form-row">
            <label>Giá từ
              <input type="number" min="0" value={params.minPrice || ""} onChange={(e) => updateParam("minPrice", e.target.value)} />
            </label>
            <label>Đến
              <input type="number" min="0" value={params.maxPrice || ""} onChange={(e) => updateParam("maxPrice", e.target.value)} />
            </label>
          </div>
          <button className="button button--ghost button--full" onClick={clearFilters}>Xóa bộ lọc</button>
        </aside>

        <section className="catalog-results">
          <div className="catalog-toolbar">
            <p><strong>{pageData.totalElements || 0}</strong> sản phẩm phù hợp</p>
            <select
              value={`${params.sortBy}-${params.direction}`}
              onChange={(e) => {
                const [sortBy, direction] = e.target.value.split("-");
                const next = new URLSearchParams(searchParams);
                next.set("sortBy", sortBy);
                next.set("direction", direction);
                next.delete("page");
                setSearchParams(next);
              }}
            >
              <option value="id-desc">Mới nhất</option>
              <option value="soldCount-desc">Bán chạy</option>
              <option value="price-asc">Giá tăng dần</option>
              <option value="price-desc">Giá giảm dần</option>
              <option value="averageRating-desc">Đánh giá cao</option>
            </select>
          </div>
          {loading ? <Loader /> : error ? (
            <EmptyState title="Không thể tải sản phẩm" description={error} />
          ) : pageData.content.length === 0 ? (
            <EmptyState title="Chưa tìm thấy sản phẩm" description="Hãy thử từ khóa hoặc bộ lọc khác." />
          ) : (
            <>
              <div className="product-grid">
                {pageData.content.map((product) => <ProductCard key={product.id} product={product} />)}
              </div>
              <Pagination
                page={pageData.number}
                totalPages={pageData.totalPages}
                onChange={(page) => {
                  updateParam("page", page);
                  window.scrollTo({ top: 0, behavior: "smooth" });
                }}
              />
            </>
          )}
        </section>
      </div>
      {filterOpen && <button className="drawer-backdrop" aria-label="Đóng bộ lọc" onClick={() => setFilterOpen(false)} />}
    </div>
  );
}
