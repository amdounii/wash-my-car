import { Link, Route, Routes } from "react-router-dom";
import ProtectedRoute from "./auth/ProtectedRoute";
import AdminLayout from "./layouts/AdminLayout";
import LoginPage from "./pages/LoginPage";
import DashboardPage from "./pages/DashboardPage";
import StationsPage from "./pages/StationsPage";
import ClientsPage from "./pages/ClientsPage";
import ReservationsPage from "./pages/ReservationsPage";
import EvaluationsPage from "./pages/EvaluationsPage";

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route element={<ProtectedRoute />}>
        <Route element={<AdminLayout />}>
          <Route index element={<DashboardPage />} />
          <Route path="stations" element={<StationsPage />} />
          <Route path="clients" element={<ClientsPage />} />
          <Route path="reservations" element={<ReservationsPage />} />
          <Route path="evaluations" element={<EvaluationsPage />} />
          <Route path="*" element={<NotFound />} />
        </Route>
      </Route>
    </Routes>
  );
}

function NotFound() {
  return (
    <div className="py-20 text-center">
      <h1 className="text-3xl font-bold">Page introuvable</h1>
      <Link to="/" className="mt-4 inline-block text-brand-600 hover:underline">
        Retour au tableau de bord
      </Link>
    </div>
  );
}
