import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
} from "react";
import { wishlistApi } from "../api/api";
import { useAuth } from "./AuthContext";

const WishlistContext = createContext(null);

export function WishlistProvider({ children }) {
  const { isAuthenticated, isAdmin } = useAuth();
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(false);

  const refreshWishlist = useCallback(async () => {
    if (!isAuthenticated || isAdmin) {
      setItems([]);
      return [];
    }

    setLoading(true);
    try {
      const response = await wishlistApi.get();
      const nextItems = response.data ?? [];
      setItems(nextItems);
      return nextItems;
    } finally {
      setLoading(false);
    }
  }, [isAuthenticated, isAdmin]);

  useEffect(() => {
    refreshWishlist().catch(() => setItems([]));
  }, [refreshWishlist]);

  const addFavorite = useCallback(async (productId) => {
    const response = await wishlistApi.add(productId);
    const newItem = response.data;
    if (newItem) {
      setItems((current) =>
        current.some((item) => item.product.id === productId)
          ? current
          : [newItem, ...current],
      );
    } else {
      await refreshWishlist();
    }
  }, [refreshWishlist]);

  const removeFavorite = useCallback(async (productId) => {
    await wishlistApi.remove(productId);
    setItems((current) =>
      current.filter((item) => item.product.id !== productId),
    );
  }, []);

  const favoriteIds = useMemo(
    () => new Set(items.map((item) => item.product.id)),
    [items],
  );

  const value = useMemo(
    () => ({
      items,
      loading,
      favoriteIds,
      isFavorite: (productId) => favoriteIds.has(productId),
      refreshWishlist,
      addFavorite,
      removeFavorite,
    }),
    [items, loading, favoriteIds, refreshWishlist, addFavorite, removeFavorite],
  );

  return (
    <WishlistContext.Provider value={value}>
      {children}
    </WishlistContext.Provider>
  );
}

export function useWishlist() {
  const context = useContext(WishlistContext);
  if (!context) {
    throw new Error("useWishlist must be used inside WishlistProvider");
  }
  return context;
}
