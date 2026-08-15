import http from "./httpClient";

const unwrap = (response) => response.data;

export const authApi = {
  login: (body) => http.post("/auth/login", body).then(unwrap),
  register: (body) => http.post("/auth/register", body).then(unwrap),
  refresh: () => http.post("/auth/refresh").then(unwrap),
  logout: () => http.post("/auth/logout"),
  logoutAll: () => http.post("/auth/logout-all"),
  forgotPassword: (email) =>
    http.post("/auth/forgot-password", { email }).then(unwrap),
  resetPassword: (body) =>
    http.post("/auth/reset-password", body).then(unwrap),
};

export const catalogApi = {
  search: (params) => http.get("/products", { params }).then(unwrap),
  getById: (id) => http.get(`/product/get-by-product-id/${id}`).then(unwrap),
  categories: () => http.get("/category/get-all").then(unwrap),
  categoryById: (id) =>
    http.get(`/category/get-category-by-id/${id}`).then(unwrap),
};

export const cartApi = {
  get: () => http.get("/cart").then(unwrap),
  add: (productId, quantity = 1) =>
    http.post("/cart/items", { productId, quantity }).then(unwrap),
  update: (itemId, quantity) =>
    http.patch(`/cart/items/${itemId}`, null, { params: { quantity } }).then(unwrap),
  remove: (itemId) => http.delete(`/cart/items/${itemId}`).then(unwrap),
  clear: () => http.delete("/cart").then(unwrap),
};

export const wishlistApi = {
  get: () => http.get("/wishlist").then(unwrap),
  add: (productId) => http.post(`/wishlist/${productId}`).then(unwrap),
  remove: (productId) => http.delete(`/wishlist/${productId}`).then(unwrap),
};

export const profileApi = {
  get: () => http.get("/user/profile").then(unwrap),
  update: (body) => http.put("/user/profile", body).then(unwrap),
  saveAddress: (body) => http.post("/address/save", body).then(unwrap),
  changePassword: (body) =>
    http.post("/user/change-password", body).then(unwrap),
};

export const orderApi = {
  checkout: (body, idempotencyKey) =>
    http.post("/orders/checkout", body, {
      headers: {
        "Idempotency-Key": idempotencyKey,
      },
    }).then(unwrap),
  mine: (params) => http.get("/orders/my", { params }).then(unwrap),
  get: (id) => http.get(`/orders/${id}`).then(unwrap),
  cancel: (id) => http.post(`/orders/${id}/cancel`).then(unwrap),
  createVnPayUrl: (id) =>
    http.post(`/payments/vnpay/create/${id}`).then(unwrap),
  verifyVnPayReturn: (params) =>
    http.get("/payments/vnpay/return", { params }).then(unwrap),
};

export const refundApi = {
  request: (orderId, reason) =>
    http
      .post(`/refunds/orders/${orderId}`, { reason })
      .then(unwrap),
  mine: (params) => http.get("/refunds/my", { params }).then(unwrap),
};

export const adminRefundApi = {
  list: (params) =>
    http.get("/admin/refunds", { params }).then(unwrap),

  approve: (id, adminNote) =>
    http
      .patch(`/admin/refunds/${id}/approve`, { adminNote })
      .then(unwrap),

  reject: (id, adminNote) =>
    http
      .patch(`/admin/refunds/${id}/reject`, { adminNote })
      .then(unwrap),

  process: (id) =>
    http
      .post(`/admin/refunds/${id}/process`)
      .then(unwrap),
};

export const adminAuditApi = {
  list: (params) =>
    http.get("/admin/audit-logs", { params }).then(unwrap),
};

export const adminInventoryApi = {
  list: (params) =>
    http
      .get("/admin/inventory-transactions", { params })
      .then(unwrap),
};

export const couponApi = {
  validate: (code, subtotal) =>
    http
      .get("/coupons/validate", {
        params: { code, subtotal },
      })
      .then(unwrap),

  available: (subtotal) =>
    http
      .get("/coupons/available", {
        params: { subtotal },
      })
      .then(unwrap),
};

export const reviewApi = {
  list: (productId, params) =>
    http.get(`/reviews/product/${productId}`, { params }).then(unwrap),
  create: (productId, body) =>
    http.post(`/reviews/product/${productId}`, body).then(unwrap),
  update: (reviewId, body) =>
    http.put(`/reviews/${reviewId}`, body).then(unwrap),
  remove: (reviewId) => http.delete(`/reviews/${reviewId}`).then(unwrap),
};

export const adminReviewApi = {
  list: (params) => http.get("/reviews/admin", { params }).then(unwrap),
  statistics: () => http.get("/reviews/admin/statistics").then(unwrap),
  reply: (reviewId, reply) => http.put(`/reviews/admin/${reviewId}/reply`, { reply }).then(unwrap),
  remove: (reviewId) => http.delete(`/reviews/admin/${reviewId}`).then(unwrap),
};

export const ragApi = {
  chat: (body) =>
    http
      .post("/rag/chat", body, { timeout: 120000 })
      .then(unwrap),
};

export const adminApi = {
  dashboard: () => http.get("/statistics/dashboard").then(unwrap),
  monthlyStatistics: (month, year) =>
    http.get("/statistics/monthly", { params: { month, year } }).then(unwrap),

  users: (params) =>
    http.get("/user/get-all", { params }).then(unwrap),
  setUserEnabled: (id, enabled) =>
    http.patch(`/user/admin/${id}/enabled`, null, { params: { enabled } }).then(unwrap),

  createCategory: (body) => http.post("/category/create", body).then(unwrap),
  updateCategory: (id, body) =>
    http.put(`/category/update/${id}`, body).then(unwrap),
  deleteCategory: (id) => http.delete(`/category/delete/${id}`).then(unwrap),

  createProduct: (formData) =>
    http.post("/product/create", formData).then(unwrap),
  updateProduct: (formData) =>
    http.put("/product/update", formData).then(unwrap),
  deleteProduct: (id) => http.delete(`/product/delete/${id}`).then(unwrap),

  orders: (params) =>
    http.get("/orders/admin/all", { params }).then(unwrap),
  updateOrderStatus: (id, body) =>
    http.patch(`/orders/admin/${id}/status`, body).then(unwrap),

  coupons: () => http.get("/coupons").then(unwrap),
  createCoupon: (body) => http.post("/coupons", body).then(unwrap),
  updateCoupon: (id, body) => http.put(`/coupons/${id}`, body).then(unwrap),
  deleteCoupon: (id) => http.delete(`/coupons/${id}`).then(unwrap),

  knowledgeDocuments: () =>
    http.get("/rag/admin/documents").then(unwrap),
  createKnowledgeDocument: (body) =>
    http.post("/rag/admin/documents", body).then(unwrap),
  setKnowledgeDocumentActive: (id, active) =>
    http.patch(`/rag/admin/documents/${id}`, null, { params: { active } }).then(unwrap),
  deleteKnowledgeDocument: (id) =>
    http.delete(`/rag/admin/documents/${id}`).then(unwrap),

  products: (params) =>
    http.get("/admin/products", { params }).then(unwrap),

  product: (id) =>
    http.get(`/admin/products/${id}`).then(unwrap),
};
