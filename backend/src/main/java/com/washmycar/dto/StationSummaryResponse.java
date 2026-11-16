package com.washmycar.dto;

import java.math.BigDecimal;

/** Élément de la liste des stations proches (application client). */
public record StationSummaryResponse(
        Long id,
        String nom,
        String adresse,
        String ville,
        Double latitude,
        Double longitude,
        String photoUrl,
        BigDecimal noteMoyenne,
        Double distanceKm,
        boolean ouverteMaintenant,
        BigDecimal prixMin
) {
}
