package com.example.precisionlayertesting.core.di

import android.content.Context
import com.example.precisionlayertesting.core.network.RetrofitClient
import com.example.precisionlayertesting.core.remote.BugApiService
import com.example.precisionlayertesting.core.repository.BugRepository
import com.example.precisionlayertesting.core.utils.PrefsManager
import com.example.precisionlayertesting.core.remote.AuthApiService
import com.example.precisionlayertesting.core.repository.AuthRepository
import com.example.precisionlayertesting.core.network.PlainHttpClient
import com.example.precisionlayertesting.core.network.TokenRefreshService
import com.example.precisionlayertesting.core.remote.R2ApiService
import com.example.precisionlayertesting.core.utils.ApkMetadataExtractor
import retrofit2.converter.gson.GsonConverterFactory

object ManualDI {
    // Basic service locator for manual dependency injection

    private lateinit var appContext: Context

    fun init(context: Context) {
        appContext = context.applicationContext
    }


    private val authApiService: AuthApiService by lazy {
        RetrofitClient.retrofit.create(AuthApiService::class.java)
    }

    val prefsManager: PrefsManager by lazy {
        PrefsManager(appContext)
    }

    val authRepository: AuthRepository by lazy {
        AuthRepository(authApiService, prefsManager)
    }

    val apkMetadataExtractor: ApkMetadataExtractor by lazy {
        ApkMetadataExtractor(appContext)
    }

    // Separate Retrofit for BugApiService — uses serializeNulls() so all batch
    // request objects have identical JSON keys, avoiding PostgREST PGRST102 errors.
    private val bugGson: com.google.gson.Gson = com.google.gson.GsonBuilder()
        .serializeNulls()
        .create()

    private val bugRetrofit: retrofit2.Retrofit by lazy {
        retrofit2.Retrofit.Builder()
            .baseUrl(RetrofitClient.retrofit.baseUrl())
            .client(RetrofitClient.okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(bugGson))
            .build()
    }

    private val bugApiService: BugApiService by lazy {
        bugRetrofit.create(BugApiService::class.java)
    }

    private val r2Retrofit: retrofit2.Retrofit by lazy {
        retrofit2.Retrofit.Builder()
            .baseUrl("https://placeholder.com/") // Will be overridden by @Url
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private val r2ApiService: R2ApiService by lazy {
        r2Retrofit.create(R2ApiService::class.java)
    }

    val bugRepository: BugRepository by lazy {
        BugRepository(bugApiService, r2ApiService)
    }

    /** Used by RetrofitClient.tokenAuthenticator for synchronous token refresh. */
    val plainAuthService: TokenRefreshService by lazy {
        PlainHttpClient.tokenRefreshService
    }
}
