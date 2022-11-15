package com.shido.giffity.domain

import java.util.UUID

data class ErrorEvent(
    val id: String = UUID.randomUUID().toString(),
    val message: String
)