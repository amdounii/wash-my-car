package com.washmycar.controller;

import com.washmycar.dto.EvaluationRequest;
import com.washmycar.dto.EvaluationResponse;
import com.washmycar.dto.ReservationCreateRequest;
import com.washmycar.dto.ReservationResponse;
import com.washmycar.security.AuthUser;
import com.washmycar.service.EvaluationService;
import com.washmycar.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Espace client : réservations et évaluations. */
@RestController
@RequestMapping("/api/client")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CLIENT')")
@Tag(name = "Client", description = "Réservations et évaluations du client connecté")
public class ClientController {

    private final ReservationService reservationService;
    private final EvaluationService evaluationService;

    @PostMapping("/reservations")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Réserver un créneau (statut initial : EN_ATTENTE)")
    public ReservationResponse reserver(@AuthenticationPrincipal AuthUser me,
                                        @Valid @RequestBody ReservationCreateRequest req) {
        return reservationService.create(me.id(), req);
    }

    @GetMapping("/reservations")
    @Operation(summary = "Mes réservations (plus récentes d'abord)")
    public List<ReservationResponse> mesReservations(@AuthenticationPrincipal AuthUser me) {
        return reservationService.mesReservations(me.id());
    }

    @PatchMapping("/reservations/{id}/annuler")
    @Operation(summary = "Annuler une de mes réservations à venir")
    public ReservationResponse annuler(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        return reservationService.annulerParClient(me.id(), id);
    }

    @PostMapping("/evaluations")
    @Operation(summary = "Évaluer une station (crée ou remplace mon évaluation)")
    public EvaluationResponse evaluer(@AuthenticationPrincipal AuthUser me,
                                      @Valid @RequestBody EvaluationRequest req) {
        return evaluationService.evaluer(me.id(), req);
    }

    @GetMapping("/evaluations")
    @Operation(summary = "Mes évaluations")
    public List<EvaluationResponse> mesEvaluations(@AuthenticationPrincipal AuthUser me) {
        return evaluationService.parClient(me.id());
    }
}
