package org.openkis.android.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "servers")
data class ServerEntity(
    @PrimaryKey val url: String,
    val name: String = "",
    val lastSync: Long = 0L,
    val addedAt: Long = System.currentTimeMillis(),
    val visible: Boolean = true,
    val syncCaves: Boolean = true,
    val syncSprings: Boolean = true,
    val syncArtificials: Boolean = true
)
