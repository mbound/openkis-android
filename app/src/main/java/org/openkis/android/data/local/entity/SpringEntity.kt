package org.openkis.android.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "springs")
data class SpringEntity(
    @PrimaryKey val code: String,
    val name: String = "",
    val caveCode: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val elevation: String = "",
    val flowMax: String = "",
    val flowMin: String = "",
    val flowAverage: String = "",
    val usage: String = "",
    val utilization: String = "",
    val description: String = "",
    val region: String = "",
    val province: String = "",
    val municipality: String = "",
    val photoUrl: String = "",
    val serverUrl: String = "",
    val lastSync: Long = System.currentTimeMillis()
)
