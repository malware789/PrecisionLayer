package com.example.precisionlayertesting.core.network

import com.example.precisionlayertesting.core.di.ManualDI
import okhttp3.Authenticator
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    /**
     * OkHttp Authenticator — called automatically when the server returns HTTP 401.
     * It refreshes the JWT using the stored refresh_token, saves the new tokens,
     * and retries the original request with the updated Bearer header.
     *
     * If refresh fails (no refresh token / server error), returns null to cancel the request.
     * Prevents infinite loops by inspecting the prior 401 count.
     */
    private val tokenAuthenticator = object : Authenticator {
        override fun authenticate(route: Route?, response: Response): Request? {
            // Prevent infinite retry loops — give up after 2 attempts
            if (response.priorResponse()?.code() == 401) return null

            val refreshToken = try {
                ManualDI.prefsManager.getRefreshToken()
            } catch (e: Exception) {
                null
            } ?: return null

            // Synchronously call the refresh endpoint (OkHttp authenticator is blocking)
            return try {
                val authService = ManualDI.plainAuthService
                val refreshResponse = authService.refreshTokenBlocking(refreshToken)
                if (refreshResponse != null) {
                    ManualDI.prefsManager.saveAccessToken(refreshResponse.access_token)
                    ManualDI.prefsManager.saveRefreshToken(refreshResponse.refresh_token)
                    // Retry original request with new token
                    response.request().newBuilder()
                        .header("Authorization", "Bearer ${refreshResponse.access_token}")
                        .build()
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    private val authInterceptor = okhttp3.Interceptor { chain ->
        val token = try {
            ManualDI.prefsManager.getAccessToken() ?: SupabaseConfig.API_KEY
        } catch (e: Exception) {
            SupabaseConfig.API_KEY
        }

        val request = chain.request().newBuilder()
            .header("apikey", SupabaseConfig.API_KEY)
            .header("Authorization", "Bearer $token")
            .build()
        chain.proceed(request)
    }

    internal val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .authenticator(tokenAuthenticator)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(SupabaseConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}
