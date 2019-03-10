package com.ne1c.telegramcharts.view

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewParent
import android.view.animation.AccelerateInterpolator
import com.ne1c.telegramcharts.dpToPx
import com.ne1c.telegramcharts.model.LineChartData
import com.ne1c.telegramcharts.renderer.*
import java.util.*

class ChartView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr), RootRenderer {
    var charts = ArrayList<LineChartData>()
        set(value) {
            field = value

            var maxY = 0L

            value.asSequence()
                .filter { it.isActive }
                .forEach {
                    val max = it.y.max() ?: 0

                    if (max > maxY) maxY = max
                }

            spaceBetweenLines = maxY / countLines.toFloat()

            xAxisRenderer.setData(value)
            yAxisRenderer.setData(value)
            chartRenderer.setData(value)
        }

    private val paint = Paint()

    private val countLines = 6
    private var spaceBetweenLines = 0f

    private val yAxisRenderer: Renderer
    private val xAxisRenderer: Renderer
    private val chartRenderer: Renderer
    private val selectedDataRenderer: Renderer

    private val desireHeight = 320.dpToPx()

    private var animation: AnimatorSet
    private val invalidateAnimator: ValueAnimator
    private val animationListener: Animator.AnimatorListener

    init {
        paint.isAntiAlias = true
        paint.strokeWidth = 1f.dpToPx()

        val padding = 16.dpToPx()
        setPadding(padding, padding, padding, padding)

        chartRenderer = LineChartRenderer(this)

        xAxisRenderer = XAxisRenderer(chartRenderer, this)

        yAxisRenderer = YAxisRenderer(this)
        yAxisRenderer.setCountLines(countLines)

        selectedDataRenderer = SelectedPointsRenderer(chartRenderer, this)

        animationListener = object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator?, isReverse: Boolean) {
                xAxisRenderer.needAnimate = false
                yAxisRenderer.needAnimate = false
                chartRenderer.needAnimate = false
            }

            override fun onAnimationCancel(animation: Animator?) {
                xAxisRenderer.needAnimate = false
                yAxisRenderer.needAnimate = false
                chartRenderer.needAnimate = false
            }
        }

        invalidateAnimator = ValueAnimator.ofFloat(0f, 1f)
        invalidateAnimator.addUpdateListener {
            redraw()
        }

        animation = AnimatorSet()
        animation.duration = Renderer.ANIMATION_DURATION
        animation.interpolator = AccelerateInterpolator()
        animation.addListener(animationListener)
        animation.playTogether(xAxisRenderer.getAnimator(), yAxisRenderer.getAnimator(),
            chartRenderer.getAnimator(), invalidateAnimator)
    }

    fun showChart(chart: LineChartData, show: Boolean) {
        chartRenderer.showChart(chart, show)
        xAxisRenderer.showChart(chart, show)
        yAxisRenderer.showChart(chart, show)

        animation.start()
    }

    fun attach(sliderView: MinimapChartView) {
        sliderView.selectionListener = object : MinimapChartView.SelectionListener {
            override fun changedSelection(startIndex: Float, endIndex: Float) {
                chartRenderer.rangeChanged(startIndex, endIndex)
                xAxisRenderer.rangeChanged(startIndex, endIndex)
                yAxisRenderer.rangeChanged(startIndex, endIndex)

                if (yAxisRenderer.needAnimate) {
                    animation.start()
                } else {
                    redraw()
                }
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val h = MeasureSpec.getSize(heightMeasureSpec)
        val hMode = MeasureSpec.getMode(heightMeasureSpec)

        if (h < desireHeight || hMode == MeasureSpec.AT_MOST) {
            setMeasuredDimension(
                widthMeasureSpec,
                MeasureSpec.makeMeasureSpec(desireHeight, MeasureSpec.EXACTLY)
            )
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        }

        xAxisRenderer.setParentSize(measuredWidth, measuredHeight)
        yAxisRenderer.setParentSize(measuredWidth, measuredHeight - xAxisRenderer.height())
        chartRenderer.setParentSize(measuredWidth, measuredHeight - xAxisRenderer.height())
        selectedDataRenderer.setParentSize(measuredWidth, measuredHeight - xAxisRenderer.height())
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        yAxisRenderer.render(canvas)
        chartRenderer.render(canvas)
        xAxisRenderer.render(canvas)
        selectedDataRenderer.render(canvas)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (selectedDataRenderer is Touchable) {
            return selectedDataRenderer.onTouchEvent(event)
        }

        return false
    }

    override fun redraw() {
        invalidate()
    }

    override fun parent(): ViewParent = parent

    override fun context(): Context = context

    override fun spacingLeft(): Int = paddingLeft

    override fun spacingRight(): Int = paddingRight

    override fun spacingBottom(): Int = paddingBottom

    override fun spacingTop(): Int = paddingTop

}