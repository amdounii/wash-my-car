package com.washmycar.dto;

import jakarta.validation.constraints.*;

/**
 * Création / modification d'un client par l'administrateur.
 * En modification, un mot de passe vide conserve l'ancien.
 */
public record ClientRequest(
        @NotBlank(message = "Le nom est obligatoire") @Size(max = 100) String nom,
        @NotBlank(message = "Le prénom est obligatoire") @Size(max = 100) String prenom,
        @NotBlank(message = "L'email est obligatoire") @Email(message = "Email invalide") @Size(max = 150) String email,
        @Size(min = 8, max = 72, message = "Le mot de passe doit contenir entre 8 et 72 caractères") String motDePasse,
        @Pattern(regexp = "^$|^(\\+216)?[2-9]\\d{7}$", message = "Numéro tunisien invalide (8 chiffres)") String telephone,
        Boolean emailVerifie
) {
}
