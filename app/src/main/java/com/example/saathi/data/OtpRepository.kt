package com.example.saathi.data

import com.example.saathi.api.OtpResponse
import com.example.saathi.api.RetrofitClient
import com.example.saathi.api.SendOtpRequest
import com.example.saathi.api.VerifyOtpRequest
import retrofit2.Response

class OtpRepository {
    suspend fun sendOtp(email: String): Response<OtpResponse> {
        return RetrofitClient.apiService.sendOtp(SendOtpRequest(email))
    }

    suspend fun verifyOtp(email: String, otp: String): Response<OtpResponse> {
        return RetrofitClient.apiService.verifyOtp(VerifyOtpRequest(email, otp))
    }
}
