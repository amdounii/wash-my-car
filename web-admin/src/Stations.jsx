import { useCallback, useEffect, useState } from "react";
import { approve, getPending, reject } from "./api";

export default function Stations({ token, onLogout }) {
  const [stations, setStations] = useState([]);
  const [error, setError] = useState("");
  const [rejectingId, setRejectingId] = useState(null);
  const [motif, setMotif] = useState("");

  const handleError = useCallback(
    (err) => {
      if (err.status === 401) onLogout();
      else setError(err.message);
    },
    [onLogout]
  );

  const load = useCallback(async () => {
    try {
      setError("");
      setStations(await getPending(token));
    } catch (err) {
      handleError(err);
    }
  }, [token, handleError]);

  useEffect(() => {
    load();
  }, [load]);

  async function onApprove(id) {
    try {
      await approve(id, token);
      load();
    } catch (err) {
      handleError(err);
    }
  }

  async function onReject(id) {
    if (!motif.trim()) {
      setError("Le motif de refus est obligatoire.");
      return;
    }
    try {
      await reject(id, motif, token);
      setRejectingId(null);
      setMotif("");
      load();
    } catch (err) {
      handleError(err);
    }
  }

  return (
    <div className="container">
      <div className="header">
        <h2>Demandes d'inscription des stations</h2>
        <button className="ghost" onClick={onLogout}>Déconnexion</button>
      </div>
      {error && <div className="error">{error}</div>}
      <div className="card">
        {stations.length === 0 ? (
          <p>Aucune demande en attente.</p>
        ) : (
          <table>
            <thead>
              <tr><th>Station</th><th>Adresse</th><th>Responsable</th><th>Contact</th><th>Actions</th></tr>
            </thead>
            <tbody>
              {stations.map((s) => (
                <tr key={s.id}>
                  <td>{s.nomStation}</td>
                  <td>{s.adresse}</td>
                  <td>{s.responsablePrenom} {s.responsableNom}</td>
                  <td>{s.email}<br />{s.telephone}</td>
                  <td className="actions">
                    {rejectingId === s.id ? (
                      <>
                        <input placeholder="Motif du refus" value={motif} onChange={(e) => setMotif(e.target.value)} />
                        <button className="danger" onClick={() => onReject(s.id)}>Confirmer</button>
                        <button className="ghost" onClick={() => { setRejectingId(null); setMotif(""); }}>Annuler</button>
                      </>
                    ) : (
                      <>
                        <button onClick={() => onApprove(s.id)}>Valider</button>
                        <button className="danger" onClick={() => setRejectingId(s.id)}>Refuser</button>
                      </>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
