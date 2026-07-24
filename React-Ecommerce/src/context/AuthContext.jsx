import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
} from "react";
import { authApi } from "../api/api";
import {
  AUTH_CHANGED_EVENT,
  clearSession,
  readSession,
  saveSession,
} from "../utils/session";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [session, setSession] = useState(readSession);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const syncSession = (event) => setSession(event.detail ?? readSession());
    window.addEventListener(AUTH_CHANGED_EVENT, syncSession);
    return () => window.removeEventListener(AUTH_CHANGED_EVENT, syncSession);
  }, []);

  useEffect(() => {
    let active = true;
    async function bootstrap() {
      if (!readSession().accessToken) {
        try {
          const response = await authApi.refresh();
          if (active) setSession(saveSession(response));
        } catch {
          if (active) setSession(clearSession());
        }
      }
      if (active) setLoading(false);
    }
    bootstrap();
    return () => {
      active = false;
    };
  }, []);

  const login = useCallback(async (credentials) => {
    const response = await authApi.login(credentials);
    const nextSession = saveSession(response);
    setSession(nextSession);
    return nextSession.user;
  }, []);

  const register = useCallback((payload) => authApi.register(payload), []);

  const logout = useCallback(async (allDevices = false) => {
    try {
      if (allDevices) await authApi.logoutAll();
      else await authApi.logout();
    } finally {
      setSession(clearSession());
    }
  }, []);

  const value = useMemo(
    () => ({
      user: session.user,
      accessToken: session.accessToken,
      isAuthenticated: Boolean(session.accessToken && session.user),
      isAdmin: session.user?.role === "ADMIN",
      loading,
      login,
      register,
      logout,
    }),
    [session, loading, login, register, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error("useAuth must be used inside AuthProvider");
  return context;
}
