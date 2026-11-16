import axios from "axios";

const TOKEN_KEY = "wmc_admin_token";

/** Instance Axios partagée : URL de l'API et jeton JWT ajoutés automatiquement. */
const http = axios.create({
  baseURL: (import.meta.env.VITE_API_URL || "http://localhost:8081") + "/api",
  timeout: 15000,
});

export const tokenStorage = {
  get: () => localStorage.getItem(TOKEN_KEY),
  set: (token) => localStorage.setItem(TOKEN_KEY, token),
  clear: () => localStorage.removeItem(TOKEN_KEY),
};

http.interceptors.request.use((config) => {
  const token = tokenStorage.get();
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

/** Jeton expiré ou invalide : on vide la session et on renvoie vers la connexion. */
http.interceptors.response.use(
  (response) => response,
  (error) => {
    const isLogin = error.config?.url?.includes("/auth/login");
    if (error.response?.status === 401 && !isLogin) {
      tokenStorage.clear();
      if (window.location.pathname !== "/login") window.location.assign("/login");
    }
    return Promise.reject(error);
  }
);

/** Message d'erreur lisible à partir d'une erreur Axios (format ErrorResponse du backend). */
export function errorMessage(error) {
  if (!error.response) return "Serveur injoignable. Vérifiez que l'API est démarrée.";
  return error.response.data?.message || `Erreur ${error.response.status}`;
}

/** Erreurs de validation champ par champ ({ champ: message }). */
export function fieldErrors(error) {
  return error.response?.data?.erreurs || {};
}

export default http;
