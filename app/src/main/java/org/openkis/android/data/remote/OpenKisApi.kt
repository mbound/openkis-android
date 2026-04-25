package org.openkis.android.data.remote

import org.openkis.android.data.remote.dto.NearResponse
import org.openkis.android.data.remote.dto.OpenKisResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface OpenKisApi {

    @GET("openkis_json.php")
    suspend fun getCaves(
        @Query("mod") mod: String = "caves"
    ): OpenKisResponse

    @GET("openkis_json.php")
    suspend fun getArtificials(
        @Query("mod") mod: String = "artificials"
    ): OpenKisResponse

    @GET("openkis_json.php")
    suspend fun getSprings(
        @Query("mod") mod: String = "springs"
    ): OpenKisResponse

    @GET("openkis_API.php")
    suspend fun getNearestCave(
        @Query("op") op: String = "near",
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("mod") mod: String = "caves"
    ): NearResponse
}
