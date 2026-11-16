package com.washmycar.controller;

import com.washmycar.dto.ProfileUpdateRequest;
import com.washmycar.dto.UserResponse;
import com.washmycar.security.AuthUser;
import com.washmycar.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** Profil de l'utilisateur connecté (tous rôles). */
@RestController
@RequestMapping("/api/profil")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Tag(name = "Profil", description = "Consultation et mise à jour du profil connecté")
public class ProfileController {

    private final UserService userService;

    @GetMapping
    @Operation(summary = "Mon profil")
    public UserResponse get(@AuthenticationPrincipal AuthUser me) {
        return userService.getProfil(me.id());
    }

    @PutMapping
    @Operation(summary = "Mettre à jour mon profil (et éventuellement mon mot de passe)")
    public UserResponse update(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody ProfileUpdateRequest req) {
        return userService.updateProfil(me.id(), req);
    }
}
