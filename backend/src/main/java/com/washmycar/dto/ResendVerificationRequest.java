package com.washmycar.dto;

import jakarta.validation.constraints.*;

public record ResendVerificationRequest(
        @NotBlank(message = "L'email est obligatoire") @Email(message = "Email invalide") String email
) {
}
