import { Navigate, Outlet, useLocation } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";
import Loader from "./Loader";

export function ProtectedRoute() {
  const { loading, isAuthenticated } = useAuth();
  const location = useLocation();
  if (loading) return <Loader fullPage label="Đang kiểm tra phiên đăng nhập..." />;
  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: location }} />;
  }
  return <Outlet />;
}

export function GuestRoute() {
  const { loading, isAuthenticated, isAdmin } = useAuth();
  if (loading) return <Loader fullPage />;
  if (isAuthenticated) {
    return <Navigate to={isAdmin ? "/admin/dashboard" : "/"} replace />;
  }
  return <Outlet />;
}

export function AdminRoute() {
  const { loading, isAuthenticated, isAdmin } = useAuth();
  const location = useLocation();
  if (loading) return <Loader fullPage label="Đang xác thực quyền quản trị..." />;
  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: location }} />;
  }
  if (!isAdmin) return <Navigate to="/" replace />;
  return <Outlet />;
}
