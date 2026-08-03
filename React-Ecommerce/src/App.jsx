import { Navigate, Route, Routes } from "react-router-dom";
import StoreLayout from "./components/layout/StoreLayout";
import AdminLayout from "./components/layout/AdminLayout";
import { AdminRoute, GuestRoute, ProtectedRoute } from "./components/common/RouteGuards";
import HomePage from "./pages/HomePage";
import ProductsPage from "./pages/ProductsPage";
import ProductDetailPage from "./pages/ProductDetailPage";
import LoginPage from "./pages/auth/LoginPage";
import RegisterPage from "./pages/auth/RegisterPage";
import ForgotPasswordPage from "./pages/auth/ForgotPasswordPage";
import ResetPasswordPage from "./pages/auth/ResetPasswordPage";
import CartPage from "./pages/account/CartPage";
import WishlistPage from "./pages/account/WishlistPage";
import CheckoutPage from "./pages/account/CheckoutPage";
import OrdersPage from "./pages/account/OrdersPage";
import OrderDetailPage from "./pages/account/OrderDetailPage";
import ProfilePage from "./pages/account/ProfilePage";
import VnPayReturnPage from "./pages/account/VnPayReturnPage";
import DashboardPage from "./pages/admin/DashboardPage";
import AdminProductsPage from "./pages/admin/AdminProductsPage";
import ProductFormPage from "./pages/admin/ProductFormPage";
import AdminCategoriesPage from "./pages/admin/AdminCategoriesPage";
import AdminOrdersPage from "./pages/admin/AdminOrdersPage";
import AdminRefundsPage from "./pages/admin/AdminRefundsPage";
import AdminCouponsPage from "./pages/admin/AdminCouponsPage";
import AdminUsersPage from "./pages/admin/AdminUsersPage";
import KnowledgeAdminPage from "./pages/admin/KnowledgeAdminPage";
import AdminInventoryPage from "./pages/admin/AdminInventoryPage";
import AdminAuditLogsPage from "./pages/admin/AdminAuditLogsPage";
import NotFoundPage from "./pages/NotFoundPage";
import { REFUND_ENABLED } from "./config/features";

export default function App() {
  return (
    <Routes>
      <Route element={<StoreLayout />}>
        <Route index element={<HomePage />} />
        <Route path="products" element={<ProductsPage />} />
        <Route path="products/:productId" element={<ProductDetailPage />} />

        <Route element={<GuestRoute />}>
          <Route path="login" element={<LoginPage />} />
          <Route path="register" element={<RegisterPage />} />
        </Route>
        <Route path="forgot-password" element={<ForgotPasswordPage />} />
        <Route path="reset-password" element={<ResetPasswordPage />} />

        <Route element={<ProtectedRoute />}>
          <Route path="cart" element={<CartPage />} />
          <Route path="wishlist" element={<WishlistPage />} />
          <Route path="checkout" element={<CheckoutPage />} />
          <Route path="orders" element={<OrdersPage />} />
          <Route path="orders/:orderId" element={<OrderDetailPage />} />
          <Route path="profile" element={<ProfilePage />} />
        </Route>
        <Route path="payment/vnpay/return" element={<VnPayReturnPage />} />
      </Route>

      <Route element={<AdminRoute />}>
        <Route path="admin" element={<AdminLayout />}>
          <Route index element={<Navigate to="dashboard" replace />} />
          <Route path="dashboard" element={<DashboardPage />} />
          <Route path="products" element={<AdminProductsPage />} />
          <Route path="products/new" element={<ProductFormPage />} />
          <Route path="products/:productId/edit" element={<ProductFormPage />} />
          <Route path="categories" element={<AdminCategoriesPage />} />
          <Route path="orders" element={<AdminOrdersPage />} />
          {REFUND_ENABLED && (
            <Route
              path="refunds"
              element={<AdminRefundsPage />}
            />
          )}
          <Route path="inventory" element={<AdminInventoryPage />} />
          <Route path="audit-logs" element={<AdminAuditLogsPage />} />
          <Route path="coupons" element={<AdminCouponsPage />} />
          <Route path="users" element={<AdminUsersPage />} />
          <Route path="knowledge" element={<KnowledgeAdminPage />} />
        </Route>
      </Route>

      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  );
}
