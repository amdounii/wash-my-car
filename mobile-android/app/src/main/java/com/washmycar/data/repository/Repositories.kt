package com.washmycar.data.repository

import com.washmycar.data.local.SessionStore
import com.washmycar.data.model.*
import com.washmycar.data.remote.ApiCaller
import com.washmycar.data.remote.ApiException
import com.washmycar.data.remote.ApiService
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/** Inscription, connexion et déconnexion. */
@Singleton
class AuthRepository @Inject constructor(
    private val api: ApiService,
    private val caller: ApiCaller,
    private val sessionStore: SessionStore,
) {
    val session = sessionStore.session

    /** Seuls les clients et les stations utilisent l'application mobile. */
    suspend fun login(email: String, motDePasse: String): Result<AuthResponse> =
        caller.call { api.login(LoginRequest(email.trim(), motDePasse)) }.mapCatching { auth ->
            if (auth.role == "ADMIN") {
                throw ApiException("Les administrateurs utilisent l'interface web.")
            }
            sessionStore.save(auth)
            auth
        }

    suspend fun registerClient(req: RegisterClientRequest) = caller.call { api.registerClient(req) }

    suspend fun registerStation(req: RegisterStationRequest) = caller.call { api.registerStation(req) }

    suspend fun resendVerification(email: String) =
        caller.call { api.resendVerification(ResendVerificationRequest(email.trim())) }

    suspend fun logout() = sessionStore.clear()
}

/** Profil de l'utilisateur connecté (client ou gérant de station). */
@Singleton
class ProfileRepository @Inject constructor(
    private val api: ApiService,
    private val caller: ApiCaller,
    private val sessionStore: SessionStore,
) {
    suspend fun get() = caller.call { api.getProfile() }

    suspend fun update(req: ProfileUpdateRequest) = caller.call { api.updateProfile(req) }
        .onSuccess { sessionStore.updateNames(it.nom, it.prenom) }
}

/** Consultation publique des stations validées. */
@Singleton
class StationRepository @Inject constructor(
    private val api: ApiService,
    private val caller: ApiCaller,
) {
    suspend fun nearby(lat: Double, lng: Double, rayonKm: Double) =
        caller.call { api.nearbyStations(lat, lng, rayonKm) }

    suspend fun detail(id: Long) = caller.call { api.stationDetail(id) }

    suspend fun evaluations(id: Long) = caller.call { api.stationEvaluations(id) }

    suspend fun occupied(id: Long, date: LocalDate) = caller.call { api.occupiedSlots(id, date.toString()) }
}

/** Réservations et évaluations du client connecté. */
@Singleton
class ClientRepository @Inject constructor(
    private val api: ApiService,
    private val caller: ApiCaller,
) {
    suspend fun reserve(stationId: Long, tarifId: Long, debut: LocalDateTime) =
        caller.call { api.createReservation(ReservationRequest(stationId, tarifId, debut.toString())) }

    suspend fun myReservations() = caller.call { api.myReservations() }

    suspend fun cancel(id: Long) = caller.call { api.cancelReservation(id) }

    suspend fun evaluate(stationId: Long, note: Int, commentaire: String?) =
        caller.call { api.evaluate(EvaluationRequest(stationId, note, commentaire?.takeIf { it.isNotBlank() })) }
}

/** Gestion de la station connectée : profil, horaires, tarifs, réservations reçues, avis. */
@Singleton
class StationSpaceRepository @Inject constructor(
    private val api: ApiService,
    private val caller: ApiCaller,
) {
    suspend fun profile() = caller.call { api.myStation() }

    suspend fun updateProfile(info: StationInfo) = caller.call { api.updateMyStation(info) }

    suspend fun horaires() = caller.call { api.myHoraires() }

    suspend fun updateHoraires(horaires: List<Horaire>) = caller.call { api.updateHoraires(HorairesRequest(horaires)) }

    suspend fun tarifs() = caller.call { api.myTarifs() }

    suspend fun addTarif(req: TarifRequest) = caller.call { api.addTarif(req) }

    suspend fun updateTarif(id: Long, req: TarifRequest) = caller.call { api.updateTarif(id, req) }

    suspend fun deleteTarif(id: Long) = caller.call { with(caller) { api.deleteTarif(id).orThrow() } }

    suspend fun reservations(statut: String?) = caller.call { api.stationReservations(statut) }

    suspend fun changeStatut(id: Long, statut: String) =
        caller.call { api.changeReservationStatut(id, StatutRequest(statut)) }

    suspend fun evaluations() = caller.call { api.myEvaluations() }
}
