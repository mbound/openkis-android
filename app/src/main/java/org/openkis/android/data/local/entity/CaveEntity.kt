package org.openkis.android.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "caves")
data class CaveEntity(
    @PrimaryKey val code: String,
    val name: String = "",
    val synonyms: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val elevation: String = "",
    val depthTotal: String = "",
    val depthNegative: String = "",
    val depthPositive: String = "",
    val lengthTotal: String = "",
    val hydrology: String = "",
    val meteorology: String = "",
    val description: String = "",
    val region: String = "",
    val province: String = "",
    val municipality: String = "",
    val locality: String = "",
    val closed: String = "",
    val photoUrl: String = "",
    val serverUrl: String = "",
    val lastSync: Long = System.currentTimeMillis()
)
