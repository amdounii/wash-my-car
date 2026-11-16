package com.washmycar.dto;

import com.washmycar.entity.StatutInscription;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

/**
 * Création d'une station par l'administrateur (compte gérant + station).
 * Par défaut la station est directement validée.
 */
public record AdminStationRequest(
        @NotBlank(message = "Le nom du gérant est obligatoire") @Size(max = 100) String nom,
        @NotBlank(message = "Le prénom du gérant est obligatoire") @Size(max = 100) String prenom,
        @NotBlank(message = "L'email est obligatoire") @Email(message = "Email invalide") @Size(max = 150) String email,
        @NotBlank(message = "Le mot de passe est obligatoire")
        @Size(min = 8, max = 72, message = "Le mot de passe doit contenir entre 8 et 72 caractères") String motDePasse,
        @Pattern(regexp = "^$|^(\\+216)?[2-9]\\d{7}$", message = "Numéro tunisien invalide (8 chiffres)") String telephone,
        @NotNull(message = "Les informations de la station sont obligatoires") @Valid StationInfoRequest station,
        StatutInscription statutInscription
) {
}
