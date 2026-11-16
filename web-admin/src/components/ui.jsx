/** Petits composants d'interface réutilisables (Tailwind). */
import { useEffect } from "react";

const BUTTON_VARIANTS = {
  primary: "bg-brand-600 text-white hover:bg-brand-700 disabled:bg-brand-500/60",
  secondary: "bg-white text-slate-700 ring-1 ring-slate-300 hover:bg-slate-50",
  danger: "bg-red-600 text-white hover:bg-red-700 disabled:bg-red-400",
  success: "bg-green-600 text-white hover:bg-green-700 disabled:bg-green-400",
  ghost: "text-slate-600 hover:bg-slate-100",
};

export function Button({ variant = "primary", size = "md", className = "", loading = false, children, ...props }) {
  const sizes = { sm: "px-2.5 py-1.5 text-xs", md: "px-4 py-2 text-sm" };
  return (
    <button
      className={`inline-flex items-center justify-center gap-2 rounded-lg font-medium transition
        disabled:cursor-not-allowed ${BUTTON_VARIANTS[variant]} ${sizes[size]} ${className}`}
      disabled={loading || props.disabled}
      {...props}
    >
      {loading && <Spinner small />}
      {children}
    </button>
  );
}

const BADGE_COLORS = {
  green: "bg-green-100 text-green-800",
  red: "bg-red-100 text-red-800",
  amber: "bg-amber-100 text-amber-800",
  slate: "bg-slate-200 text-slate-700",
  blue: "bg-brand-100 text-brand-700",
};

export function Badge({ color = "slate", children }) {
  return (
    <span className={`inline-flex rounded-full px-2.5 py-0.5 text-xs font-semibold ${BADGE_COLORS[color]}`}>
      {children}
    </span>
  );
}

/** Badge à partir d'une table de statuts ({ CODE: { label, color } }). */
export function StatusBadge({ map, value }) {
  const s = map[value] || { label: value, color: "slate" };
  return <Badge color={s.color}>{s.label}</Badge>;
}

export function Spinner({ small = false }) {
  return (
    <span
      className={`inline-block animate-spin rounded-full border-2 border-current border-t-transparent
        ${small ? "h-4 w-4" : "h-8 w-8 text-brand-600"}`}
      role="status"
      aria-label="Chargement"
    />
  );
}

export function Field({ label, error, children, className = "" }) {
  return (
    <div className={className}>
      {label && <label className="label">{label}</label>}
      {children}
      {error && <p className="mt-1 text-xs text-red-600">{error}</p>}
    </div>
  );
}

export function Alert({ type = "error", children }) {
  if (!children) return null;
  const styles = type === "error" ? "bg-red-50 text-red-700 ring-red-200" : "bg-green-50 text-green-700 ring-green-200";
  return <div className={`rounded-lg px-4 py-3 text-sm ring-1 ${styles}`}>{children}</div>;
}

export function Modal({ open, title, onClose, children, footer, wide = false }) {
  useEffect(() => {
    if (!open) return undefined;
    const onKey = (e) => e.key === "Escape" && onClose();
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [open, onClose]);

  if (!open) return null;
  return (
    <div className="fixed inset-0 z-50 flex items-start justify-center overflow-y-auto bg-slate-900/50 p-4 sm:p-8">
      <div className={`card w-full ${wide ? "max-w-3xl" : "max-w-lg"}`} role="dialog" aria-modal="true">
        <div className="flex items-center justify-between border-b border-slate-200 px-6 py-4">
          <h2 className="text-lg font-semibold">{title}</h2>
          <button onClick={onClose} className="text-2xl leading-none text-slate-400 hover:text-slate-600" aria-label="Fermer">
            ×
          </button>
        </div>
        <div className="px-6 py-5">{children}</div>
        {footer && <div className="flex justify-end gap-2 border-t border-slate-200 px-6 py-4">{footer}</div>}
      </div>
    </div>
  );
}

export function ConfirmDialog({ open, title, message, confirmLabel = "Confirmer", danger = true, loading, onConfirm, onCancel }) {
  return (
    <Modal
      open={open}
      title={title}
      onClose={onCancel}
      footer={
        <>
          <Button variant="secondary" onClick={onCancel}>
            Annuler
          </Button>
          <Button variant={danger ? "danger" : "primary"} loading={loading} onClick={onConfirm}>
            {confirmLabel}
          </Button>
        </>
      }
    >
      <p className="text-sm text-slate-600">{message}</p>
    </Modal>
  );
}

export function Pagination({ page, totalPages, totalElements, onChange }) {
  if (totalElements === 0) return null;
  return (
    <div className="flex items-center justify-between border-t border-slate-200 px-4 py-3 text-sm text-slate-600">
      <span>
        {totalElements} résultat{totalElements > 1 ? "s" : ""}
      </span>
      <div className="flex items-center gap-2">
        <Button variant="secondary" size="sm" disabled={page === 0} onClick={() => onChange(page - 1)}>
          Précédent
        </Button>
        <span>
          Page {page + 1} / {Math.max(totalPages, 1)}
        </span>
        <Button variant="secondary" size="sm" disabled={page + 1 >= totalPages} onClick={() => onChange(page + 1)}>
          Suivant
        </Button>
      </div>
    </div>
  );
}

export function EmptyState({ children = "Aucun résultat" }) {
  return <p className="px-4 py-10 text-center text-sm text-slate-500">{children}</p>;
}

export function Stars({ value }) {
  const n = Math.round(Number(value) || 0);
  return (
    <span className="whitespace-nowrap text-amber-500" aria-label={`${value} sur 5`}>
      {"★".repeat(n)}
      <span className="text-slate-300">{"★".repeat(5 - n)}</span>
    </span>
  );
}

export function PageHeader({ title, subtitle, action }) {
  return (
    <div className="mb-6 flex flex-wrap items-end justify-between gap-4">
      <div>
        <h1 className="text-2xl font-bold text-slate-900">{title}</h1>
        {subtitle && <p className="mt-1 text-sm text-slate-500">{subtitle}</p>}
      </div>
      {action}
    </div>
  );
}

/** Tableau avec états de chargement / vide intégrés. */
export function Table({ columns, loading, empty, children }) {
  return (
    <div className="overflow-x-auto">
      <table className="min-w-full divide-y divide-slate-200">
        <thead className="bg-slate-50">
          <tr>
            {columns.map((c) => (
              <th key={c} className="th">
                {c}
              </th>
            ))}
          </tr>
        </thead>
        <tbody className="divide-y divide-slate-100">{!loading && children}</tbody>
      </table>
      {loading && (
        <div className="flex justify-center py-10">
          <Spinner />
        </div>
      )}
      {!loading && empty && <EmptyState />}
    </div>
  );
}
