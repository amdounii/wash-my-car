import http from "./http";

/** Supprime les filtres vides pour ne pas envoyer « ?statut= » à l'API. */
const clean = (params) =>
  Object.fromEntries(Object.entries(params).filter(([, v]) => v !== "" && v !== null && v !== undefined));

export const authApi = {
  login: (email, motDePasse) => http.post("/auth/login", { email, motDePasse }).then((r) => r.data),
  profil: () => http.get("/profil").then((r) => r.data),
};

export const dashboardApi = {
  get: () => http.get("/admin/dashboard").then((r) => r.data),
};

export const stationsApi = {
  search: (params) => http.get("/admin/stations", { params: clean(params) }).then((r) => r.data),
  details: (id) => http.get(`/admin/stations/${id}/details`).then((r) => r.data),
  create: (body) => http.post("/admin/stations", body).then((r) => r.data),
  update: (id, body) => http.put(`/admin/stations/${id}`, body).then((r) => r.data),
  setStatut: (id, statut) => http.patch(`/admin/stations/${id}/statut`, { statut }).then((r) => r.data),
  remove: (id) => http.delete(`/admin/stations/${id}`),
};

export const clientsApi = {
  search: (params) => http.get("/admin/clients", { params: clean(params) }).then((r) => r.data),
  create: (body) => http.post("/admin/clients", body).then((r) => r.data),
  update: (id, body) => http.put(`/admin/clients/${id}`, body).then((r) => r.data),
  remove: (id) => http.delete(`/admin/clients/${id}`),
};

export const reservationsApi = {
  search: (params) => http.get("/admin/reservations", { params: clean(params) }).then((r) => r.data),
  setStatut: (id, statut) => http.patch(`/admin/reservations/${id}/statut`, { statut }).then((r) => r.data),
  remove: (id) => http.delete(`/admin/reservations/${id}`),
};

export const evaluationsApi = {
  search: (params) => http.get("/admin/evaluations", { params: clean(params) }).then((r) => r.data),
  remove: (id) => http.delete(`/admin/evaluations/${id}`),
};
