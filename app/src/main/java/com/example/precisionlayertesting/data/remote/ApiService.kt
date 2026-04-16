package com.example.precisionlayertesting.data.remote

import com.example.precisionlayertesting.data.models.DummyModel
import retrofit2.Response
import retrofit2.http.GET

interface ApiService {
    @GET("placeholder")
    suspend fun getDummyData(): Response<List<DummyModel>>
}
