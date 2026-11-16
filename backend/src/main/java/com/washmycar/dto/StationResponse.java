package com.washmycar.dto;

import com.washmycar.entity.StatutInscription;

import java.math.BigDecimal;

/** Vue complète d'une station (administration et profil station). */
public record StationResponse(
        Long id,
        String nom,
        String adresse,
        String ville,
        Double latitude,
        Double longitude,
        String telephone,
        String description,
        String photoUrl,
        StatutInscription statutInscription,
        BigDecimal noteMoyenne,
        ProprietaireResponse proprietaire
) {
}
