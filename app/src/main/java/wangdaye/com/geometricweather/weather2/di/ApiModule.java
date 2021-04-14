package wangdaye.com.geometricweather.weather2.di;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.components.ApplicationComponent;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import wangdaye.com.geometricweather.BuildConfig;
import wangdaye.com.geometricweather.weather2.apis.AccuWeatherApi;
import wangdaye.com.geometricweather.weather2.apis.AtmoAuraIqaApi;
import wangdaye.com.geometricweather.weather2.apis.CNWeatherApi;
import wangdaye.com.geometricweather.weather2.apis.CaiYunApi;
import wangdaye.com.geometricweather.weather2.apis.MfWeatherApi;
import wangdaye.com.geometricweather.weather2.apis.OwmApi;

@InstallIn(ApplicationComponent.class)
@Module
public class ApiModule {

    @Provides
    public AccuWeatherApi provideAccuWeatherApi(OkHttpClient client,
                                                GsonConverterFactory converterFactory,
                                                RxJava2CallAdapterFactory callAdapterFactory) {
        return new Retrofit.Builder()
                .baseUrl(BuildConfig.ACCU_WEATHER_BASE_URL)
                .client(client)
                .addConverterFactory(converterFactory)
                .addCallAdapterFactory(callAdapterFactory)
                .build()
                .create((AccuWeatherApi.class));
    }

    @Provides
    public OwmApi provideOpenWeatherMapApi(OkHttpClient client,
                                           GsonConverterFactory converterFactory,
                                           RxJava2CallAdapterFactory callAdapterFactory) {
        return new Retrofit.Builder()
                .baseUrl(BuildConfig.OWM_BASE_URL)
                .client(client)
                .addConverterFactory(converterFactory)
                .addCallAdapterFactory(callAdapterFactory)
                .build()
                .create((OwmApi.class));
    }

    @Provides
    public CaiYunApi provideCaiYunApi(OkHttpClient client,
                                      GsonConverterFactory converterFactory,
                                      RxJava2CallAdapterFactory callAdapterFactory) {
        return new Retrofit.Builder()
                .baseUrl(BuildConfig.CAIYUN_WEATHER_BASE_URL)
                .client(client)
                .addConverterFactory(converterFactory)
                .addCallAdapterFactory(callAdapterFactory)
                .build()
                .create((CaiYunApi.class));
    }

    @Provides
    public CNWeatherApi provideCNWeatherApi(OkHttpClient client,
                                            GsonConverterFactory converterFactory,
                                            RxJava2CallAdapterFactory callAdapterFactory) {
        return new Retrofit.Builder()
                .baseUrl(BuildConfig.CN_WEATHER_BASE_URL)
                .client(client)
                .addConverterFactory(converterFactory)
                .addCallAdapterFactory(callAdapterFactory)
                .build()
                .create((CNWeatherApi.class));
    }

    @Provides
    public MfWeatherApi provideMfWeatherApi(OkHttpClient client,
                                            GsonConverterFactory converterFactory,
                                            RxJava2CallAdapterFactory callAdapterFactory) {
        return new Retrofit.Builder()
                .baseUrl(BuildConfig.MF_WSFT_BASE_URL)
                .client(client)
                .addConverterFactory(converterFactory)
                .addCallAdapterFactory(callAdapterFactory)
                .build()
                .create((MfWeatherApi.class));
    }

    @Provides
    public AtmoAuraIqaApi provideAtmoAuraIqaApi(OkHttpClient client,
                                                GsonConverterFactory converterFactory,
                                                RxJava2CallAdapterFactory callAdapterFactory) {
        return new Retrofit.Builder()
                .baseUrl(BuildConfig.IQA_ATMO_AURA_URL)
                .client(client)
                .addConverterFactory(converterFactory)
                .addCallAdapterFactory(callAdapterFactory)
                .build()
                .create((AtmoAuraIqaApi.class));
    }
}
