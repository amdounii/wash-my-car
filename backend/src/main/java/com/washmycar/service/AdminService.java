package com.washmycar.service;

import com.washmycar.dto.Dtos.StationResponse;
import com.washmycar.exception.ApiException;
import com.washmycar.model.Station;
import com.washmycar.model.StatutValidation;
import com.washmycar.model.Utilisateur;
import com.washmycar.repository.StationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AdminService {

    private final StationRepository stations;

    public AdminService(StationRepository stations) {
        this.stations = stations;
    }

    @Transactional(readOnly = true)
    public List<StationResponse> pending() {
        return stations.findByStatutValidation(StatutValidation.EN_ATTENTE).stream().map(this::toDto).toList();
    }

    @Transactional
    public StationResponse approve(Long id) {
        Station s = find(id);
        s.setStatutValidation(StatutValidation.VALIDEE);
        s.setMotifRefus(null);
        s.setDateDecision(LocalDateTime.now());
        return toDto(s);
    }

    @Transactional
    public StationResponse reject(Long id, String motif) {
        Station s = find(id);
        s.setStatutValidation(StatutValidation.REFUSEE);
        s.setMotifRefus(motif.trim());                            // RG06
        s.setDateDecision(LocalDateTime.now());
        return toDto(s);
    }

    private Station find(Long id) {
        return stations.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Station introuvable"));
    }

    private StationResponse toDto(Station s) {
        Utilisateur u = s.getUtilisateur();
        return new StationResponse(s.getId(), s.getNomStation(), s.getAdresse(), s.getDescription(),
                s.getStatutValidation().name(), s.getMotifRefus(), s.getDateDecision(),
                u.getNom(), u.getPrenom(), u.getEmail(), u.getTelephone());
    }
}
