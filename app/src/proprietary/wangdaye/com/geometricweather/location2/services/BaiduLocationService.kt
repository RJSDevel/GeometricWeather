package wangdaye.com.geometricweather.location2.services

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import com.baidu.location.BDAbstractLocationListener
import com.baidu.location.BDLocation
import com.baidu.location.LocationClient
import com.baidu.location.LocationClientOption
import kotlinx.coroutines.suspendCancellableCoroutine
import wangdaye.com.geometricweather.GeometricWeather
import wangdaye.com.geometricweather.common.utils.helpers.BuglyHelper
import wangdaye.com.geometricweather.location2.utils.LocationException
import javax.inject.Inject
import kotlin.coroutines.resume

/**
 * Baidu location service.
 */
class BaiduLocationService @Inject constructor() : LocationService() {

    override suspend fun getLocation(context: Context) = suspendCancellableCoroutine<Result?> {
        val option = LocationClientOption()
        option.locationMode = LocationClientOption.LocationMode.Battery_Saving // 可选，默认高精度，设置定位模式，高精度，低功耗，仅设备
        option.setCoorType("wgs84") // 可选，默认gcj02，设置返回的定位结果坐标系
        option.setScanSpan(0) // 可选，默认0，即仅定位一次，设置发起定位请求的间隔需要大于等于1000ms才是有效的
        option.setIsNeedAddress(true) // 可选，设置是否需要地址信息，默认不需要
        option.isOpenGps = false // 可选，默认false,设置是否使用gps
        option.isLocationNotify = false // 可选，默认false，设置是否当gps有效时按照1S1次频率输出GPS结果
        option.setIsNeedLocationDescribe(false) // 可选，默认false，设置是否需要位置语义化结果，可以在BDLocation.getLocationDescribe里得到，结果类似于“在北京天安门附近”
        option.setIsNeedLocationPoiList(false) // 可选，默认false，设置是否需要POI结果，可以在BDLocation.getPoiList里得到
        option.setIgnoreKillProcess(false) // 可选，默认true，定位SDK内部是一个SERVICE，并放到了独立进程，设置是否在stop的时候杀死这个进程，默认不杀死
        option.SetIgnoreCacheException(true) // 可选，默认false，设置是否收集CRASH信息，默认收集
        option.setEnableSimulateGps(false) // 可选，默认false，设置是否需要过滤gps仿真结果，默认需要
        option.setWifiCacheTimeOut(5 * 60 * 1000) // 可选，如果设置了该接口，首次启动定位时，会先判断当前WiFi是否超出有效期，若超出有效期，会先重新扫描WiFi，然后定位

        val client = LocationClient(context.applicationContext)
        client.locOption = option
        client.registerLocationListener(object : BDAbstractLocationListener() {
            override fun onReceiveLocation(bdLocation: BDLocation) {
                cancel(client)

                when (bdLocation.locType) {
                    61, 161 -> {
                        val result = Result(
                                bdLocation.latitude.toFloat(),
                                bdLocation.longitude.toFloat()
                        )
                        result.setGeocodeInformation(
                                bdLocation.country,
                                bdLocation.province,
                                bdLocation.city,
                                bdLocation.district
                        )
                        result.inChina = bdLocation.locationWhere == BDLocation.LOCATION_WHERE_IN_CN
                        it.resume(result)
                    }
                    else -> {
                        BuglyHelper.report(
                                LocationException(
                                        bdLocation.locType,
                                        bdLocation.locTypeDescription
                                )
                        )
                        it.resume(null)
                    }
                }
            }
        })

        it.invokeOnCancellation {
            cancel(client)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManagerCompat.from(context).createNotificationChannel(
                    getLocationNotificationChannel(context))
            client.enableLocInForeground(
                    GeometricWeather.NOTIFICATION_ID_LOCATION,
                    getLocationNotification(context))
        }
        client.start()
    }

    fun cancel(client: LocationClient) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            client.disableLocInForeground(true)
        }
        client.stop()
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