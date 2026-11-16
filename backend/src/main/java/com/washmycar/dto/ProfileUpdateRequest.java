package com.washmycar.dto;

import jakarta.validation.constraints.*;

/**
 * Mise à jour du profil de l'utilisateur connecté.
 * Pour changer de mot de passe, fournir l'ancien et le nouveau.
 */
public record ProfileUpdateRequest(
        @NotBlank(message = "Le nom est obligatoire") @Size(max = 100) String nom,
        @NotBlank(message = "Le prénom est obligatoire") @Size(max = 100) String prenom,
        @Pattern(regexp = "^$|^(\\+216)?[2-9]\\d{7}$", message = "Numéro tunisien invalide (8 chiffres)") String telephone,
        String ancienMotDePasse,
        @Size(min = 8, max = 72, message = "Le mot de passe doit contenir entre 8 et 72 caractères") String nouveauMotDePasse
) {
}
