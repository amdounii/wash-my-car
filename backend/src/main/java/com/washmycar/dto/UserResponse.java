package com.washmycar.dto;

import com.washmycar.entity.Role;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String nom,
        String prenom,
        String email,
        String telephone,
        Role role,
        boolean emailVerifie,
        LocalDateTime dateCreation
) {
}
