package com.thesis.lumine.data.model

data class OtpRequest(
    val email: String
)

data class VerifyOtpRequest(
    val email: String,
    val token: String
)

data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val email: String,
    val userId: String
)