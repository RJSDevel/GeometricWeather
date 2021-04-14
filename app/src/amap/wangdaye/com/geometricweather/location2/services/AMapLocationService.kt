package wangdaye.com.geometricweather.location2.services

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.AMapLocationClientOption
import com.amap.api.location.CoordinateConverter
import kotlinx.coroutines.suspendCancellableCoroutine
import wangdaye.com.geometricweather.GeometricWeather
import wangdaye.com.geometricweather.common.utils.helpers.BuglyHelper
import wangdaye.com.geometricweather.location2.utils.LocationException
import javax.inject.Inject
import kotlin.coroutines.resume

/**
 * A map location service.
 */
class AMapLocationService @Inject constructor() : LocationService() {

    override suspend fun getLocation(context: Context) = suspendCancellableCoroutine<Result?> {

        val option = AMapLocationClientOption()
        option.locationMode = AMapLocationClientOption.AMapLocationMode.Battery_Saving
        option.isOnceLocation = true
        option.isOnceLocationLatest = true
        option.isNeedAddress = true
        option.isMockEnable = false
        option.isLocationCacheEnable = false

        val client = AMapLocationClient(context.applicationContext)
        client.setLocationOption(option)
        client.setLocationListener { aMapLocation ->

            cancel(client)

            if (aMapLocation.errorCode == 0) {
                val result = Result(
                        aMapLocation.latitude.toFloat(),
                        aMapLocation.longitude.toFloat()
                )
                result.setGeocodeInformation(
                        aMapLocation.country,
                        aMapLocation.province,
                        aMapLocation.city,
                        aMapLocation.district
                )
                result.inChina = CoordinateConverter.isAMapDataAvailable(
                        aMapLocation.latitude,
                        aMapLocation.longitude
                )
                it.resume(result)
            } else {
                BuglyHelper.report(
                        LocationException(
                                aMapLocation.errorCode,
                                aMapLocation.errorInfo
                        )
                )
                it.resume(null)
            }
        }

        it.invokeOnCancellation {
            cancel(client)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManagerCompat.from(context).createNotificationChannel(
                    getLocationNotificationChannel(context))
            client.enableBackgroundLocation(
                    GeometricWeather.NOTIFICATION_ID_LOCATION,
                    getLocationNotification(context))
        }
        client.startLocation()
    }

    fun cancel(client: AMapLocationClient) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            client.disableBackgroundLocation(true)
        }
        client.stopLocation()
        client.onDestroy()
    }

    override fun getPermissions(): Array<String> {
        return arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }
}