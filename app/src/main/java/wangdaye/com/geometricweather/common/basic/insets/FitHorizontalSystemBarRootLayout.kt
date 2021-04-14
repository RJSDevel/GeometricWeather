package wangdaye.com.geometricweather.common.basic.insets

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.os.Build
import android.util.AttributeSet
import android.view.WindowInsets
import android.widget.FrameLayout
import androidx.annotation.ColorInt
import androidx.annotation.RequiresApi
import androidx.core.view.ViewCompat
import wangdaye.com.geometricweather.common.utils.DisplayUtils

class FitHorizontalSystemBarRootLayout @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val paint = Paint()

    @ColorInt
    private var rootColor = Color.TRANSPARENT

    @ColorInt
    private var lineColor = Color.GRAY
    private var mFitKeyboardExpanded = false

    init {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = DisplayUtils.dpToPx(context, 2f)

        setWillNotDraw(false)
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT_WATCH)
    override fun onApplyWindowInsets(insets: WindowInsets): WindowInsets {
        val r = Rect(
                insets.systemWindowInsetLeft, insets.systemWindowInsetTop,
                insets.systemWindowInsetRight, insets.systemWindowInsetBottom)

        FitBothSideBarHelper.setRootInsetsCache(
                Rect(0, r.top, 0, if (mFitKeyboardExpanded) 0 else r.bottom))
        fitSystemBar(r)

        return insets
    }

    override fun fitSystemWindows(insets: Rect): Boolean {
        FitBothSideBarHelper.setRootInsetsCache(
                Rect(0, insets.top, 0, if (mFitKeyboardExpanded) 0 else insets.bottom))

        super.fitSystemWindows(insets)
        fitSystemBar(insets)

        return false
    }

    private fun fitSystemBar(insets: Rect) {
        setPadding(insets.left, 0, insets.right, 0)
        invalidate()
    }

    public override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(rootColor)

        paint.color = lineColor
        if (paddingLeft > 0) {
            canvas.drawLine(paddingLeft.toFloat(), 0f, paddingLeft.toFloat(), measuredHeight.toFloat(), paint)
        }
        if (paddingRight > 0) {
            canvas.drawLine((measuredWidth - paddingRight).toFloat(), 0f, (
                    measuredWidth - paddingRight).toFloat(), measuredHeight.toFloat(), paint)
        }
    }

    fun setLineColor(@ColorInt lineColor: Int) {
        this.lineColor = lineColor
        invalidate()
    }

    fun setRootColor(@ColorInt rootColor: Int) {
        this.rootColor = rootColor
        invalidate()
    }

    fun setFitKeyboardExpanded(fit: Boolean) {
        mFitKeyboardExpanded = fit
        ViewCompat.requestApplyInsets(this)
        requestLayout()
    }
}