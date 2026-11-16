import { useCallback, useState } from "react";
import { evaluationsApi } from "../api/endpoints";
import { errorMessage } from "../api/http";
import usePagedSearch from "../hooks/usePagedSearch";
import useStationOptions from "../hooks/useStationOptions";
import { useToast } from "../components/Toast";
import { Alert, Button, ConfirmDialog, PageHeader, Pagination, Stars, Table } from "../components/ui";
import { formatDate } from "../utils/format";

export default function EvaluationsPage() {
  const notify = useToast();
  const stations = useStationOptions();
  const { filters, setFilter, page, setPage, data, loading, error, reload } = usePagedSearch(
    useCallback((p) => evaluationsApi.search(p), []),
    { q: "", stationId: "", note: "" }
  );
  const [toDelete, setToDelete] = useState(null);
  const [busy, setBusy] = useState(false);

  const remove = async () => {
    setBusy(true);
    try {
      await evaluationsApi.remove(toDelete.id);
      notify("Évaluation supprimée, note moyenne recalculée");
      setToDelete(null);
      reload();
    } catch (e) {
      notify(errorMessage(e), "error");
    } finally {
      setBusy(false);
    }
  };

  return (
    <>
      <PageHeader title="Évaluations" subtitle="Modération des avis laissés par les clients" />
      <div className="card">
        <div className="flex flex-wrap gap-3 border-b border-slate-200 p-4">
          <input
            className="input max-w-xs"
            placeholder="Rechercher (commentaire, client, station)…"
            value={filters.q}
            onChange={(e) => setFilter("q", e.target.value)}
          />
          <select className="input w-auto" value={filters.stationId} onChange={(e) => setFilter("stationId", e.target.value)}>
            <option value="">Toutes les stations</option>
            {stations.map((s) => (
              <option key={s.id} value={s.id}>
                {s.nom}
              </option>
            ))}
          </select>
          <select className="input w-auto" value={filters.note} onChange={(e) => setFilter("note", e.target.value)}>
            <option value="">Toutes les notes</option>
            {[5, 4, 3, 2, 1].map((n) => (
              <option key={n} value={n}>
                {n} étoile{n > 1 ? "s" : ""}
              </option>
            ))}
          </select>
        </div>
        {error && <div className="p-4"><Alert>{error}</Alert></div>}
        <Table columns={["Date", "Station", "Client", "Note", "Commentaire", ""]} loading={loading} empty={!data.content.length}>
          {data.content.map((e) => (
            <tr key={e.id} className="hover:bg-slate-50">
              <td className="td whitespace-nowrap">{formatDate(e.dateCreation)}</td>
              <td className="td">{e.stationNom}</td>
              <td className="td">{e.clientNom}</td>
              <td className="td">
                <Stars value={e.note} />
              </td>
              <td className="td max-w-md text-slate-600">{e.commentaire || <em className="text-slate-400">Sans commentaire</em>}</td>
              <td className="td">
                <Button size="sm" variant="danger" onClick={() => setToDelete(e)}>
                  Supprimer
                </Button>
              </td>
            </tr>
          ))}
        </Table>
        <Pagination {...data} page={page} onChange={setPage} />
      </div>

      <ConfirmDialog
        open={Boolean(toDelete)}
        title="Supprimer l'évaluation"
        message={`L'avis de ${toDelete?.clientNom} sur « ${toDelete?.stationNom} » sera supprimé et la note moyenne de la station recalculée.`}
        confirmLabel="Supprimer"
        loading={busy}
        onConfirm={remove}
        onCancel={() => setToDelete(null)}
      />
    </>
  );
}
