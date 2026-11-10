package com.umkt.ucopy.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import java.util.UUID

@Entity(tableName = "clips")
@Serializable
data class Clip(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false,
    val category: String? = null, // Now acts as Label
    val isTodo: Boolean = false,
    val todoItems: List<Task> = emptyList()
)
