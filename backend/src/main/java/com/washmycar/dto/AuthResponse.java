package com.washmycar.dto;

import com.washmycar.entity.Role;

/** Jeton JWT et informations de base sur l'utilisateur connecté. */
public record AuthResponse(
        String token,
        String type,
        Long userId,
        String nom,
        String prenom,
        String email,
        Role role,
        Long stationId
) {
}
