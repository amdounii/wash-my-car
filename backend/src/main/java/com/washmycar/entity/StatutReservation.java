package com.washmycar.entity;

/** Cycle de vie d'une réservation. */
public enum StatutReservation {
    EN_ATTENTE, ACCEPTEE, REFUSEE, ANNULEE;

    /** Un créneau est occupé tant que la réservation est en attente ou acceptée. */
    public boolean bloqueCreneau() {
        return this == EN_ATTENTE || this == ACCEPTEE;
    }
}
