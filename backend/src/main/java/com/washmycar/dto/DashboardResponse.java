package com.washmycar.dto;

/** Indicateurs du tableau de bord administrateur. */
public record DashboardResponse(
        long nbClients,
        long nbStations,
        long stationsEnAttente,
        long stationsValidees,
        long stationsRefusees,
        long reservationsEnAttente,
        long reservationsAcceptees,
        long reservationsRefusees,
        long reservationsAnnulees,
        long nbEvaluations
) {
}
