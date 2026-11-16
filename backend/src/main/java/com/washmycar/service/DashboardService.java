package com.washmycar.service;

import com.washmycar.dto.DashboardResponse;
import com.washmycar.entity.Role;
import com.washmycar.entity.StatutInscription;
import com.washmycar.entity.StatutReservation;
import com.washmycar.repository.EvaluationRepository;
import com.washmycar.repository.ReservationRepository;
import com.washmycar.repository.StationRepository;
import com.washmycar.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Indicateurs agrégés pour le tableau de bord administrateur. */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final UserRepository userRepository;
    private final StationRepository stationRepository;
    private final ReservationRepository reservationRepository;
    private final EvaluationRepository evaluationRepository;

    @Transactional(readOnly = true)
    public DashboardResponse get() {
        return new DashboardResponse(
                userRepository.countByRole(Role.CLIENT),
                stationRepository.count(),
                stationRepository.countByStatutInscription(StatutInscription.EN_ATTENTE),
                stationRepository.countByStatutInscription(StatutInscription.VALIDEE),
                stationRepository.countByStatutInscription(StatutInscription.REFUSEE),
                reservationRepository.countByStatut(StatutReservation.EN_ATTENTE),
                reservationRepository.countByStatut(StatutReservation.ACCEPTEE),
                reservationRepository.countByStatut(StatutReservation.REFUSEE),
                reservationRepository.countByStatut(StatutReservation.ANNULEE),
                evaluationRepository.count());
    }
}
