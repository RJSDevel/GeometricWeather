package wangdaye.com.geometricweather.location2.services.ip

import retrofit2.http.GET
import retrofit2.http.Query

interface BaiduIPLocationApi {
    @GET("location/ip")
    fun getLocation(
            @Query("ak") ak: String,
            @Query("coor") coor: String
    ): BaiduIPLocationResult
}