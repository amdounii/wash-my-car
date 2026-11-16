package com.washmycar.data

import android.os.Build
import com.google.gson.Gson
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

data class ClientRegister(
    val nom: String, val prenom: String, val email: String,
    val telephone: String, val motDePasse: String
)

data class StationRegister(
    val nomStation: String, val adresse: String, val description: String,
    val nom: String, val prenom: String, val email: String,
    val telephone: String, val motDePasse: String
)

data class LoginRequest(val email: String, val motDePasse: String)
data class LoginResponse(val jeton: String, val role: String)
data class MessageResponse(val message: String)

interface ApiService {
    @POST("api/auth/client/register")
    suspend fun registerClient(@Body body: ClientRegister): Response<MessageResponse>

    @POST("api/auth/station/register")
    suspend fun registerStation(@Body body: StationRegister): Response<MessageResponse>

    @POST("api/auth/login")
    suspend fun login(@Body body: LoginRequest): Response<LoginResponse>
}

object Api {
    private val isEmulator: Boolean = (
        Build.FINGERPRINT.startsWith("generic")
            || Build.FINGERPRINT.startsWith("unknown")
            || Build.MODEL.contains("google_sdk")
            || Build.MODEL.contains("Emulator")
            || Build.MODEL.contains("Android SDK built for x86")
            || Build.MANUFACTURER.contains("Genymotion")
            || Build.HARDWARE.contains("goldfish")
            || Build.HARDWARE.contains("ranchu")
            || Build.PRODUCT.contains("sdk")
            || Build.PRODUCT.contains("google_sdk")
    )

    // Sur émulateur : 10.0.2.2:8081 / Sur téléphone physique : IP Wi-Fi de la machine
    val BASE_URL: String = if (isEmulator) "http://10.0.2.2:8081/" else "http://192.168.81.177:8081/"

    val service: ApiService by lazy {
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}

/** Extrait le message d'erreur renvoyé par l'API (champ "message"). */
fun <T> Response<T>.errorMessage(): String = try {
    val errorStr = errorBody()?.string()
    val parsed = Gson().fromJson(errorStr, MessageResponse::class.java)
    parsed?.message ?: "Erreur ${code()}"
} catch (e: Exception) {
    "Erreur ${code()}"
}
