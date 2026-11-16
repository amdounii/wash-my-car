import { useState } from "react";
import Login from "./Login";
import Stations from "./Stations";

export default function App() {
  const [token, setToken] = useState(sessionStorage.getItem("jeton"));

  function handleLogin(t) {
    sessionStorage.setItem("jeton", t);
    setToken(t);
  }

  function handleLogout() {
    sessionStorage.removeItem("jeton");
    setToken(null);
  }

  return token ? <Stations token={token} onLogout={handleLogout} /> : <Login onLogin={handleLogin} />;
}
