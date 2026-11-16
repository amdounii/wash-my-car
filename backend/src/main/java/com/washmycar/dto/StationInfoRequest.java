package com.washmycar.dto;

import jakarta.validation.constraints.*;

/** Informations descriptives d'une station (inscription, profil, administration). */
public record StationInfoRequest(
        @NotBlank(message = "Le nom de la station est obligatoire") @Size(max = 150) String nom,
        @NotBlank(message = "L'adresse est obligatoire") @Size(max = 255) String adresse,
        @NotBlank(message = "La ville est obligatoire") @Size(max = 100) String ville,
        @NotNull(message = "La latitude est obligatoire")
        @DecimalMin(value = "-90.0") @DecimalMax(value = "90.0") Double latitude,
        @NotNull(message = "La longitude est obligatoire")
        @DecimalMin(value = "-180.0") @DecimalMax(value = "180.0") Double longitude,
        @Pattern(regexp = "^$|^(\\+216)?[2-9]\\d{7}$", message = "Numéro tunisien invalide (8 chiffres)") String telephone,
        @Size(max = 1000) String description,
        @Size(max = 500) @Pattern(regexp = "^$|^https?://.+", message = "L'URL de la photo doit commencer par http(s)://") String photoUrl
) {
}
