import { useState } from "react";
import { login } from "./api";

export default function Login({ onLogin }) {
  const [email, setEmail] = useState("");
  const [motDePasse, setMotDePasse] = useState("");
  const [error, setError] = useState("");

  async function submit(e) {
    e.preventDefault();
    setError("");
    try {
      const { jeton, role } = await login(email.trim(), motDePasse);
      if (role !== "ADMIN") {
        setError("Accès réservé à l'administrateur.");
        return;
      }
      onLogin(jeton);
    } catch (err) {
      setError(err.message);
    }
  }

  return (
    <form className="card login" onSubmit={submit}>
      <h2>Administration</h2>
      <input type="email" placeholder="E-mail" value={email} onChange={(e) => setEmail(e.target.value)} required />
      <input type="password" placeholder="Mot de passe" value={motDePasse} onChange={(e) => setMotDePasse(e.target.value)} required />
      {error && <div className="error">{error}</div>}
      <button type="submit">Se connecter</button>
    </form>
  );
}
