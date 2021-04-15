package wangdaye.com.geometricweather.location2.services

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.provider.Settings.SettingNotFoundException
import android.text.TextUtils
import androidx.annotation.WorkerThread
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellableContinuation
import wangdaye.com.geometricweather.common.utils.LanguageUtils
import wangdaye.com.geometricweather.common.utils.helpers.LogHelper
import wangdaye.com.geometricweather.common.utils.suspendCoroutineWithTimeout
import java.io.IOException
import javax.inject.Inject
import kotlin.coroutines.resume

/**
 * Android Location service.
 */
@SuppressLint("MissingPermission")
open class AndroidLocationService @Inject constructor(
        @ApplicationContext private val context: Context) : LocationService() {

    companion object {

        private const val TIMEOUT_MILLIS = (10 * 1000).toLong()

        private fun gmsEnabled(context: Context): Boolean {
            return GoogleApiAvailability.getInstance()
                    .isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS
        }

        private fun locationEnabled(context: Context, manager: LocationManager): Boolean {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                if (!manager.isLocationEnabled) {
                    return false
                }
            } else {
                var locationMode = -1
                try {
                    locationMode = Settings.Secure.getInt(
                            context.contentResolver, Settings.Secure.LOCATION_MODE)
                } catch (e: SettingNotFoundException) {
                    e.printStackTrace()
                }
                if (locationMode == Settings.Secure.LOCATION_MODE_OFF) {
                    return false
                }
            }
            return manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
                    || manager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        }
    }

    private abstract class LocationListener : android.location.LocationListener {

        override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {
            // do nothing.
        }

        override fun onProviderEnabled(provider: String) {
            // do nothing.
        }

        override fun onProviderDisabled(provider: String) {
            // do nothing.
        }
    }

    override suspend fun getLocation(context: Context) = suspendCoroutineWithTimeout<Result?>(TIMEOUT_MILLIS) {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

        val gmsClient = if (gmsEnabled(context)) {
            LocationServices.getFusedLocationProviderClient(context)
        } else {
            null
        }

        if (locationManager == null
                || !locationEnabled(context, locationManager)
                || !hasPermissions(context)) {
            if (it.isActive) {
                it.resume(null)
            }
        }

        var networkListener: LocationListener? = null
        var gpsListener: LocationListener? = null
        var gmsListener: LocationCallback? = null

        networkListener = object : LocationListener() {
            override fun onLocationChanged(location: Location) {
                LogHelper.log("loc from network. $location")
                stopLocationUpdates(locationManager, networkListener, gpsListener, gmsClient, gmsListener)
                handleLocation(it, location)
            }
        }
        gpsListener = object : LocationListener() {
            override fun onLocationChanged(location: Location) {
                LogHelper.log("loc from gps. $location")
                stopLocationUpdates(locationManager, networkListener, gpsListener, gmsClient, gmsListener)
                handleLocation(it, location)
            }
        }
        gmsListener = object: LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                if (locationResult.locations.size > 0) {
                    stopLocationUpdates(locationManager, networkListener, gpsListener, gmsClient, gmsListener)
                    handleLocation(it, locationResult.locations[0])
                }
            }
        }
        var lastKnownLocation = getLastKnownLocation(locationManager)

        it.invokeOnCancellation { _ ->
            LogHelper.log("cancel.")
            stopLocationUpdates(locationManager, networkListener, gpsListener, gmsClient, gmsListener)
            handleLocation(it, lastKnownLocation)
        }

        if (locationManager!!.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                    0, 0f, networkListener, Looper.getMainLooper())
        }
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                    0, 0f, gpsListener, Looper.getMainLooper())
        }
        if (lastKnownLocation == null && gmsClient != null) {
            gmsClient.lastLocation.addOnSuccessListener { location: Location? -> lastKnownLocation = location
            }
        }
    }

    private fun getLastKnownLocation(locationManager: LocationManager?): Location? {
        if (locationManager == null) {
            return null
        }
        return locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                ?: locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
    }

    private fun stopLocationUpdates(locationManager: LocationManager?,
                                    networkListener: LocationListener?,
                                    gpsListener: LocationListener?,
                                    gmsClient: FusedLocationProviderClient?,
                                    gmsListener: LocationCallback?) {
        if (locationManager != null) {
            networkListener?.let {
                locationManager.removeUpdates(it)
            }
            gpsListener?.let {
                locationManager.removeUpdates(it)
            }
        }
        if (gmsClient != null && gmsListener != null) {
            gmsClient.removeLocationUpdates(gmsListener)
        }
    }

    override fun getPermissions(): Array<String> {
        return arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    private fun handleLocation(continuation: CancellableContinuation<Result?>, location: Location?) {
        if (location == null) {
            LogHelper.log("location == null.")
            if (continuation.isActive) {
                continuation.resume(null)
            }
            return
        }
        if (continuation.isActive) {
            continuation.resume(buildResult(location))
        }
    }

    @WorkerThread
    private fun buildResult(location: Location): Result {
        val result = Result(location.latitude.toFloat(), location.longitude.toFloat())
        result.hasGeocodeInformation = false
        if (!Geocoder.isPresent()) {
            return result
        }

        var addressList: List<Address>? = null
        try {
            addressList = Geocoder(context, LanguageUtils.getCurrentLocale(context))
                    .getFromLocation(
                            location.latitude,
                            location.longitude,
                            1
                    )
        } catch (e: IOException) {
            e.printStackTrace()
        }
        if (addressList == null || addressList.isEmpty()) {
            return result
        }

        result.setGeocodeInformation(
                addressList[0].countryName,
                addressList[0].adminArea,
                if (TextUtils.isEmpty(addressList[0].locality)) {
                    addressList[0].subAdminArea
                } else {
                    addressList[0].locality
                },
                addressList[0].subLocality
        )

        val countryCode = addressList[0].countryCode

        if (TextUtils.isEmpty(countryCode)) {
            if (TextUtils.isEmpty(result.country)) {
                result.inChina = false
            } else {
                result.inChina = result.country == "中国"
                        || result.country == "香港"
                        || result.country == "澳门"
                        || result.country == "台湾"
                        || result.country == "China"
            }
        } else {
            result.inChina = countryCode == "CN"
                    || countryCode == "cn"
                    || countryCode == "HK"
                    || countryCode == "hk"
                    || countryCode == "TW"
                    || countryCode == "tw"
        }

        LogHelper.log("return $result")
        return result
    }
}