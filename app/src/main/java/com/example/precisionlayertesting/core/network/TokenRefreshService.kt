package com.example.precisionlayertesting.core.network

import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Thin data class used only for the synchronous token refresh call.
 * Kept minimal to avoid creating circular dependencies.
 */
data class RefreshTokenResponse(
    val access_token: String,
    val refresh_token: String
)

/**
 * A plain Retrofit service (NOT suspend) that can be called synchronously
 * from inside OkHttp's Authenticator (which runs on a background thread).
 */
interface TokenRefreshService {
    @POST("auth/v1/token?grant_type=refresh_token")
    fun refreshToken(@Body body: Map<String, String>): Call<RefreshTokenResponse>
}

/**
 * Helper that wraps the synchronous Retrofit call and returns the response or null.
 * Called by RetrofitClient.tokenAuthenticator.
 */
fun TokenRefreshService.refreshTokenBlocking(refreshToken: String): RefreshTokenResponse? {
    return try {
        val call = this.refreshToken(mapOf("refresh_token" to refreshToken))
        val response = call.execute()
        if (response.isSuccessful) response.body() else null
    } catch (e: Exception) {
        null
    }
}

/**
 * Separate OkHttpClient without the Authenticator to avoid re-entrant refresh calls.
 */
object PlainHttpClient {
    private val plainClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val req = chain.request().newBuilder()
                .header("apikey", SupabaseConfig.API_KEY)
                .build()
            chain.proceed(req)
        }
        .build()

    val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(SupabaseConfig.BASE_URL)
        .client(plainClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val tokenRefreshService: TokenRefreshService by lazy {
        retrofit.create(TokenRefreshService::class.java)
    }
}
