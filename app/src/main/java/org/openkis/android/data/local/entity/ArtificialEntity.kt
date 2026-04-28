package org.openkis.android.data.local.entity

import androidx.room.Entity

@Entity(tableName = "artificials", primaryKeys = ["serverUrl", "code"])
data class ArtificialEntity(
    val code: String,
    val name: String = "",
    val synonyms: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val elevation: String = "",
    val depthTotal: String = "",
    val lengthTotal: String = "",
    val year: String = "",
    val epoch: String = "",
    val typology: String = "",
    val category: String = "",
    val description: String = "",
    val address: String = "",
    val region: String = "",
    val province: String = "",
    val municipality: String = "",
    val locality: String = "",
    val photoUrl: String = "",
    val serverUrl: String = "",
    val lastSync: Long = System.currentTimeMillis()
)
