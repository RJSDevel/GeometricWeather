package wangdaye.com.geometricweather.common.basic.insets

import android.graphics.Rect
import android.os.Build
import android.view.View
import android.view.WindowInsets
import androidx.annotation.RequiresApi
import wangdaye.com.geometricweather.common.basic.insets.FitBothSideBarView.FitSide

class FitBothSideBarHelper @JvmOverloads constructor(
        private val target: View,
        private var fitSide: Int = FitBothSideBarView.SIDE_TOP or FitBothSideBarView.SIDE_BOTTOM,
        private var fitTopSideEnabled: Boolean = true,
        private var fitBottomSideEnabled: Boolean = true
) {

    companion object {

        private val rootInsetsCache = ThreadLocal<Rect>()

        @JvmStatic
        fun setRootInsetsCache(rootInsets: Rect?) {
            rootInsetsCache.set(rootInsets)
        }
    }

    var windowInsets = Rect(0, 0, 0, 0)
        private set
        get() {
            rootInsetsCache.get()?.let {
                return it
            }
            return field
        }

    interface InsetsConsumer {
        fun consume()
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT_WATCH)
    @JvmOverloads
    fun onApplyWindowInsets(insets: WindowInsets, consumer: InsetsConsumer = object : InsetsConsumer {
        override fun consume() {
            target.requestLayout()
        }
    }): WindowInsets {
        windowInsets = Rect(
                insets.systemWindowInsetLeft, insets.systemWindowInsetTop,
                insets.systemWindowInsetRight, insets.systemWindowInsetBottom)
        consumer.consume()
        return insets
    }

    @JvmOverloads
    fun fitSystemWindows(r: Rect, consumer: InsetsConsumer = object : InsetsConsumer {
        override fun consume() {
            target.requestLayout()
        }
    }): Boolean {
        windowInsets = r
        consumer.consume()
        return false
    }

    fun left() = windowInsets.left

    fun top() = if (fitSide and FitBothSideBarView.SIDE_TOP != 0 && fitTopSideEnabled) {
        windowInsets.top
    } else {
        0
    }

    fun right() = windowInsets.right

    fun bottom() = if (fitSide and FitBothSideBarView.SIDE_BOTTOM != 0 && fitBottomSideEnabled) {
        windowInsets.bottom
    } else {
        0
    }

    fun addFitSide(@FitSide side: Int) {
        if (fitSide and side != 0) {
            fitSide = fitSide or side
            target.requestLayout()
        }
    }

    fun removeFitSide(@FitSide side: Int) {
        if (fitSide and side != 0) {
            fitSide = fitSide xor side
            target.requestLayout()
        }
    }

    fun setFitSystemBarEnabled(top: Boolean, bottom: Boolean) {
        if (fitTopSideEnabled != top || fitBottomSideEnabled != bottom) {
            fitTopSideEnabled = top
            fitBottomSideEnabled = bottom
            target.requestLayout()
        }
    }
}