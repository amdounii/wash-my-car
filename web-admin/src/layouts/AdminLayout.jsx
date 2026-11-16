import { useState } from "react";
import { NavLink, Outlet, useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";

const NAV = [
  { to: "/", label: "Tableau de bord", icon: "▦", end: true },
  { to: "/stations", label: "Stations", icon: "⛽" },
  { to: "/clients", label: "Clients", icon: "👤" },
  { to: "/reservations", label: "Réservations", icon: "📅" },
  { to: "/evaluations", label: "Évaluations", icon: "★" },
];

/** Barre latérale (repliable sur mobile) + en-tête + contenu de la page. */
export default function AdminLayout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);

  const handleLogout = () => {
    logout();
    navigate("/login", { replace: true });
  };

  return (
    <div className="min-h-screen lg:flex">
      <aside
        className={`fixed inset-y-0 left-0 z-40 w-64 transform bg-brand-900 text-white transition-transform
          lg:static lg:translate-x-0 ${open ? "translate-x-0" : "-translate-x-full"}`}
      >
        <div className="flex h-16 items-center gap-2 px-6 text-lg font-bold">
          <span className="text-2xl">🚿</span> Wash My Car
        </div>
        <nav className="mt-4 space-y-1 px-3">
          {NAV.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.end}
              onClick={() => setOpen(false)}
              className={({ isActive }) =>
                `flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition ${
                  isActive ? "bg-white/15 text-white" : "text-brand-100 hover:bg-white/10"
                }`
              }
            >
              <span className="w-5 text-center">{item.icon}</span>
              {item.label}
            </NavLink>
          ))}
        </nav>
      </aside>

      {open && <div className="fixed inset-0 z-30 bg-slate-900/40 lg:hidden" onClick={() => setOpen(false)} />}

      <div className="flex min-w-0 flex-1 flex-col">
        <header className="flex h-16 items-center justify-between border-b border-slate-200 bg-white px-4 sm:px-6">
          <button className="text-2xl lg:hidden" onClick={() => setOpen(true)} aria-label="Ouvrir le menu">
            ☰
          </button>
          <div className="ml-auto flex items-center gap-4">
            <span className="hidden text-sm text-slate-600 sm:inline">
              {user?.prenom} {user?.nom}
            </span>
            <button onClick={handleLogout} className="text-sm font-medium text-red-600 hover:text-red-700">
              Déconnexion
            </button>
          </div>
        </header>
        <main className="flex-1 p-4 sm:p-6 lg:p-8">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
