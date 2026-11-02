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
    // Legacy OpenKIS field depth_total: Dislivello totale / total vertical extent.
    val depthTotal: String = "",
    val depthNegative: String = "",
    val depthPositive: String = "",
    // Legacy OpenKIS field lenght_total: Sviluppo reale / surveyed 3D development.
    val lengthTotal: String = "",
    val closed: String = "",
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
    val dbId: String = "",
    val lastSync: Long = System.currentTimeMillis()
)
