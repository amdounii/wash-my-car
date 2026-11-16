package com.washmycar.dto;

import jakarta.validation.constraints.*;

public record LoginRequest(
        @NotBlank(message = "L'email est obligatoire") @Email(message = "Email invalide") String email,
        @NotBlank(message = "Le mot de passe est obligatoire") String motDePasse
) {
}
