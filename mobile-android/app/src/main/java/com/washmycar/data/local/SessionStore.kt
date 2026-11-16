package com.washmycar.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.washmycar.data.model.AuthResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

/** Utilisateur connecté. */
data class Session(
    val token: String,
    val userId: Long,
    val role: String,
    val nom: String,
    val prenom: String,
    val email: String,
    val stationId: Long?,
) {
    val isClient get() = role == "CLIENT"
    val isStation get() = role == "STATION"
}

/**
 * Session persistée avec DataStore. Le jeton est chiffré par [TokenCipher] avant écriture ;
 * les autres informations (nom, rôle…) ne sont pas sensibles.
 */
@Singleton
class SessionStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val cipher: TokenCipher,
) {
    private object Keys {
        val TOKEN = stringPreferencesKey("token_chiffre")
        val USER_ID = longPreferencesKey("user_id")
        val ROLE = stringPreferencesKey("role")
        val NOM = stringPreferencesKey("nom")
        val PRENOM = stringPreferencesKey("prenom")
        val EMAIL = stringPreferencesKey("email")
        val STATION_ID = longPreferencesKey("station_id")
    }

    /** Copie en mémoire du jeton pour l'intercepteur HTTP (évite de déchiffrer à chaque requête). */
    @Volatile
    private var cachedToken: String? = null

    val session: Flow<Session?> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs ->
            val token = prefs[Keys.TOKEN]?.let(cipher::decrypt) ?: return@map null
            Session(
                token = token,
                userId = prefs[Keys.USER_ID] ?: return@map null,
                role = prefs[Keys.ROLE] ?: return@map null,
                nom = prefs[Keys.NOM].orEmpty(),
                prenom = prefs[Keys.PRENOM].orEmpty(),
                email = prefs[Keys.EMAIL].orEmpty(),
                stationId = prefs[Keys.STATION_ID],
            ).also { cachedToken = it.token }
        }

    suspend fun save(auth: AuthResponse) {
        dataStore.edit { p ->
            p.clear()
            p[Keys.TOKEN] = cipher.encrypt(auth.token)
            p[Keys.USER_ID] = auth.userId
            p[Keys.ROLE] = auth.role
            p[Keys.NOM] = auth.nom
            p[Keys.PRENOM] = auth.prenom
            p[Keys.EMAIL] = auth.email
            auth.stationId?.let { p[Keys.STATION_ID] = it }
        }
        cachedToken = auth.token
    }

    suspend fun updateNames(nom: String, prenom: String) {
        dataStore.edit { p ->
            p[Keys.NOM] = nom
            p[Keys.PRENOM] = prenom
        }
    }

    suspend fun clear() {
        cachedToken = null
        dataStore.edit { it.clear() }
    }

    /** Appelé depuis le thread réseau d'OkHttp (jamais depuis le thread principal). */
    fun currentTokenBlocking(): String? =
        cachedToken ?: runBlocking { session.first()?.token }
}
