package org.openkis.android.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OpenKisResponse(
    val sourceurl: String = "",
    val name: String = "",
    val items: List<OpenKisItem> = emptyList()
)

@Serializable
data class OpenKisItem(
    val id: String = "",
    val code: String = "",
    val name: String = "",
    val synonyms: String = "",
    val latitude: String = "",
    val longitude: String = "",
    val lat: String = "",
    val lon: String = "",
    val elevation: String = "",
    @SerialName("depth_total") val depthTotal: String = "",
    @SerialName("depth_negative") val depthNegative: String = "",
    @SerialName("depth_positive") val depthPositive: String = "",
    @SerialName("lenght_total") val lengthTotal: String = "",
    val hydrology: String = "",
    val meteorology: String = "",
    val fauna: String = "",
    val closed: String = "",
    val title: String = "",
    @SerialName("abstract") val description: String = "",
    val size: String = "",
    val icon: String = "",
    val sourceurl: String = "",
    // Spring-specific
    @SerialName("flow_max") val flowMax: String = "",
    @SerialName("flow_min") val flowMin: String = "",
    @SerialName("flow_average") val flowAverage: String = "",
    @SerialName("use") val usage: String = "",
    val utilization: String = "",
    // Artificial-specific
    val year: String = "",
    val epoch: String = "",
    val typology: String = "",
    val category: String = "",
    val address: String = "",
    // Location
    val regione: String = "",
    val provincia: String = "",
    val comune: String = "",
    val localita: String = "",
    // Photo
    val photo1: String = ""
)

@Serializable
data class NearResponse(
    @SerialName("methers") val meters: Double = 0.0,
    val cave: OpenKisItem? = null
)
