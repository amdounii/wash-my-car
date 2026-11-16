package com.washmycar.controller;

import com.washmycar.dto.CreneauOccupeResponse;
import com.washmycar.dto.EvaluationResponse;
import com.washmycar.dto.StationDetailResponse;
import com.washmycar.dto.StationSummaryResponse;
import com.washmycar.service.EvaluationService;
import com.washmycar.service.StationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/** Consultation publique des stations validées. */
@RestController
@RequestMapping("/api/stations")
@RequiredArgsConstructor
@Validated
@Tag(name = "Stations (public)", description = "Recherche des stations proches et fiches publiques")
@SecurityRequirements
public class PublicStationController {

    private final StationService stationService;
    private final EvaluationService evaluationService;

    @GetMapping("/proches")
    @Operation(summary = "Stations validées dans un rayon, triées par distance (Haversine)")
    public List<StationSummaryResponse> proches(
            @Parameter(example = "36.8065") @RequestParam @Min(-90) @Max(90) double lat,
            @Parameter(example = "10.1815") @RequestParam @Min(-180) @Max(180) double lng,
            @RequestParam(defaultValue = "10") @Min(1) @Max(200) double rayonKm,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int limite) {
        return stationService.findProches(lat, lng, rayonKm, limite);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Fiche détaillée d'une station (horaires, tarifs, note)")
    public StationDetailResponse detail(@PathVariable Long id) {
        return stationService.getDetailPublic(id);
    }

    @GetMapping("/{id}/evaluations")
    @Operation(summary = "Évaluations d'une station")
    public List<EvaluationResponse> evaluations(@PathVariable Long id) {
        return evaluationService.parStationPublique(id);
    }

    @GetMapping("/{id}/creneaux-occupes")
    @Operation(summary = "Créneaux déjà réservés un jour donné (disponibilité)")
    public List<CreneauOccupeResponse> creneauxOccupes(
            @PathVariable Long id,
            @Parameter(example = "2026-11-20") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return stationService.getCreneauxOccupes(id, date);
    }
}
