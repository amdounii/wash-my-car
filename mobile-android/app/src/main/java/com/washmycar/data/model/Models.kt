package com.washmycar.data.model

/*
 * Modèles échangés avec l'API (mêmes noms de champs que les DTO du backend).
 * Les dates sont reçues au format ISO « 2026-11-23T09:00:00 » et les heures « 08:00 ».
 * Les énumérations (statuts, types de véhicule, jours) sont transmises sous forme de chaînes.
 */

// ------------------------------------------------------------------ authentification

data class LoginRequest(val email: String, val motDePasse: String)

data class AuthResponse(
    val token: String,
    val userId: Long,
    val nom: String,
    val prenom: String,
    val email: String,
    val role: String,
    val stationId: Long?,
)

data class MessageResponse(val message: String)

data class RegisterClientRequest(
    val nom: String,
    val prenom: String,
    val email: String,
    val motDePasse: String,
    val telephone: String?,
)

data class RegisterStationRequest(
    val nom: String,
    val prenom: String,
    val email: String,
    val motDePasse: String,
    val telephone: String?,
    val station: StationInfo,
)

data class ResendVerificationRequest(val email: String)

// ------------------------------------------------------------------ profil

data class UserProfile(
    val id: Long,
    val nom: String,
    val prenom: String,
    val email: String,
    val telephone: String?,
    val role: String,
    val emailVerifie: Boolean,
)

data class ProfileUpdateRequest(
    val nom: String,
    val prenom: String,
    val telephone: String?,
    val ancienMotDePasse: String?,
    val nouveauMotDePasse: String?,
)

// ------------------------------------------------------------------ stations

/** Informations modifiables d'une station (inscription et profil station). */
data class StationInfo(
    val nom: String,
    val adresse: String,
    val ville: String,
    val latitude: Double,
    val longitude: Double,
    val telephone: String?,
    val description: String?,
    val photoUrl: String?,
)

data class StationSummary(
    val id: Long,
    val nom: String,
    val adresse: String,
    val ville: String,
    val latitude: Double,
    val longitude: Double,
    val photoUrl: String?,
    val noteMoyenne: Double,
    val distanceKm: Double,
    val ouverteMaintenant: Boolean,
    val prixMin: Double?,
)

data class Horaire(
    val jourSemaine: String,
    val heureOuverture: String?,
    val heureFermeture: String?,
    val ferme: Boolean,
)

data class Tarif(
    val id: Long,
    val typeVehicule: String,
    val service: String,
    val prix: Double,
    val dureeMinutes: Int,
)

data class StationDetail(
    val id: Long,
    val nom: String,
    val adresse: String,
    val ville: String,
    val latitude: Double,
    val longitude: Double,
    val telephone: String?,
    val description: String?,
    val photoUrl: String?,
    val noteMoyenne: Double,
    val nbEvaluations: Long,
    val ouverteMaintenant: Boolean,
    val horaires: List<Horaire>,
    val tarifs: List<Tarif>,
)

data class Proprietaire(val id: Long, val nom: String, val prenom: String, val email: String, val telephone: String?)

/** Fiche complète de la station connectée. */
data class StationProfile(
    val id: Long,
    val nom: String,
    val adresse: String,
    val ville: String,
    val latitude: Double,
    val longitude: Double,
    val telephone: String?,
    val description: String?,
    val photoUrl: String?,
    val statutInscription: String,
    val noteMoyenne: Double,
    val proprietaire: Proprietaire?,
)

data class HorairesRequest(val horaires: List<Horaire>)

data class TarifRequest(
    val typeVehicule: String,
    val service: String,
    val prix: Double,
    val dureeMinutes: Int,
)

// ------------------------------------------------------------------ réservations

data class CreneauOccupe(val debut: String, val fin: String)

data class ReservationRequest(val stationId: Long, val tarifId: Long, val dateDebut: String)

data class Reservation(
    val id: Long,
    val clientId: Long,
    val clientNom: String,
    val clientEmail: String,
    val clientTelephone: String?,
    val stationId: Long,
    val stationNom: String,
    val stationAdresse: String,
    val typeVehicule: String,
    val service: String,
    val dateDebut: String,
    val dateFin: String,
    val statut: String,
    val prix: Double,
    val evaluable: Boolean,
)

data class StatutRequest(val statut: String)

// ------------------------------------------------------------------ évaluations

data class EvaluationRequest(val stationId: Long, val note: Int, val commentaire: String?)

data class Evaluation(
    val id: Long,
    val clientId: Long,
    val clientNom: String,
    val stationId: Long,
    val stationNom: String,
    val note: Int,
    val commentaire: String?,
    val dateCreation: String,
)

// ------------------------------------------------------------------ erreurs

/** Corps des réponses d'erreur du backend (ErrorResponse). */
data class ApiErrorBody(val status: Int?, val message: String?, val erreurs: Map<String, String>?)
