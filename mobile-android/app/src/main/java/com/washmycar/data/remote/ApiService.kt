package com.washmycar.data.remote

import com.washmycar.data.model.*
import retrofit2.Response
import retrofit2.http.*

/** Routes de l'API Wash My Car utilisées par l'application mobile. */
interface ApiService {

    // ------------------------------------------------------------------ authentification
    @POST("api/auth/login")
    suspend fun login(@Body body: LoginRequest): AuthResponse

    @POST("api/auth/register/client")
    suspend fun registerClient(@Body body: RegisterClientRequest): MessageResponse

    @POST("api/auth/register/station")
    suspend fun registerStation(@Body body: RegisterStationRequest): MessageResponse

    @POST("api/auth/resend-verification")
    suspend fun resendVerification(@Body body: ResendVerificationRequest): MessageResponse

    // ------------------------------------------------------------------ profil
    @GET("api/profil")
    suspend fun getProfile(): UserProfile

    @PUT("api/profil")
    suspend fun updateProfile(@Body body: ProfileUpdateRequest): UserProfile

    // ------------------------------------------------------------------ stations (public)
    @GET("api/stations/proches")
    suspend fun nearbyStations(
        @Query("lat") lat: Double,
        @Query("lng") lng: Double,
        @Query("rayonKm") rayonKm: Double,
        @Query("limite") limite: Int = 50,
    ): List<StationSummary>

    @GET("api/stations/{id}")
    suspend fun stationDetail(@Path("id") id: Long): StationDetail

    @GET("api/stations/{id}/evaluations")
    suspend fun stationEvaluations(@Path("id") id: Long): List<Evaluation>

    @GET("api/stations/{id}/creneaux-occupes")
    suspend fun occupiedSlots(@Path("id") id: Long, @Query("date") date: String): List<CreneauOccupe>

    // ------------------------------------------------------------------ client
    @POST("api/client/reservations")
    suspend fun createReservation(@Body body: ReservationRequest): Reservation

    @GET("api/client/reservations")
    suspend fun myReservations(): List<Reservation>

    @PATCH("api/client/reservations/{id}/annuler")
    suspend fun cancelReservation(@Path("id") id: Long): Reservation

    @POST("api/client/evaluations")
    suspend fun evaluate(@Body body: EvaluationRequest): Evaluation

    // ------------------------------------------------------------------ station connectée
    @GET("api/station/profil")
    suspend fun myStation(): StationProfile

    @PUT("api/station/profil")
    suspend fun updateMyStation(@Body body: StationInfo): StationProfile

    @GET("api/station/horaires")
    suspend fun myHoraires(): List<Horaire>

    @PUT("api/station/horaires")
    suspend fun updateHoraires(@Body body: HorairesRequest): List<Horaire>

    @GET("api/station/tarifs")
    suspend fun myTarifs(): List<Tarif>

    @POST("api/station/tarifs")
    suspend fun addTarif(@Body body: TarifRequest): Tarif

    @PUT("api/station/tarifs/{id}")
    suspend fun updateTarif(@Path("id") id: Long, @Body body: TarifRequest): Tarif

    @DELETE("api/station/tarifs/{id}")
    suspend fun deleteTarif(@Path("id") id: Long): Response<Unit>

    @GET("api/station/reservations")
    suspend fun stationReservations(@Query("statut") statut: String?): List<Reservation>

    @PATCH("api/station/reservations/{id}/statut")
    suspend fun changeReservationStatut(@Path("id") id: Long, @Body body: StatutRequest): Reservation

    @GET("api/station/evaluations")
    suspend fun myEvaluations(): List<Evaluation>
}
