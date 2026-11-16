package com.washmycar.controller;

import com.washmycar.dto.*;
import com.washmycar.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.HtmlUtils;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentification", description = "Inscription, validation d'email et connexion")
@SecurityRequirements // routes publiques : pas de jeton requis
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register/client")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Inscription d'un client (email de validation envoyé)")
    public MessageResponse registerClient(@Valid @RequestBody RegisterClientRequest req) {
        return authService.registerClient(req);
    }

    @PostMapping("/register/station")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Inscription d'une station (en attente de validation par l'administrateur)")
    public MessageResponse registerStation(@Valid @RequestBody RegisterStationRequest req) {
        return authService.registerStation(req);
    }

    @PostMapping("/login")
    @Operation(summary = "Connexion : renvoie un jeton JWT")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        return authService.login(req);
    }

    @PostMapping("/resend-verification")
    @Operation(summary = "Renvoyer l'email de validation d'un compte client")
    public MessageResponse resend(@Valid @RequestBody ResendVerificationRequest req) {
        return authService.resendVerification(req.email());
    }

    /** Lien cliqué depuis l'email : renvoie une petite page HTML lisible dans un navigateur. */
    @GetMapping(value = "/verify", produces = MediaType.TEXT_HTML_VALUE)
    @Operation(summary = "Validation du compte via le lien reçu par email")
    public String verify(@RequestParam(required = false) String token) {
        AuthService.VerificationResult result = authService.verifyEmail(token);
        String message = switch (result) {
            case VALIDE -> "Votre compte est validé ! Vous pouvez maintenant vous connecter dans l'application.";
            case DEJA_VALIDE -> "Votre compte est déjà validé. Vous pouvez vous connecter.";
            case EXPIRE -> "Ce lien a expiré. Demandez un nouveau lien depuis l'application.";
            case INVALIDE -> "Lien de validation invalide.";
        };
        boolean ok = result == AuthService.VerificationResult.VALIDE
                || result == AuthService.VerificationResult.DEJA_VALIDE;
        return """
                <!doctype html><html lang="fr"><head><meta charset="utf-8">
                <meta name="viewport" content="width=device-width,initial-scale=1">
                <title>Wash My Car</title></head>
                <body style="font-family:sans-serif;display:flex;justify-content:center;padding:48px 16px;background:#f1f5f9">
                <div style="max-width:420px;background:#fff;border-radius:12px;padding:32px;text-align:center;box-shadow:0 2px 8px #0001">
                <h1 style="color:#0369a1;margin-top:0">Wash My Car</h1>
                <p style="font-size:18px;color:%s">%s</p></div></body></html>
                """.formatted(ok ? "#15803d" : "#b91c1c", HtmlUtils.htmlEscape(message, "UTF-8"));
    }
}
