package com.washmycar.service;

import com.washmycar.dto.EvaluationRequest;
import com.washmycar.dto.EvaluationResponse;
import com.washmycar.dto.PageResponse;
import com.washmycar.entity.*;
import com.washmycar.exception.ApiException;
import com.washmycar.mapper.DtoMapper;
import com.washmycar.repository.EvaluationRepository;
import com.washmycar.repository.ReservationRepository;
import com.washmycar.repository.StationRepository;
import com.washmycar.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

import static com.washmycar.service.AuthService.blankToNull;

/**
 * Évaluations des stations.
 * Un client ne peut évaluer qu'une station où il a une réservation acceptée ;
 * la note moyenne de la station est recalculée après chaque modification.
 */
@Service
@RequiredArgsConstructor
public class EvaluationService {

    private final EvaluationRepository evaluationRepository;
    private final ReservationRepository reservationRepository;
    private final StationRepository stationRepository;
    private final UserRepository userRepository;

    /** Crée l'évaluation du client ou met à jour la précédente (une seule par station). */
    @Transactional
    public EvaluationResponse evaluer(Long clientId, EvaluationRequest req) {
        Station station = stationRepository.findById(req.stationId())
                .filter(s -> s.getStatutInscription() == StatutInscription.VALIDEE)
                .orElseThrow(() -> ApiException.notFound("Station introuvable"));
        if (!reservationRepository.existsByClientIdAndStationIdAndStatut(clientId, station.getId(),
                StatutReservation.ACCEPTEE)) {
            throw ApiException.forbidden("Vous pouvez évaluer uniquement une station où vous avez une réservation acceptée");
        }

        Evaluation e = evaluationRepository.findByClientIdAndStationId(clientId, station.getId())
                .orElseGet(() -> Evaluation.builder()
                        .client(userRepository.getReferenceById(clientId))
                        .station(station)
                        .build());
        e.setNote(req.note());
        e.setCommentaire(blankToNull(req.commentaire()));
        e.setDateCreation(LocalDateTime.now());
        e = evaluationRepository.saveAndFlush(e);

        recalculerNoteMoyenne(station);
        return DtoMapper.toEvaluation(e);
    }

    @Transactional(readOnly = true)
    public List<EvaluationResponse> parStation(Long stationId) {
        return evaluationRepository.findByStationIdOrderByDateCreationDesc(stationId).stream()
                .map(DtoMapper::toEvaluation).toList();
    }

    /** Évaluations publiques : seulement pour une station validée. */
    @Transactional(readOnly = true)
    public List<EvaluationResponse> parStationPublique(Long stationId) {
        stationRepository.findById(stationId)
                .filter(s -> s.getStatutInscription() == StatutInscription.VALIDEE)
                .orElseThrow(() -> ApiException.notFound("Station introuvable"));
        return parStation(stationId);
    }

    @Transactional(readOnly = true)
    public List<EvaluationResponse> parStationDuUser(Long stationUserId) {
        Station s = stationRepository.findByUserId(stationUserId)
                .orElseThrow(() -> ApiException.notFound("Aucune station associée à ce compte"));
        return parStation(s.getId());
    }

    @Transactional(readOnly = true)
    public List<EvaluationResponse> parClient(Long clientId) {
        return evaluationRepository.findByClientIdOrderByDateCreationDesc(clientId).stream()
                .map(DtoMapper::toEvaluation).toList();
    }

    // ================================================================== modération

    @Transactional(readOnly = true)
    public PageResponse<EvaluationResponse> search(Long stationId, Integer note, String q, int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "dateCreation"));
        return PageResponse.of(evaluationRepository.search(stationId, note, blankToNull(q), pageable),
                DtoMapper::toEvaluation);
    }

    @Transactional
    public void deleteByAdmin(Long id) {
        Evaluation e = evaluationRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Évaluation introuvable"));
        Station station = e.getStation();
        evaluationRepository.delete(e);
        evaluationRepository.flush();
        recalculerNoteMoyenne(station);
    }

    /** Moyenne arrondie à une décimale (0.0 lorsqu'il n'y a plus d'évaluation). */
    private void recalculerNoteMoyenne(Station station) {
        Double moyenne = evaluationRepository.averageNote(station.getId());
        station.setNoteMoyenne(moyenne == null
                ? BigDecimal.ZERO.setScale(1)
                : BigDecimal.valueOf(moyenne).setScale(1, RoundingMode.HALF_UP));
    }
}
