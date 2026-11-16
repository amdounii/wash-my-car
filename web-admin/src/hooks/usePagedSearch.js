import { useCallback, useEffect, useState } from "react";
import { errorMessage } from "../api/http";

const EMPTY_PAGE = { content: [], page: 0, size: 10, totalElements: 0, totalPages: 0 };

/**
 * Gère une liste paginée filtrable : filtres, page courante, chargement et erreurs.
 * La recherche texte est temporisée (300 ms) pour éviter un appel à chaque frappe.
 */
export default function usePagedSearch(fetcher, initialFilters, size = 10) {
  const [filters, setFilters] = useState(initialFilters);
  const [debounced, setDebounced] = useState(initialFilters);
  const [page, setPage] = useState(0);
  const [data, setData] = useState(EMPTY_PAGE);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    const t = setTimeout(() => setDebounced(filters), 300);
    return () => clearTimeout(t);
  }, [filters]);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    fetcher({ ...debounced, page, size })
      .then((res) => {
        if (cancelled) return;
        // Page devenue vide après une suppression : on revient à la précédente.
        if (res.content.length === 0 && page > 0) setPage(page - 1);
        else setData(res);
        setError("");
      })
      .catch((e) => !cancelled && setError(errorMessage(e)))
      .finally(() => !cancelled && setLoading(false));
    return () => {
      cancelled = true;
    };
  }, [fetcher, debounced, page, size, reloadKey]);

  /** Change un filtre et revient à la première page. */
  const setFilter = useCallback((name, value) => {
    setFilters((f) => ({ ...f, [name]: value }));
    setPage(0);
  }, []);

  const reload = useCallback(() => setReloadKey((k) => k + 1), []);

  return { filters, setFilter, page, setPage, data, loading, error, reload };
}
