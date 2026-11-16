package com.washmycar.security;

import com.washmycar.entity.Role;

/**
 * Utilisateur authentifié placé dans le SecurityContext par le filtre JWT.
 * Récupérable dans les contrôleurs avec {@code @AuthenticationPrincipal AuthUser user}.
 */
public record AuthUser(Long id, String email, Role role) {
}
