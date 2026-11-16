package com.washmycar.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "station")
@Getter @Setter @NoArgsConstructor
public class Station {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "utilisateur_id", nullable = false, unique = true)
    private Utilisateur utilisateur;

    @Column(name = "nom_station", nullable = false)
    private String nomStation;

    @Column(nullable = false)
    private String adresse;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut_validation", nullable = false)
    private StatutValidation statutValidation = StatutValidation.EN_ATTENTE;

    @Column(name = "motif_refus", length = 500)
    private String motifRefus;

    @Column(name = "date_decision")
    private LocalDateTime dateDecision;
}
