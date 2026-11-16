import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { dashboardApi } from "../api/endpoints";
import { errorMessage } from "../api/http";
import { Alert, PageHeader, Spinner } from "../components/ui";

export default function DashboardPage() {
  const [stats, setStats] = useState(null);
  const [error, setError] = useState("");

  useEffect(() => {
    dashboardApi.get().then(setStats).catch((e) => setError(errorMessage(e)));
  }, []);

  if (error) return <Alert>{error}</Alert>;
  if (!stats) {
    return (
      <div className="flex justify-center py-20">
        <Spinner />
      </div>
    );
  }

  const totalReservations =
    stats.reservationsEnAttente + stats.reservationsAcceptees + stats.reservationsRefusees + stats.reservationsAnnulees;

  return (
    <>
      <PageHeader title="Tableau de bord" subtitle="Vue d'ensemble de la plateforme" />

      {stats.stationsEnAttente > 0 && (
        <Link
          to="/stations?statut=EN_ATTENTE"
          className="mb-6 block rounded-xl bg-amber-50 px-5 py-4 text-sm text-amber-800 ring-1 ring-amber-200 hover:bg-amber-100"
        >
          <strong>{stats.stationsEnAttente}</strong> inscription{stats.stationsEnAttente > 1 ? "s" : ""} de station en
          attente de validation →
        </Link>
      )}

      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <StatCard label="Clients" value={stats.nbClients} to="/clients" />
        <StatCard label="Stations" value={stats.nbStations} to="/stations" />
        <StatCard label="Réservations" value={totalReservations} to="/reservations" />
        <StatCard label="Évaluations" value={stats.nbEvaluations} to="/evaluations" />
      </div>

      <div className="mt-6 grid gap-6 lg:grid-cols-2">
        <Breakdown
          title="Stations par statut"
          total={stats.nbStations}
          rows={[
            { label: "Validées", value: stats.stationsValidees, color: "bg-green-500" },
            { label: "En attente", value: stats.stationsEnAttente, color: "bg-amber-400" },
            { label: "Refusées", value: stats.stationsRefusees, color: "bg-red-500" },
          ]}
        />
        <Breakdown
          title="Réservations par statut"
          total={totalReservations}
          rows={[
            { label: "En attente", value: stats.reservationsEnAttente, color: "bg-amber-400" },
            { label: "Acceptées", value: stats.reservationsAcceptees, color: "bg-green-500" },
            { label: "Refusées", value: stats.reservationsRefusees, color: "bg-red-500" },
            { label: "Annulées", value: stats.reservationsAnnulees, color: "bg-slate-400" },
          ]}
        />
      </div>
    </>
  );
}

function StatCard({ label, value, to }) {
  return (
    <Link to={to} className="card block p-5 transition hover:shadow-md">
      <p className="text-sm text-slate-500">{label}</p>
      <p className="mt-2 text-3xl font-bold text-slate-900">{value}</p>
    </Link>
  );
}

/** Répartition en barres horizontales proportionnelles au total. */
function Breakdown({ title, total, rows }) {
  return (
    <div className="card p-5">
      <h2 className="mb-4 font-semibold">{title}</h2>
      <div className="space-y-3">
        {rows.map((r) => (
          <div key={r.label}>
            <div className="mb-1 flex justify-between text-sm">
              <span>{r.label}</span>
              <span className="font-medium">{r.value}</span>
            </div>
            <div className="h-2 rounded-full bg-slate-100">
              <div
                className={`h-2 rounded-full ${r.color}`}
                style={{ width: `${total ? (r.value / total) * 100 : 0}%` }}
              />
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
