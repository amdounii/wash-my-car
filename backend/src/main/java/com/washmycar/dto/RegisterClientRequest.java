package com.washmycar.dto;

import jakarta.validation.constraints.*;

/** Inscription d'un client depuis l'application mobile. */
public record RegisterClientRequest(
        @NotBlank(message = "Le nom est obligatoire") @Size(max = 100) String nom,
        @NotBlank(message = "Le prénom est obligatoire") @Size(max = 100) String prenom,
        @NotBlank(message = "L'email est obligatoire") @Email(message = "Email invalide") @Size(max = 150) String email,
        @NotBlank(message = "Le mot de passe est obligatoire")
        @Size(min = 8, max = 72, message = "Le mot de passe doit contenir entre 8 et 72 caractères") String motDePasse,
        @Pattern(regexp = "^$|^(\\+216)?[2-9]\\d{7}$", message = "Numéro tunisien invalide (8 chiffres)") String telephone
) {
}
