package com.example.precisionlayertesting.core.di

import android.content.Context
import com.example.precisionlayertesting.core.network.RetrofitClient
import com.example.precisionlayertesting.data.remote.ApiService
import com.example.precisionlayertesting.data.remote.BugApiService
import com.example.precisionlayertesting.data.repository.BugRepository
import com.example.precisionlayertesting.core.utils.PrefsManager
import com.example.precisionlayertesting.data.remote.AuthApiService
import com.example.precisionlayertesting.data.repository.AppRepository
import com.example.precisionlayertesting.data.repository.AuthRepository
import com.example.precisionlayertesting.core.network.PlainHttpClient
import com.example.precisionlayertesting.core.network.TokenRefreshService

object ManualDI {
    // Basic service locator for manual dependency injection

    private lateinit var appContext: Context

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    private val apiService: ApiService by lazy {
        RetrofitClient.retrofit.create(ApiService::class.java)
    }

    val appRepository: AppRepository by lazy {
        AppRepository(apiService)
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

    private val bugApiService: BugApiService by lazy {
        RetrofitClient.retrofit.create(BugApiService::class.java)
    }

    val bugRepository: BugRepository by lazy {
        BugRepository(bugApiService)
    }

    /** Used by RetrofitClient.tokenAuthenticator for synchronous token refresh. */
    val plainAuthService: TokenRefreshService by lazy {
        PlainHttpClient.tokenRefreshService
    }
}
