package com.example.precisionlayertesting.data.remote

import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.PUT
import retrofit2.http.Url

interface R2ApiService {

    @PUT
    suspend fun uploadFile(
        @Url uploadUrl: String,
        @Header("Content-Type") contentType: String = "application/vnd.android.package-archive",
        @Body file: RequestBody
    ): Response<Unit>
}
