package com.umkt.ucopy.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class Task(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val isCompleted: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val dueDate: Long? = null
)
