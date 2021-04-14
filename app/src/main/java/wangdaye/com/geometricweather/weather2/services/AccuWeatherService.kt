package wangdaye.com.geometricweather.weather2.services

import android.content.Context
import android.text.TextUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import wangdaye.com.geometricweather.BuildConfig
import wangdaye.com.geometricweather.common.basic.models.Location
import wangdaye.com.geometricweather.common.basic.models.weather.Weather
import wangdaye.com.geometricweather.settings.ConfigStore
import wangdaye.com.geometricweather.settings.SettingsManager
import wangdaye.com.geometricweather.weather2.apis.AccuWeatherApi
import wangdaye.com.geometricweather.weather2.converters.AccuResultConverter
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList

/**
 * Accu weather service.
 */
class AccuWeatherService @Inject constructor(
        @ApplicationContext context: Context,
        private val api: AccuWeatherApi,
) : WeatherService() {

    companion object {
        private const val CONFIG_NAME_LOCAL = "LOCAL_PREFERENCE"
        private const val KEY_OLD_DISTRICT = "OLD_DISTRICT"
        private const val KEY_OLD_CITY = "OLD_CITY"
        private const val KEY_OLD_PROVINCE = "OLD_PROVINCE"
        private const val KEY_OLD_KEY = "OLD_KEY"
    }

    private val config = ConfigStore.getInstance(context, CONFIG_NAME_LOCAL)

    override suspend fun getWeather(context: Context, location: Location): Weather? {
        return withContext(Dispatchers.IO) {
            val languageCode = SettingsManager.getInstance(context).getLanguage().code

            val realtime = async {
                api.callCurrent(
                        location.cityId, BuildConfig.ACCU_CURRENT_KEY, languageCode, true)
            }
            val daily = async {
                api.callDaily(
                        location.cityId, BuildConfig.ACCU_WEATHER_KEY, languageCode, metric = true, details = true)
            }
            val hourly = async {
                api.callHourly(
                        location.cityId, BuildConfig.ACCU_WEATHER_KEY, languageCode, true)
            }
            val minutely = async {
                try {
                    api.callMinutely(
                            BuildConfig.ACCU_WEATHER_KEY,
                            languageCode,
                            true,
                            location.latitude.toString() + "," + location.longitude
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }
            val alert = async {
                try {
                    api.callAlert(location.cityId, BuildConfig.ACCU_WEATHER_KEY, languageCode, true)
                } catch (e: Exception) {
                    e.printStackTrace()
                    ArrayList()
                }
            }
            val aqi = async {
                try {
                    api.callAirQuality(location.cityId, BuildConfig.ACCU_AQI_KEY)
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }

            return@withContext AccuResultConverter.convert(
                    context,
                    location,
                    realtime.await()[0],
                    daily.await(),
                    hourly.await(),
                    minutely.await(),
                    aqi.await(),
                    alert.await()
            )
        }.result
    }

    override suspend fun getLocation(context: Context, query: String): List<Location> {
        return withContext(Dispatchers.IO) {
            val languageCode = SettingsManager.getInstance(context).getLanguage().code
            val resultList = api.callWeatherLocation(
                    "Always",
                    BuildConfig.ACCU_WEATHER_KEY,
                    query,
                    languageCode
            )

            val zipCode = if (query.matches(Regex("[a-zA-Z0-9]*"))) {
                query
            } else {
                null
            }

            val locationList = ArrayList<Location>()
            for (r in resultList) {
                locationList.add(AccuResultConverter.convert(null, r, zipCode))
            }

            return@withContext locationList
        }
    }

    override suspend fun getLocation(context: Context, location: Location): List<Location> {
        return withContext(Dispatchers.IO) {

            val oldDistrict = config.getString(KEY_OLD_DISTRICT, "")
            val oldCity = config.getString(KEY_OLD_CITY, "")
            val oldProvince = config.getString(KEY_OLD_PROVINCE, "")
            val oldKey = config.getString(KEY_OLD_KEY, "")

            if (location.hasGeocodeInformation()
                    && queryEqualsIgnoreEmpty(location.district, oldDistrict)
                    && queryEquals(location.city, oldCity)
                    && queryEquals(location.province, oldProvince)
                    && queryEquals(location.cityId, oldKey)) {

                return@withContext ArrayList<Location>()
            }

            config.edit()
                    .putString(KEY_OLD_DISTRICT, location.district)
                    .putString(KEY_OLD_CITY, location.city)
                    .putString(KEY_OLD_PROVINCE, location.province)
                    .apply()

            val languageCode = SettingsManager.getInstance(context).getLanguage().code

            val result = api.callWeatherLocationByGeoPosition(
                    "Always",
                    BuildConfig.ACCU_WEATHER_KEY,
                    location.latitude.toString() + "," + location.longitude,
                    languageCode
            )

            val locationList = ArrayList<Location>()
            locationList.add(AccuResultConverter.convert(location, result, null))

            if (locationList.isEmpty()) {
                config.edit()
                        .putString(KEY_OLD_DISTRICT, "")
                        .putString(KEY_OLD_CITY, "")
                        .putString(KEY_OLD_PROVINCE, "")
                        .putString(KEY_OLD_KEY, "")
                        .apply()
            } else if (!TextUtils.isEmpty(locationList[0].cityId)) {
                config.edit()
                        .putString(KEY_OLD_KEY, locationList[0].cityId)
                        .apply()
            }

            return@withContext locationList
        }
    }

    private fun queryEquals(a: String?, b: String?): Boolean {
        return if (!TextUtils.isEmpty(a) && !TextUtils.isEmpty(b)) {
            a == b
        } else {
            false
        }
    }

    private fun queryEqualsIgnoreEmpty(a: String?, b: String?): Boolean {
        if (TextUtils.isEmpty(a) && TextUtils.isEmpty(b)) {
            return true
        }
        return queryEquals(a, b)
    }
}