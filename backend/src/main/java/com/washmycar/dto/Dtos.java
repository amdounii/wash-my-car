package com.washmycar.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public final class Dtos {
    private Dtos() {}

    private static final String MDP_MSG = "Le mot de passe doit contenir au moins 8 caractères";

    public record ClientRegisterRequest(
            @NotBlank(message = "Le nom est obligatoire") String nom,
            @NotBlank(message = "Le prénom est obligatoire") String prenom,
            @NotBlank(message = "L'e-mail est obligatoire") @Email(message = "E-mail invalide") String email,
            @NotBlank(message = "Le téléphone est obligatoire") String telephone,
            @NotBlank(message = MDP_MSG) @Size(min = 8, message = MDP_MSG) String motDePasse) {}

    public record StationRegisterRequest(
            @NotBlank(message = "Le nom de la station est obligatoire") String nomStation,
            @NotBlank(message = "L'adresse est obligatoire") String adresse,
            String description,
            @NotBlank(message = "Le nom du responsable est obligatoire") String nom,
            @NotBlank(message = "Le prénom du responsable est obligatoire") String prenom,
            @NotBlank(message = "L'e-mail est obligatoire") @Email(message = "E-mail invalide") String email,
            @NotBlank(message = "Le téléphone est obligatoire") String telephone,
            @NotBlank(message = MDP_MSG) @Size(min = 8, message = MDP_MSG) String motDePasse) {}

    public record LoginRequest(
            @NotBlank(message = "L'e-mail est obligatoire") String email,
            @NotBlank(message = "Le mot de passe est obligatoire") String motDePasse) {}

    public record LoginResponse(String jeton, String role) {}

    public record RejectRequest(@NotBlank(message = "Le motif de refus est obligatoire") String motif) {}

    public record MessageResponse(String message) {}

    // RG11 : jamais de mot de passe ni de hash dans les réponses
    public record StationResponse(
            Long id, String nomStation, String adresse, String description,
            String statutValidation, String motifRefus, LocalDateTime dateDecision,
            String responsableNom, String responsablePrenom, String email, String telephone) {}
}
