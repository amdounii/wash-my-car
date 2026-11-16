package com.washmycar.data.remote

import com.google.gson.Gson
import com.washmycar.data.model.ApiErrorBody
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** Erreur lisible renvoyée aux écrans (message du backend + erreurs champ par champ). */
class ApiException(
    message: String,
    val code: Int? = null,
    val fieldErrors: Map<String, String> = emptyMap(),
) : Exception(message)

/** Exécute un appel réseau et convertit toute erreur en [ApiException] dans un [Result]. */
@Singleton
class ApiCaller @Inject constructor(private val gson: Gson) {

    suspend fun <T> call(block: suspend () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: HttpException) {
        Result.failure(parse(e.response()))
    } catch (e: IOException) {
        Result.failure(ApiException("Serveur injoignable. Vérifiez votre connexion et que l'API est démarrée."))
    } catch (e: Exception) {
        Result.failure(ApiException(e.message ?: "Erreur inattendue"))
    }

    /** Pour les routes sans contenu (204) déclarées en Response<Unit>. */
    fun <T> Response<T>.orThrow(): T? = if (isSuccessful) body() else throw HttpException(this)

    private fun parse(response: Response<*>?): ApiException {
        val code = response?.code()
        val body = runCatching {
            gson.fromJson(response?.errorBody()?.charStream(), ApiErrorBody::class.java)
        }.getOrNull()
        val message = body?.message ?: when (code) {
            401 -> "Session expirée, veuillez vous reconnecter"
            403 -> "Accès refusé"
            404 -> "Ressource introuvable"
            else -> "Erreur du serveur (${code ?: "?"})"
        }
        return ApiException(message, code, body?.erreurs.orEmpty())
    }
}
