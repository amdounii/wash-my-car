/** Formatage et libellés français partagés par toutes les pages. */

const tnd = new Intl.NumberFormat("fr-TN", { minimumFractionDigits: 3, maximumFractionDigits: 3 });

/** 12.5 -> « 12,500 TND » (le dinar se divise en 1000 millimes). */
export const formatTND = (value) => (value == null ? "—" : `${tnd.format(value)} TND`);

export const formatDateTime = (iso) =>
  iso
    ? new Date(iso).toLocaleString("fr-FR", {
        day: "2-digit",
        month: "2-digit",
        year: "numeric",
        hour: "2-digit",
        minute: "2-digit",
      })
    : "—";

export const formatDate = (iso) => (iso ? new Date(iso).toLocaleDateString("fr-FR") : "—");

export const formatTime = (iso) =>
  iso ? new Date(iso).toLocaleTimeString("fr-FR", { hour: "2-digit", minute: "2-digit" }) : "—";

export const STATUT_INSCRIPTION = {
  EN_ATTENTE: { label: "En attente", color: "amber" },
  VALIDEE: { label: "Validée", color: "green" },
  REFUSEE: { label: "Refusée", color: "red" },
};

export const STATUT_RESERVATION = {
  EN_ATTENTE: { label: "En attente", color: "amber" },
  ACCEPTEE: { label: "Acceptée", color: "green" },
  REFUSEE: { label: "Refusée", color: "red" },
  ANNULEE: { label: "Annulée", color: "slate" },
};

export const TYPE_VEHICULE = {
  CITADINE: "Citadine",
  BERLINE: "Berline",
  SUV: "SUV",
  UTILITAIRE: "Utilitaire",
  MOTO: "Moto",
};

export const JOURS = {
  LUNDI: "Lundi",
  MARDI: "Mardi",
  MERCREDI: "Mercredi",
  JEUDI: "Jeudi",
  VENDREDI: "Vendredi",
  SAMEDI: "Samedi",
  DIMANCHE: "Dimanche",
};
