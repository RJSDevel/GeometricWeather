package wangdaye.com.geometricweather.common.basic

import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import wangdaye.com.geometricweather.GeometricWeather
import wangdaye.com.geometricweather.R
import wangdaye.com.geometricweather.common.basic.insets.FitHorizontalSystemBarRootLayout
import wangdaye.com.geometricweather.common.snackbar.SnackbarContainer
import wangdaye.com.geometricweather.common.utils.DisplayUtils
import wangdaye.com.geometricweather.common.utils.LanguageUtils
import wangdaye.com.geometricweather.settings.SettingsManager.Companion.getInstance


abstract class GeoActivity : AppCompatActivity() {

    lateinit var fitHorizontalSystemBarRootLayout: FitHorizontalSystemBarRootLayout

    private var topDialog: GeoDialog? = null

    var foreground = false
        private set

    private class KeyboardResizeBugWorkaround private constructor(activity: GeoActivity) {

        companion object {
            fun assistActivity(activity: GeoActivity) {
                KeyboardResizeBugWorkaround(activity)
            }
        }

        private val mRoot = activity.fitHorizontalSystemBarRootLayout
        private val mRootParams = mRoot.layoutParams

        private var mUsableHeightPrevious = 0

        init {
            mRoot.viewTreeObserver.addOnGlobalLayoutListener {
                possiblyResizeChildOfContent()
            }
        }

        private fun possiblyResizeChildOfContent() {
            val usableHeightNow = computeUsableHeight()

            if (usableHeightNow != mUsableHeightPrevious) {
                val screenHeight = mRoot.rootView.height
                val keyboardExpanded: Boolean

                if (screenHeight - usableHeightNow > screenHeight / 5) {
                    // keyboard probably just became visible.
                    keyboardExpanded = true
                    mRootParams.height = usableHeightNow
                } else {
                    // keyboard probably just became hidden.
                    keyboardExpanded = false
                    mRootParams.height = screenHeight
                }
                mUsableHeightPrevious = usableHeightNow

                mRoot.setFitKeyboardExpanded(keyboardExpanded)
            }
        }

        private fun computeUsableHeight(): Int {
            val r = Rect()
            DisplayUtils.getVisibleDisplayFrame(mRoot, r)
            return r.bottom // - r.top; --> Do not reduce the height of status bar.
        }
    }

    @CallSuper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fitHorizontalSystemBarRootLayout = FitHorizontalSystemBarRootLayout(this)
        fitHorizontalSystemBarRootLayout.setRootColor(ContextCompat.getColor(this, R.color.colorRoot))
        fitHorizontalSystemBarRootLayout.setLineColor(ContextCompat.getColor(this, R.color.colorLine))

        GeometricWeather.getInstance().addActivity(this)
        LanguageUtils.setLanguage(this, getInstance(this).getLanguage().locale)

        val darkMode = DisplayUtils.isDarkMode(this)
        DisplayUtils.setSystemBarStyle(this, window,
                false, false, true, !darkMode)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        // decor -> fit horizontal system bar -> decor child.
        val decorView = window.decorView as ViewGroup
        val decorChild = decorView.getChildAt(0) as ViewGroup
        decorView.removeView(decorChild)
        decorView.addView(fitHorizontalSystemBarRootLayout)
        fitHorizontalSystemBarRootLayout.removeAllViews()
        fitHorizontalSystemBarRootLayout.addView(decorChild)

        KeyboardResizeBugWorkaround.assistActivity(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        GeometricWeather.getInstance().setTopActivity(this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        GeometricWeather.getInstance().setTopActivity(this)
    }

    @CallSuper
    override fun onResume() {
        super.onResume()
        foreground = true
        GeometricWeather.getInstance().setTopActivity(this)
    }

    @CallSuper
    override fun onPause() {
        super.onPause()
        foreground = false
        GeometricWeather.getInstance().checkToCleanTopActivity(this)
    }

    @CallSuper
    override fun onDestroy() {
        super.onDestroy()
        GeometricWeather.getInstance().removeActivity(this)
    }

    open fun getSnackbarContainer() = SnackbarContainer(
            this,
            findViewById<ViewGroup>(android.R.id.content).getChildAt(0) as ViewGroup,
            true
    )

    open fun provideSnackbarContainer(): SnackbarContainer {
        topDialog?.let {
            return it.getSnackbarContainer()
        }
        return getSnackbarContainer()
    }

    fun setTopDialog(d: GeoDialog) {
        topDialog = d
    }

    fun checkToCleanTopDialog(d: GeoDialog) {
        if (topDialog === d) {
            topDialog = null
        }
    }
}