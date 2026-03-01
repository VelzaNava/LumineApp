package com.thesis.lumine.data.repository

import com.thesis.lumine.data.api.RetrofitClient
import com.thesis.lumine.data.model.*

class JewelryRepository {

    private val api = RetrofitClient.apiService

    // Auth
    suspend fun sendOtp(email: String) = api.sendOtp(OtpRequest(email))

    suspend fun verifyOtp(email: String, token: String) =
        api.verifyOtp(VerifyOtpRequest(email, token))

    // Jewelry operations
    suspend fun getAllJewelry() = api.getAllJewelry()

    suspend fun getJewelryById(id: String) = api.getJewelryById(id)

    suspend fun createJewelry(jewelry: Jewelry) = api.createJewelry(jewelry)

    suspend fun updateJewelry(id: String, jewelry: Jewelry) =
        api.updateJewelry(id, jewelry)

    suspend fun deleteJewelry(id: String) = api.deleteJewelry(id)
}