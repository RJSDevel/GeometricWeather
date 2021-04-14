package wangdaye.com.geometricweather.location2.services.ip

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import wangdaye.com.geometricweather.BuildConfig
import wangdaye.com.geometricweather.location2.services.LocationService
import javax.inject.Inject

class BaiduIPLocationService @Inject constructor(private val api: BaiduIPLocationApi) : LocationService() {

    override suspend fun getLocation(context: Context): Result? {
        return withContext(Dispatchers.IO) {
            val ipResult = api.getLocation(BuildConfig.BAIDU_IP_LOCATION_AK, "gcj02")
            val result = Result(ipResult.content.point.y.toFloat(), ipResult.content.point.x.toFloat())

            result.setGeocodeInformation(
                    "中国",
                    ipResult.content.address_detail.province,
                    ipResult.content.address_detail.city,
                    ipResult.content.address_detail.district
            )
            result.inChina = true
            return@withContext result
        }
    }

    override fun getPermissions(): Array<String> = emptyArray()

    override fun hasPermissions(context: Context?): Boolean {
        return true
    }
}