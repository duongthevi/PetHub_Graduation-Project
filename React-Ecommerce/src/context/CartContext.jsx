import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
} from "react";
import { cartApi } from "../api/api";
import { useAuth } from "./AuthContext";

const EMPTY_CART = { id: null, items: [], totalItems: 0, subtotal: 0 };
const CartContext = createContext(null);

export function CartProvider({ children }) {
  const { isAuthenticated } = useAuth();
  const [cart, setCart] = useState(EMPTY_CART);
  const [loading, setLoading] = useState(false);

  const refreshCart = useCallback(async () => {
    if (!isAuthenticated) {
      setCart(EMPTY_CART);
      return EMPTY_CART;
    }
    setLoading(true);
    try {
      const response = await cartApi.get();
      const nextCart = response.data ?? EMPTY_CART;
      setCart(nextCart);
      return nextCart;
    } finally {
      setLoading(false);
    }
  }, [isAuthenticated]);

  useEffect(() => {
    refreshCart().catch(() => setCart(EMPTY_CART));
  }, [refreshCart]);

  const runAndStore = useCallback(async (request) => {
    const response = await request();
    const nextCart = response.data ?? EMPTY_CART;
    setCart(nextCart);
    return nextCart;
  }, []);

  const addItem = useCallback(
    (productId, quantity = 1) =>
      runAndStore(() => cartApi.add(productId, quantity)),
    [runAndStore],
  );
  const updateItem = useCallback(
    (itemId, quantity) =>
      runAndStore(() => cartApi.update(itemId, quantity)),
    [runAndStore],
  );
  const removeItem = useCallback(
    (itemId) => runAndStore(() => cartApi.remove(itemId)),
    [runAndStore],
  );
  const clearCart = useCallback(async () => {
    await cartApi.clear();
    setCart(EMPTY_CART);
  }, []);

  const value = useMemo(
    () => ({
      cart,
      loading,
      cartCount: cart.totalItems ?? 0,
      refreshCart,
      addItem,
      updateItem,
      removeItem,
      clearCart,
    }),
    [cart, loading, refreshCart, addItem, updateItem, removeItem, clearCart],
  );

  return <CartContext.Provider value={value}>{children}</CartContext.Provider>;
}

export function useCart() {
  const context = useContext(CartContext);
  if (!context) throw new Error("useCart must be used inside CartProvider");
  return context;
}
