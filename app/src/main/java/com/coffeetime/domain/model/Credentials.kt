package com.coffeetime.domain.model

data class Credentials(
    val pinHash: String,
    val pinSalt: String
)