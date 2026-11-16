package com.washmycar.dto;

/** Gérant d'une station (vue administrateur). */
public record ProprietaireResponse(Long id, String nom, String prenom, String email, String telephone) {
}
