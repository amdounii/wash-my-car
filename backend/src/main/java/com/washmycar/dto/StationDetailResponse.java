package com.washmycar.dto;

import java.math.BigDecimal;
import java.util.List;

/** Fiche détaillée d'une station (application client). */
public record StationDetailResponse(
        Long id,
        String nom,
        String adresse,
        String ville,
        Double latitude,
        Double longitude,
        String telephone,
        String description,
        String photoUrl,
        BigDecimal noteMoyenne,
        long nbEvaluations,
        boolean ouverteMaintenant,
        List<HoraireDto> horaires,
        List<TarifResponse> tarifs
) {
}
