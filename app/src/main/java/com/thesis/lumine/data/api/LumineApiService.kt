package com.thesis.lumine.data.api

import com.thesis.lumine.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface LumineApiService {

    // Auth endpoints
    @POST("api/auth/send-otp")
    suspend fun sendOtp(@Body request: OtpRequest): Response<Map<String, String>>

    @POST("api/auth/verify-otp")
    suspend fun verifyOtp(@Body request: VerifyOtpRequest): Response<AuthResponse>

    // Jewelry endpoints
    @GET("api/jewelry")
    suspend fun getAllJewelry(): Response<List<Jewelry>>

    @GET("api/jewelry/{id}")
    suspend fun getJewelryById(@Path("id") id: String): Response<Jewelry>

    @POST("api/jewelry")
    suspend fun createJewelry(@Body jewelry: Jewelry): Response<Jewelry>

    @PUT("api/jewelry/{id}")
    suspend fun updateJewelry(
        @Path("id") id: String,
        @Body jewelry: Jewelry
    ): Response<Jewelry>

    @DELETE("api/jewelry/{id}")
    suspend fun deleteJewelry(@Path("id") id: String): Response<Unit>
}