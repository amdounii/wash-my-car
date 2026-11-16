const BASE = "http://localhost:8081";

async function request(path, options = {}, token) {
  const res = await fetch(BASE + path, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
  });
  const data = await res.json().catch(() => ({}));
  if (!res.ok) {
    const err = new Error(data.message || `Erreur ${res.status}`);
    err.status = res.status;
    throw err;
  }
  return data;
}

export const login = (email, motDePasse) =>
  request("/api/auth/login", { method: "POST", body: JSON.stringify({ email, motDePasse }) });

export const getPending = (token) => request("/api/admin/stations/pending", {}, token);

export const approve = (id, token) =>
  request(`/api/admin/stations/${id}/approve`, { method: "PATCH" }, token);

export const reject = (id, motif, token) =>
  request(`/api/admin/stations/${id}/reject`, { method: "PATCH", body: JSON.stringify({ motif }) }, token);
