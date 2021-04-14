package wangdaye.com.geometricweather.main

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.withContext
import wangdaye.com.geometricweather.common.basic.models.Location
import wangdaye.com.geometricweather.common.basic.models.Response
import wangdaye.com.geometricweather.common.basic.models.weather.Weather
import wangdaye.com.geometricweather.db.DatabaseHelper
import wangdaye.com.geometricweather.location2.LocationHelper
import wangdaye.com.geometricweather.weather2.WeatherHelper
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList

@ObsoleteCoroutinesApi
class MainActivityRepository @Inject constructor(private val locationHelper: LocationHelper,
                                                 private val weatherHelper: WeatherHelper) {

    private val singleThreadDispatcher = newSingleThreadContext(
            "com.wangdaye.geometricweather.main.repo")

    suspend fun getLocationList(context: Context, oldList: List<Location>): List<Location> {
        return withContext(singleThreadDispatcher) {
            val list = DatabaseHelper.getInstance(context).readLocationList()
            for (oldOne in oldList) {
                for (newOne in list) {
                    if (newOne.equals(oldOne)) {
                        newOne.weather = oldOne.weather
                        break
                    }
                }
            }
            return@withContext list
        }
    }

    suspend fun getWeatherCaches(context: Context, list: List<Location>): List<Location> {
        return withContext(singleThreadDispatcher) {
            for (location in list) {
                location.weather = DatabaseHelper.getInstance(context).readWeather(location)
            }
            return@withContext list;
        }
    }

    suspend fun writeLocation(context: Context, location: Location) {
        withContext(singleThreadDispatcher) {
            DatabaseHelper.getInstance(context).writeLocation(location)
            if (location.weather != null) {
                DatabaseHelper.getInstance(context).writeWeather(location, location.weather!!)
            }
        }
    }

    suspend fun writeLocationList(context: Context, locationList: List<Location>) {
        withContext(singleThreadDispatcher) {
            DatabaseHelper.getInstance(context).writeLocationList(locationList)
        }
    }

    suspend fun writeLocationList(context: Context, locationList: List<Location>, newIndex: Int) {
        withContext(singleThreadDispatcher) {
            DatabaseHelper.getInstance(context).writeLocationList(locationList)
            locationList[newIndex].weather?.let {
                DatabaseHelper.getInstance(context).writeWeather(locationList[newIndex], it)
            }
        }
    }

    suspend fun deleteLocation(context: Context, location: Location) {
        withContext(singleThreadDispatcher) {
            DatabaseHelper.getInstance(context).deleteLocation(location)
            DatabaseHelper.getInstance(context).deleteWeather(location)
        }
    }

    suspend fun getLocation(context: Context, location: Location): Response<Location?> {
        return withContext(Dispatchers.IO) {
            locationHelper.getLocation(context, location, false)
        }
    }

    suspend fun getWeather(context: Context, location: Location): Response<Weather?> {
        return withContext(Dispatchers.IO) {
            weatherHelper.getWeather(context, location)
        }
    }

    fun getLocatePermissionList(context: Context): List<String> {
        val list = ArrayList<String>()
        locationHelper.getPermissions(context).forEach {
            list.add(it)
        }
        return list
    }

    fun destroy() {
        singleThreadDispatcher.close()
    }
}