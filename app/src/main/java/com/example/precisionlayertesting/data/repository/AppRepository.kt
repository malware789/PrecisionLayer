package com.example.precisionlayertesting.data.repository

import com.example.precisionlayertesting.core.utils.Result
import com.example.precisionlayertesting.data.models.DummyModel
import com.example.precisionlayertesting.data.remote.ApiService

class AppRepository(private val apiService: ApiService) {
    suspend fun getDummyData(): Result<List<DummyModel>> {
        return try {
            val response = apiService.getDummyData()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Result.Success(body)
                } else {
                    Result.Error(Exception("Empty body response"))
                }
            } else {
                Result.Error(Exception("Error: code ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
