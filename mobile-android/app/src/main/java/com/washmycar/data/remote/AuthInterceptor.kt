package com.washmycar.data.remote

import com.washmycar.data.local.SessionStore
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Ajoute « Authorization: Bearer <jeton> » aux requêtes.
 * Un 401 sur une requête authentifiée (jeton expiré, compte supprimé ou désactivé)
 * vide la session : la navigation renvoie alors vers l'écran de connexion.
 */
@Singleton
class AuthInterceptor @Inject constructor(private val sessionStore: SessionStore) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val isAuthRoute = original.url.encodedPath.startsWith("/api/auth/")
        val token = if (isAuthRoute) null else sessionStore.currentTokenBlocking()

        val request = if (token != null) {
            original.newBuilder().header("Authorization", "Bearer $token").build()
        } else {
            original
        }
        val response = chain.proceed(request)
        if (response.code == 401 && token != null) {
            runBlocking { sessionStore.clear() }
        }
        return response
    }
}
