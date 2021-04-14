package wangdaye.com.geometricweather.common.basic

import android.app.Application
import androidx.lifecycle.AndroidViewModel

open class GeoViewModel(application: Application) : AndroidViewModel(application) {

    private var mNewInstance = true

    fun checkIsNewInstance(): Boolean {
        val result = mNewInstance
        mNewInstance = false
        return result
    }
}