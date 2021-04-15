package wangdaye.com.geometricweather.weather2.services

import android.content.Context
import android.text.TextUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import wangdaye.com.geometricweather.BuildConfig
import wangdaye.com.geometricweather.common.basic.models.Location
import wangdaye.com.geometricweather.common.basic.models.weather.Weather
import wangdaye.com.geometricweather.settings.ConfigStore
import wangdaye.com.geometricweather.settings.SettingsManager
import wangdaye.com.geometricweather.weather2.apis.AtmoAuraIqaApi
import wangdaye.com.geometricweather.weather2.apis.MfWeatherApi
import wangdaye.com.geometricweather.weather2.converters.MfResultConverter
import wangdaye.com.geometricweather.weather2.json.mf.*
import java.util.*
import javax.inject.Inject

/**
 * Mf weather service.
 */
class MfWeatherService @Inject constructor(@ApplicationContext context: Context,
                                           private val mfApi: MfWeatherApi,
                                           private val atmoAuraApi: AtmoAuraIqaApi) : WeatherService() {

    companion object {
        private const val CONFIG_NAME_LOCAL = "LOCAL_PREFERENCE_MF"
        private const val KEY_OLD_DISTRICT = "OLD_DISTRICT"
        private const val KEY_OLD_CITY = "OLD_CITY"
        private const val KEY_OLD_PROVINCE = "OLD_PROVINCE"
        private const val KEY_OLD_KEY = "OLD_KEY"
    }

    private val config = ConfigStore.getInstance(context, CONFIG_NAME_LOCAL)

    override suspend fun getWeather(context: Context, location: Location): Weather? = coroutineScope {
        val languageCode = SettingsManager.getInstance(context).getLanguage().code

        val current = async {
            mfApi.getCurrent(
                    location.latitude.toDouble(),
                    location.longitude.toDouble(),
                    languageCode,
                    BuildConfig.MF_WSFT_KEY
            )
        }
        val forecast = async {
            mfApi.getForecast(
                    location.latitude.toDouble(),
                    location.longitude.toDouble(),
                    languageCode,
                    BuildConfig.MF_WSFT_KEY
            )
        }

        // TODO: Will allow us to display forecast for day and night in daily
        //Observable<MfForecastResult> dayNightForecast = api.getForecastInstants(
        //        location.getLatitude(), location.getLongitude(), languageCode, "afternoon,night", BuildConfig.MF_WSFT_KEY);
        val ephemeris = async {
            mfApi.getEphemeris(
                    location.latitude.toDouble(),
                    location.longitude.toDouble(),
                    "en",
                    BuildConfig.MF_WSFT_KEY
            )
        }
        // English required to convert moon phase
        val rain = async {
            mfApi.getRain(
                    location.latitude.toDouble(),
                    location.longitude.toDouble(),
                    languageCode,
                    BuildConfig.MF_WSFT_KEY
            )
        }
        val warnings = async {
            mfApi.getWarnings(location.province, null, BuildConfig.MF_WSFT_KEY)
        }
        val aqiAtmoAura = async {
            try {
                if (location.province == "Auvergne-Rhône-Alpes"
                        || location.province == "01"
                        || location.province == "03"
                        || location.province == "07"
                        || location.province == "15"
                        || location.province == "26"
                        || location.province == "38"
                        || location.province == "42"
                        || location.province == "43"
                        || location.province == "63"
                        || location.province == "69"
                        || location.province == "73"
                        || location.province == "74") {
                    atmoAuraApi.getQAFull(
                            BuildConfig.IQA_ATMO_AURA_KEY,
                            location.latitude.toString(),
                            location.longitude.toString()
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            null
        }

        return@coroutineScope MfResultConverter.convert(
                context,
                location,
                current.await(),
                forecast.await(),
                ephemeris.await(),
                rain.await(),
                warnings.await(),
                aqiAtmoAura.await()
        ).result
    }

    override suspend fun getLocation(context: Context, query: String): List<Location> = coroutineScope {
        val resultList = mfApi.callWeatherLocation(query, 48.86, 2.34, BuildConfig.MF_WSFT_KEY)

        val locationList: MutableList<Location> = ArrayList()
        for (r in resultList) {
            if (r.postCode != null) {
                locationList.add(MfResultConverter.convert(null, r))
            }
        }
        return@coroutineScope locationList
    }

    override suspend fun getLocation(context: Context, location: Location): List<Location> = coroutineScope {
        val oldDistrict = config.getString(KEY_OLD_DISTRICT, "")
        val oldCity = config.getString(KEY_OLD_CITY, "")
        val oldProvince = config.getString(KEY_OLD_PROVINCE, "")
        val oldKey = config.getString(KEY_OLD_KEY, "")

        if (location.hasGeocodeInformation()
                && queryEqualsIgnoreEmpty(location.district, oldDistrict)
                && queryEquals(location.city, oldCity)
                && queryEquals(location.province, oldProvince)
                && queryEquals(location.cityId, oldKey)) {

            return@coroutineScope ArrayList<Location>()
        }
        config.edit()
                .putString(KEY_OLD_DISTRICT, location.district)
                .putString(KEY_OLD_CITY, location.city)
                .putString(KEY_OLD_PROVINCE, location.province)
                .apply()

        val languageCode = SettingsManager.getInstance(context).getLanguage().code

        val result = mfApi.getForecastV2(
                location.latitude.toDouble(),
                location.longitude.toDouble(),
                languageCode,
                BuildConfig.MF_WSFT_KEY
        )

        val locationList = ArrayList<Location>()
        if (result.properties.insee != null) {
            locationList.add(MfResultConverter.convert(null, result))
        }

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

        return@coroutineScope locationList
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