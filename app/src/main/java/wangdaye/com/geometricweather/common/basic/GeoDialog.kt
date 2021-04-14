package wangdaye.com.geometricweather.common.basic

import android.content.Context
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import wangdaye.com.geometricweather.R
import wangdaye.com.geometricweather.common.snackbar.SnackbarContainer

abstract class GeoDialog : DialogFragment() {

    companion object {

        @JvmStatic
        fun injectStyle(f: DialogFragment) {
            f.lifecycle.addObserver(LifecycleEventObserver { _: LifecycleOwner, event: Lifecycle.Event ->
                when (event) {
                    Lifecycle.Event.ON_CREATE -> f.setStyle(
                            STYLE_NO_TITLE, android.R.style.Theme_DeviceDefault_Dialog_MinWidth)

                    Lifecycle.Event.ON_START -> f.requireDialog().window!!.setBackgroundDrawableResource(
                            R.drawable.dialog_background)

                    else -> {}
                }
            })
        }
    }

    var foreground = false
        private set

    override fun onAttach(context: Context) {
        super.onAttach(context)
        injectStyle(this)
    }

    override fun onResume() {
        super.onResume()
        foreground = true
        (requireActivity() as GeoActivity).setTopDialog(this)
    }

    override fun onPause() {
        super.onPause()
        foreground = false
        (requireActivity() as GeoActivity).checkToCleanTopDialog(this)
    }

    open fun getSnackbarContainer() = SnackbarContainer(this, view as ViewGroup, false)
}