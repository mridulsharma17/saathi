package com.example.saathi.api

import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

// Request Models
data class SendOtpRequest(val email: String)
data class VerifyOtpRequest(val email: String, val otp: String)

// Response Models
data class OtpResponse(
    val success: Boolean,
    val message: String
)

interface ApiService {
    @POST("api/send-otp")
    suspend fun sendOtp(@Body request: SendOtpRequest): Response<OtpResponse>

    @POST("api/verify-otp")
    suspend fun verifyOtp(@Body request: VerifyOtpRequest): Response<OtpResponse>
}

object RetrofitClient {
    private const val BASE_URL = "https://saathi-backend-3j4n.onrender.com/"

    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }
}
