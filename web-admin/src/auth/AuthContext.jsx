import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import { authApi } from "../api/endpoints";
import { tokenStorage } from "../api/http";

const AuthContext = createContext(null);

/**
 * Session administrateur : le jeton est conservé dans le localStorage,
 * le profil est rechargé au démarrage pour vérifier que le jeton est encore valide.
 */
export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(Boolean(tokenStorage.get()));

  useEffect(() => {
    if (!tokenStorage.get()) return;
    authApi
      .profil()
      .then((profil) => (profil.role === "ADMIN" ? setUser(profil) : tokenStorage.clear()))
      .catch(() => tokenStorage.clear())
      .finally(() => setLoading(false));
  }, []);

  const login = useCallback(async (email, motDePasse) => {
    const res = await authApi.login(email, motDePasse);
    if (res.role !== "ADMIN") {
      throw new Error("Cet espace est réservé aux administrateurs.");
    }
    tokenStorage.set(res.token);
    setUser({ id: res.userId, nom: res.nom, prenom: res.prenom, email: res.email, role: res.role });
  }, []);

  const logout = useCallback(() => {
    tokenStorage.clear();
    setUser(null);
  }, []);

  const value = useMemo(() => ({ user, loading, login, logout }), [user, loading, login, logout]);
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export const useAuth = () => useContext(AuthContext);
