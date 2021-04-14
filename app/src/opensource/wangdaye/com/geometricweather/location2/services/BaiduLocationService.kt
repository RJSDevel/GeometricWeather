package wangdaye.com.geometricweather.location2.services

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Baidu location service.
 */
class BaiduLocationService @Inject constructor(
        @ApplicationContext context: Context) : AndroidLocationService(context)