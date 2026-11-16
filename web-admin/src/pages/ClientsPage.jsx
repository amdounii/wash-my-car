import { useCallback, useState } from "react";
import { clientsApi } from "../api/endpoints";
import { errorMessage, fieldErrors } from "../api/http";
import usePagedSearch from "../hooks/usePagedSearch";
import { useToast } from "../components/Toast";
import { Alert, Badge, Button, ConfirmDialog, Field, Modal, PageHeader, Pagination, Table } from "../components/ui";
import { formatDate } from "../utils/format";

const EMPTY = { nom: "", prenom: "", email: "", motDePasse: "", telephone: "", emailVerifie: true };

export default function ClientsPage() {
  const notify = useToast();
  const { filters, setFilter, page, setPage, data, loading, error, reload } = usePagedSearch(
    useCallback((p) => clientsApi.search(p), []),
    { q: "" }
  );
  const [form, setForm] = useState(null); // { id?, values }
  const [toDelete, setToDelete] = useState(null);
  const [busy, setBusy] = useState(false);

  const remove = async () => {
    setBusy(true);
    try {
      await clientsApi.remove(toDelete.id);
      notify(`Client ${toDelete.prenom} ${toDelete.nom} supprimé`);
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
      <PageHeader
        title="Clients"
        subtitle="Comptes clients de l'application mobile"
        action={<Button onClick={() => setForm({ values: EMPTY })}>+ Nouveau client</Button>}
      />
      <div className="card">
        <div className="border-b border-slate-200 p-4">
          <input
            className="input max-w-xs"
            placeholder="Rechercher (nom, email, téléphone)…"
            value={filters.q}
            onChange={(e) => setFilter("q", e.target.value)}
          />
        </div>
        {error && <div className="p-4"><Alert>{error}</Alert></div>}
        <Table columns={["Client", "Téléphone", "Compte", "Inscription", "Actions"]} loading={loading} empty={!data.content.length}>
          {data.content.map((c) => (
            <tr key={c.id} className="hover:bg-slate-50">
              <td className="td">
                <p className="font-medium text-slate-900">
                  {c.prenom} {c.nom}
                </p>
                <p className="text-xs text-slate-500">{c.email}</p>
              </td>
              <td className="td">{c.telephone || "—"}</td>
              <td className="td">
                {c.emailVerifie ? <Badge color="green">Validé</Badge> : <Badge color="amber">Email non validé</Badge>}
              </td>
              <td className="td">{formatDate(c.dateCreation)}</td>
              <td className="td">
                <div className="flex gap-1.5">
                  <Button
                    size="sm"
                    variant="secondary"
                    onClick={() =>
                      setForm({
                        id: c.id,
                        values: { ...EMPTY, ...c, telephone: c.telephone || "", motDePasse: "" },
                      })
                    }
                  >
                    Modifier
                  </Button>
                  <Button size="sm" variant="danger" onClick={() => setToDelete(c)}>
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
        <ClientFormModal
          form={form}
          onClose={() => setForm(null)}
          onSaved={(msg) => {
            notify(msg);
            setForm(null);
            reload();
          }}
        />
      )}

      <ConfirmDialog
        open={Boolean(toDelete)}
        title="Supprimer le client"
        message={`Le compte de ${toDelete?.prenom} ${toDelete?.nom}, ses réservations et ses évaluations seront définitivement supprimés.`}
        confirmLabel="Supprimer"
        loading={busy}
        onConfirm={remove}
        onCancel={() => setToDelete(null)}
      />
    </>
  );
}

function ClientFormModal({ form, onClose, onSaved }) {
  const isCreate = !form.id;
  const [values, setValues] = useState(form.values);
  const [errors, setErrors] = useState({});
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);
  const set = (name) => (e) =>
    setValues((v) => ({ ...v, [name]: e.target.type === "checkbox" ? e.target.checked : e.target.value }));

  const submit = async (e) => {
    e.preventDefault();
    setSaving(true);
    setErrors({});
    setError("");
    const body = {
      nom: values.nom,
      prenom: values.prenom,
      email: values.email,
      telephone: values.telephone,
      emailVerifie: values.emailVerifie,
      motDePasse: values.motDePasse || null, // vide = mot de passe inchangé
    };
    try {
      if (isCreate) await clientsApi.create(body);
      else await clientsApi.update(form.id, body);
      onSaved(isCreate ? "Client créé" : "Client mis à jour");
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
      title={isCreate ? "Nouveau client" : "Modifier le client"}
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Annuler
          </Button>
          <Button type="submit" form="client-form" loading={saving}>
            Enregistrer
          </Button>
        </>
      }
    >
      <form id="client-form" onSubmit={submit} className="grid gap-4 sm:grid-cols-2">
        <div className="sm:col-span-2">
          <Alert>{error}</Alert>
        </div>
        <Field label="Nom *" error={errors.nom}>
          <input className="input" value={values.nom} onChange={set("nom")} required />
        </Field>
        <Field label="Prénom *" error={errors.prenom}>
          <input className="input" value={values.prenom} onChange={set("prenom")} required />
        </Field>
        <Field label="Email *" error={errors.email} className="sm:col-span-2">
          <input type="email" className="input" value={values.email} onChange={set("email")} required />
        </Field>
        <Field label="Téléphone" error={errors.telephone}>
          <input className="input" value={values.telephone} onChange={set("telephone")} placeholder="22123456" />
        </Field>
        <Field label={isCreate ? "Mot de passe *" : "Nouveau mot de passe"} error={errors.motDePasse}>
          <input
            type="password"
            className="input"
            value={values.motDePasse}
            onChange={set("motDePasse")}
            minLength={8}
            required={isCreate}
            placeholder={isCreate ? "" : "Laisser vide pour ne pas changer"}
          />
        </Field>
        <label className="flex items-center gap-2 text-sm sm:col-span-2">
          <input type="checkbox" checked={values.emailVerifie} onChange={set("emailVerifie")} className="h-4 w-4" />
          Compte validé (le client peut se connecter)
        </label>
      </form>
    </Modal>
  );
}
