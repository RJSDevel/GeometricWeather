package wangdaye.com.geometricweather.common.basic

import android.view.ViewGroup
import androidx.fragment.app.Fragment
import wangdaye.com.geometricweather.common.snackbar.SnackbarContainer

open class GeoFragment : Fragment() {

    open fun getSnackbarContainer() = SnackbarContainer(this, view as ViewGroup, true)
}