import { useCallback, useState } from "react";
import { reservationsApi } from "../api/endpoints";
import { errorMessage } from "../api/http";
import usePagedSearch from "../hooks/usePagedSearch";
import useStationOptions from "../hooks/useStationOptions";
import { useToast } from "../components/Toast";
import { Alert, Button, ConfirmDialog, PageHeader, Pagination, StatusBadge, Table } from "../components/ui";
import { formatDate, formatTime, formatTND, STATUT_RESERVATION, TYPE_VEHICULE } from "../utils/format";

export default function ReservationsPage() {
  const notify = useToast();
  const stations = useStationOptions();
  const { filters, setFilter, page, setPage, data, loading, error, reload } = usePagedSearch(
    useCallback((p) => reservationsApi.search(p), []),
    { q: "", statut: "", stationId: "" }
  );
  const [busyId, setBusyId] = useState(null);
  const [toDelete, setToDelete] = useState(null);

  const changeStatut = async (r, statut) => {
    setBusyId(r.id);
    try {
      await reservationsApi.setStatut(r.id, statut);
      notify(`Réservation n°${r.id} : ${STATUT_RESERVATION[statut].label.toLowerCase()}`);
      reload();
    } catch (e) {
      notify(errorMessage(e), "error");
    } finally {
      setBusyId(null);
    }
  };

  const remove = async () => {
    setBusyId(toDelete.id);
    try {
      await reservationsApi.remove(toDelete.id);
      notify(`Réservation n°${toDelete.id} supprimée`);
      setToDelete(null);
      reload();
    } catch (e) {
      notify(errorMessage(e), "error");
    } finally {
      setBusyId(null);
    }
  };

  return (
    <>
      <PageHeader title="Réservations" subtitle="Suivi et modération des réservations de créneaux" />
      <div className="card">
        <div className="flex flex-wrap gap-3 border-b border-slate-200 p-4">
          <input
            className="input max-w-xs"
            placeholder="Rechercher (client, station)…"
            value={filters.q}
            onChange={(e) => setFilter("q", e.target.value)}
          />
          <select className="input w-auto" value={filters.statut} onChange={(e) => setFilter("statut", e.target.value)}>
            <option value="">Tous les statuts</option>
            {Object.entries(STATUT_RESERVATION).map(([k, v]) => (
              <option key={k} value={k}>
                {v.label}
              </option>
            ))}
          </select>
          <select className="input w-auto" value={filters.stationId} onChange={(e) => setFilter("stationId", e.target.value)}>
            <option value="">Toutes les stations</option>
            {stations.map((s) => (
              <option key={s.id} value={s.id}>
                {s.nom}
              </option>
            ))}
          </select>
        </div>
        {error && <div className="p-4"><Alert>{error}</Alert></div>}
        <Table
          columns={["N°", "Créneau", "Client", "Station", "Prestation", "Prix", "Statut", "Actions"]}
          loading={loading}
          empty={!data.content.length}
        >
          {data.content.map((r) => (
            <tr key={r.id} className="hover:bg-slate-50">
              <td className="td text-slate-500">{r.id}</td>
              <td className="td whitespace-nowrap">
                <p className="font-medium">{formatDate(r.dateDebut)}</p>
                <p className="text-xs text-slate-500">
                  {formatTime(r.dateDebut)} – {formatTime(r.dateFin)}
                </p>
              </td>
              <td className="td">
                <p>{r.clientNom}</p>
                <p className="text-xs text-slate-500">{r.clientTelephone || r.clientEmail}</p>
              </td>
              <td className="td">{r.stationNom}</td>
              <td className="td">
                <p>{r.service}</p>
                <p className="text-xs text-slate-500">{TYPE_VEHICULE[r.typeVehicule]}</p>
              </td>
              <td className="td whitespace-nowrap">{formatTND(r.prix)}</td>
              <td className="td">
                <StatusBadge map={STATUT_RESERVATION} value={r.statut} />
              </td>
              <td className="td">
                <div className="flex flex-wrap gap-1.5">
                  {r.statut !== "ACCEPTEE" && (
                    <Button size="sm" variant="success" disabled={busyId === r.id} onClick={() => changeStatut(r, "ACCEPTEE")}>
                      Accepter
                    </Button>
                  )}
                  {r.statut !== "REFUSEE" && (
                    <Button size="sm" variant="secondary" disabled={busyId === r.id} onClick={() => changeStatut(r, "REFUSEE")}>
                      Refuser
                    </Button>
                  )}
                  {r.statut !== "EN_ATTENTE" && (
                    <Button size="sm" variant="ghost" disabled={busyId === r.id} onClick={() => changeStatut(r, "EN_ATTENTE")}>
                      Remettre en attente
                    </Button>
                  )}
                  <Button size="sm" variant="danger" disabled={busyId === r.id} onClick={() => setToDelete(r)}>
                    Supprimer
                  </Button>
                </div>
              </td>
            </tr>
          ))}
        </Table>
        <Pagination {...data} page={page} onChange={setPage} />
      </div>

      <ConfirmDialog
        open={Boolean(toDelete)}
        title="Supprimer la réservation"
        message={`La réservation n°${toDelete?.id} de ${toDelete?.clientNom} sera définitivement supprimée.`}
        confirmLabel="Supprimer"
        loading={busyId === toDelete?.id}
        onConfirm={remove}
        onCancel={() => setToDelete(null)}
      />
    </>
  );
}
