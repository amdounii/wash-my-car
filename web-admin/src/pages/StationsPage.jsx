import { useCallback, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { stationsApi } from "../api/endpoints";
import { errorMessage, fieldErrors } from "../api/http";
import usePagedSearch from "../hooks/usePagedSearch";
import { useToast } from "../components/Toast";
import {
  Alert, Button, ConfirmDialog, Field, Modal, PageHeader, Pagination, Spinner, Stars, StatusBadge, Table,
} from "../components/ui";
import { formatTND, JOURS, STATUT_INSCRIPTION, TYPE_VEHICULE } from "../utils/format";

const EMPTY_FORM = {
  nom: "", prenom: "", email: "", motDePasse: "", telephone: "", statutInscription: "VALIDEE",
  station: { nom: "", adresse: "", ville: "", latitude: "", longitude: "", telephone: "", description: "", photoUrl: "" },
};

export default function StationsPage() {
  const notify = useToast();
  const [searchParams] = useSearchParams();
  const { filters, setFilter, page, setPage, data, loading, error, reload } = usePagedSearch(
    useCallback((p) => stationsApi.search(p), []),
    { q: "", statut: searchParams.get("statut") || "" }
  );

  const [form, setForm] = useState(null); // { mode: "create" | "edit", id?, values }
  const [details, setDetails] = useState(null);
  const [confirm, setConfirm] = useState(null); // { action, station }
  const [busy, setBusy] = useState(false);

  const openDetails = async (station) => {
    setDetails({ station, data: null });
    try {
      setDetails({ station, data: await stationsApi.details(station.id) });
    } catch (e) {
      notify(errorMessage(e), "error");
      setDetails(null);
    }
  };

  const runConfirm = async () => {
    const { action, station } = confirm;
    setBusy(true);
    try {
      if (action === "delete") {
        await stationsApi.remove(station.id);
        notify(`Station « ${station.nom} » supprimée`);
      } else {
        await stationsApi.setStatut(station.id, action);
        notify(`Station « ${station.nom} » ${action === "VALIDEE" ? "validée" : "refusée"}`);
      }
      setConfirm(null);
      reload();
    } catch (e) {
      notify(errorMessage(e), "error");
    } finally {
      setBusy(false);
    }
  };

  return (
    <>
      <PageHeader
        title="Stations de lavage"
        subtitle="Inscriptions, informations et validation des stations"
        action={<Button onClick={() => setForm({ mode: "create", values: EMPTY_FORM })}>+ Nouvelle station</Button>}
      />

      <div className="card">
        <div className="flex flex-wrap gap-3 border-b border-slate-200 p-4">
          <input
            className="input max-w-xs"
            placeholder="Rechercher (nom, ville, adresse, email)…"
            value={filters.q}
            onChange={(e) => setFilter("q", e.target.value)}
          />
          <select className="input w-auto" value={filters.statut} onChange={(e) => setFilter("statut", e.target.value)}>
            <option value="">Tous les statuts</option>
            {Object.entries(STATUT_INSCRIPTION).map(([k, v]) => (
              <option key={k} value={k}>
                {v.label}
              </option>
            ))}
          </select>
        </div>
        {error && <div className="p-4"><Alert>{error}</Alert></div>}

        <Table columns={["Station", "Gérant", "Note", "Statut", "Actions"]} loading={loading} empty={!data.content.length}>
          {data.content.map((s) => (
            <tr key={s.id} className="hover:bg-slate-50">
              <td className="td">
                <p className="font-medium text-slate-900">{s.nom}</p>
                <p className="text-xs text-slate-500">
                  {s.adresse}, {s.ville}
                </p>
              </td>
              <td className="td">
                <p>
                  {s.proprietaire.prenom} {s.proprietaire.nom}
                </p>
                <p className="text-xs text-slate-500">{s.proprietaire.email}</p>
              </td>
              <td className="td">
                <Stars value={s.noteMoyenne} /> <span className="text-xs text-slate-500">{s.noteMoyenne}</span>
              </td>
              <td className="td">
                <StatusBadge map={STATUT_INSCRIPTION} value={s.statutInscription} />
              </td>
              <td className="td">
                <div className="flex flex-wrap gap-1.5">
                  {s.statutInscription !== "VALIDEE" && (
                    <Button size="sm" variant="success" onClick={() => setConfirm({ action: "VALIDEE", station: s })}>
                      Valider
                    </Button>
                  )}
                  {s.statutInscription !== "REFUSEE" && (
                    <Button size="sm" variant="secondary" onClick={() => setConfirm({ action: "REFUSEE", station: s })}>
                      Refuser
                    </Button>
                  )}
                  <Button size="sm" variant="secondary" onClick={() => openDetails(s)}>
                    Fiche
                  </Button>
                  <Button size="sm" variant="secondary" onClick={() => setForm({ mode: "edit", id: s.id, values: toForm(s) })}>
                    Modifier
                  </Button>
                  <Button size="sm" variant="danger" onClick={() => setConfirm({ action: "delete", station: s })}>
                    Supprimer
                  </Button>
                </div>
              </td>
            </tr>
          ))}
        </Table>
        <Pagination {...data} page={page} onChange={setPage} />
      </div>

      {form && (
        <StationFormModal
          form={form}
          onClose={() => setForm(null)}
          onSaved={(msg) => {
            notify(msg);
            setForm(null);
            reload();
          }}
        />
      )}

      <StationDetailsModal details={details} onClose={() => setDetails(null)} />

      <ConfirmDialog
        open={Boolean(confirm)}
        title={
          confirm?.action === "delete" ? "Supprimer la station" : confirm?.action === "VALIDEE" ? "Valider l'inscription" : "Refuser l'inscription"
        }
        message={
          confirm?.action === "delete"
            ? `La station « ${confirm?.station.nom} », le compte de son gérant, ses réservations et ses évaluations seront définitivement supprimés.`
            : `Confirmer le changement de statut de « ${confirm?.station.nom} » ? Le gérant sera prévenu par email.`
        }
        confirmLabel={confirm?.action === "delete" ? "Supprimer" : "Confirmer"}
        danger={confirm?.action !== "VALIDEE"}
        loading={busy}
        onConfirm={runConfirm}
        onCancel={() => setConfirm(null)}
      />
    </>
  );
}

/** Convertit une station de l'API en valeurs de formulaire (modification). */
function toForm(s) {
  return {
    ...EMPTY_FORM,
    station: {
      nom: s.nom, adresse: s.adresse, ville: s.ville, latitude: s.latitude, longitude: s.longitude,
      telephone: s.telephone || "", description: s.description || "", photoUrl: s.photoUrl || "",
    },
  };
}

function StationFormModal({ form, onClose, onSaved }) {
  const [values, setValues] = useState(form.values);
  const [errors, setErrors] = useState({});
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);
  const isCreate = form.mode === "create";

  const set = (name) => (e) => setValues((v) => ({ ...v, [name]: e.target.value }));
  const setStation = (name) => (e) => setValues((v) => ({ ...v, station: { ...v.station, [name]: e.target.value } }));
  // Erreurs du gérant : « nom » ; erreurs de la station : « station.nom » en création, « nom » en modification.
  const err = (name) => errors[name];
  const errStation = (name) => (isCreate ? errors[`station.${name}`] : errors[name]);

  const submit = async (e) => {
    e.preventDefault();
    setSaving(true);
    setErrors({});
    setError("");
    const station = {
      ...values.station,
      latitude: values.station.latitude === "" ? null : Number(values.station.latitude),
      longitude: values.station.longitude === "" ? null : Number(values.station.longitude),
    };
    try {
      if (isCreate) {
        await stationsApi.create({ ...values, station });
        onSaved(`Station « ${station.nom} » créée`);
      } else {
        await stationsApi.update(form.id, station);
        onSaved(`Station « ${station.nom} » mise à jour`);
      }
    } catch (e2) {
      setErrors(fieldErrors(e2));
      setError(errorMessage(e2));
    } finally {
      setSaving(false);
    }
  };

  return (
    <Modal
      open
      wide
      title={isCreate ? "Nouvelle station" : "Modifier la station"}
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Annuler
          </Button>
          <Button type="submit" form="station-form" loading={saving}>
            Enregistrer
          </Button>
        </>
      }
    >
      <form id="station-form" onSubmit={submit} className="space-y-6">
        <Alert>{error}</Alert>
        {isCreate && (
          <fieldset>
            <legend className="mb-3 font-semibold text-slate-900">Compte du gérant</legend>
            <div className="grid gap-4 sm:grid-cols-2">
              <Field label="Nom *" error={err("nom")}>
                <input className="input" value={values.nom} onChange={set("nom")} required />
              </Field>
              <Field label="Prénom *" error={err("prenom")}>
                <input className="input" value={values.prenom} onChange={set("prenom")} required />
              </Field>
              <Field label="Email *" error={err("email")}>
                <input type="email" className="input" value={values.email} onChange={set("email")} required />
              </Field>
              <Field label="Mot de passe * (8 caractères min.)" error={err("motDePasse")}>
                <input type="password" className="input" value={values.motDePasse} onChange={set("motDePasse")} minLength={8} required />
              </Field>
              <Field label="Téléphone" error={err("telephone")}>
                <input className="input" value={values.telephone} onChange={set("telephone")} placeholder="22123456" />
              </Field>
              <Field label="Statut initial">
                <select className="input" value={values.statutInscription} onChange={set("statutInscription")}>
                  {Object.entries(STATUT_INSCRIPTION).map(([k, v]) => (
                    <option key={k} value={k}>
                      {v.label}
                    </option>
                  ))}
                </select>
              </Field>
            </div>
          </fieldset>
        )}

        <fieldset>
          <legend className="mb-3 font-semibold text-slate-900">Station</legend>
          <div className="grid gap-4 sm:grid-cols-2">
            <Field label="Nom de la station *" error={errStation("nom")} className="sm:col-span-2">
              <input className="input" value={values.station.nom} onChange={setStation("nom")} required />
            </Field>
            <Field label="Adresse *" error={errStation("adresse")}>
              <input className="input" value={values.station.adresse} onChange={setStation("adresse")} required />
            </Field>
            <Field label="Ville *" error={errStation("ville")}>
              <input className="input" value={values.station.ville} onChange={setStation("ville")} required />
            </Field>
            <Field label="Latitude *" error={errStation("latitude")}>
              <input type="number" step="any" min="-90" max="90" className="input" value={values.station.latitude} onChange={setStation("latitude")} placeholder="36.8065" required />
            </Field>
            <Field label="Longitude *" error={errStation("longitude")}>
              <input type="number" step="any" min="-180" max="180" className="input" value={values.station.longitude} onChange={setStation("longitude")} placeholder="10.1815" required />
            </Field>
            <Field label="Téléphone de la station" error={errStation("telephone")}>
              <input className="input" value={values.station.telephone} onChange={setStation("telephone")} />
            </Field>
            <Field label="URL de la photo" error={errStation("photoUrl")}>
              <input type="url" className="input" value={values.station.photoUrl} onChange={setStation("photoUrl")} placeholder="https://…" />
            </Field>
            <Field label="Description" error={errStation("description")} className="sm:col-span-2">
              <textarea className="input" rows={3} maxLength={1000} value={values.station.description} onChange={setStation("description")} />
            </Field>
          </div>
          <p className="mt-2 text-xs text-slate-500">
            Astuce : clic droit sur un point dans Google Maps ou OpenStreetMap pour copier ses coordonnées.
          </p>
        </fieldset>
      </form>
    </Modal>
  );
}

function StationDetailsModal({ details, onClose }) {
  if (!details) return null;
  const { station, data } = details;
  return (
    <Modal open wide title={station.nom} onClose={onClose}>
      {!data ? (
        <div className="flex justify-center py-8">
          <Spinner />
        </div>
      ) : (
        <div className="space-y-6">
          <div className="grid gap-4 text-sm sm:grid-cols-2">
            <div>
              <p className="text-slate-500">Adresse</p>
              <p>
                {data.adresse}, {data.ville}
              </p>
              <a
                className="text-brand-600 hover:underline"
                href={`https://www.openstreetmap.org/?mlat=${data.latitude}&mlon=${data.longitude}#map=17/${data.latitude}/${data.longitude}`}
                target="_blank"
                rel="noreferrer"
              >
                Voir sur la carte ({data.latitude}, {data.longitude})
              </a>
            </div>
            <div>
              <p className="text-slate-500">Note</p>
              <p>
                <Stars value={data.noteMoyenne} /> {data.noteMoyenne} / 5 ({data.nbEvaluations} avis)
              </p>
              <p className="mt-1 text-slate-500">{data.ouverteMaintenant ? "🟢 Ouverte actuellement" : "🔴 Fermée actuellement"}</p>
            </div>
            {data.description && <p className="text-slate-600 sm:col-span-2">{data.description}</p>}
          </div>

          <div>
            <h3 className="mb-2 font-semibold">Horaires</h3>
            <ul className="grid gap-1 text-sm sm:grid-cols-2">
              {data.horaires.map((h) => (
                <li key={h.jourSemaine} className="flex justify-between rounded bg-slate-50 px-3 py-1.5">
                  <span>{JOURS[h.jourSemaine]}</span>
                  <span className={h.ferme ? "text-red-600" : ""}>
                    {h.ferme ? "Fermé" : `${h.heureOuverture} – ${h.heureFermeture}`}
                  </span>
                </li>
              ))}
            </ul>
          </div>

          <div>
            <h3 className="mb-2 font-semibold">Tarifs</h3>
            {data.tarifs.length === 0 ? (
              <p className="text-sm text-slate-500">Aucun tarif renseigné.</p>
            ) : (
              <table className="min-w-full text-sm">
                <thead>
                  <tr className="text-left text-slate-500">
                    <th className="py-1">Véhicule</th>
                    <th>Service</th>
                    <th>Durée</th>
                    <th className="text-right">Prix</th>
                  </tr>
                </thead>
                <tbody>
                  {data.tarifs.map((t) => (
                    <tr key={t.id} className="border-t border-slate-100">
                      <td className="py-1.5">{TYPE_VEHICULE[t.typeVehicule]}</td>
                      <td>{t.service}</td>
                      <td>{t.dureeMinutes} min</td>
                      <td className="text-right font-medium">{formatTND(t.prix)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        </div>
      )}
    </Modal>
  );
}
