package com.ghostreborn.akira.features.auth.models

data class AuthResult(
    val accessToken: String,
    val refreshToken: String,
    val sessionId: String,
    val username: String,
    val displayName: String? = null,
    val picture: String? = null,
    val userId: String? = null,
    val email: String? = null,
    val isEmailVerified: Boolean = false,
    val rawResponseJson: String = ""
)
