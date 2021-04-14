package wangdaye.com.geometricweather.weather2.services

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import wangdaye.com.geometricweather.common.basic.models.ChineseCity.CNWeatherSource
import wangdaye.com.geometricweather.common.basic.models.Location
import wangdaye.com.geometricweather.common.basic.models.weather.Weather
import wangdaye.com.geometricweather.common.utils.LanguageUtils
import wangdaye.com.geometricweather.db.DatabaseHelper
import wangdaye.com.geometricweather.weather2.apis.CNWeatherApi
import wangdaye.com.geometricweather.weather2.converters.CNResultConverter
import java.util.*
import javax.inject.Inject

/**
 * CN weather service.
 */
open class CNWeatherService @Inject constructor(private val api: CNWeatherApi) : WeatherService() {

    open val source: CNWeatherSource
        get() = CNWeatherSource.CN

    override suspend fun getWeather(context: Context, location: Location): Weather? {
        return withContext(Dispatchers.IO) {
            CNResultConverter.convert(
                    context,
                    location,
                    api.getWeather(location.cityId)
            ).result
        }
    }

    override suspend fun getLocation(context: Context, query: String): List<Location> {
        return withContext(Dispatchers.IO) {
            if (!LanguageUtils.isChinese(query)) {
                return@withContext ArrayList()
            }

            DatabaseHelper.getInstance(context).ensureChineseCityList(context)
            val cityList = DatabaseHelper.getInstance(context).readChineseCityList(query)

            val locationList = ArrayList<Location>()
            for (c in cityList) {
                locationList.add(c.toLocation(source))
            }
            return@withContext locationList
        }
    }

    override suspend fun getLocation(context: Context, location: Location): List<Location> {
        return withContext(Dispatchers.IO) {
            val hasGeocodeInformation = location.hasGeocodeInformation()

            DatabaseHelper.getInstance(context).ensureChineseCityList(context)

            val locationList = ArrayList<Location>()

            if (hasGeocodeInformation) {
                DatabaseHelper.getInstance(context).readChineseCity(
                        formatLocationString(convertChinese(location.province)),
                        formatLocationString(convertChinese(location.city)),
                        formatLocationString(convertChinese(location.district))
                )?.let {
                    locationList.add(it.toLocation(source))
                }
            }
            if (locationList.isNotEmpty()) {
                return@withContext locationList
            }

            DatabaseHelper.getInstance(context).readChineseCity(
                    location.latitude, location.longitude
            )?.let {
                locationList.add(it.toLocation(source))
            }
            return@withContext locationList
        }
    }
}