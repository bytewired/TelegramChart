package com.ne1c.telegramcharts.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Region
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewParent
import android.view.animation.AccelerateInterpolator
import com.ne1c.telegramcharts.R
import com.ne1c.telegramcharts.dpToPx
import com.ne1c.telegramcharts.model.LineChartData
import com.ne1c.telegramcharts.renderer.Renderer
import com.ne1c.telegramcharts.renderer.LineChartRenderer
import com.ne1c.telegramcharts.renderer.RootRenderer

class MinimapChartView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr), RootRenderer {
    companion object {
        private const val NONE = 0
        private const val MOVE = 1
        private const val DRAGGING_RIGHT_SIDE = 2
        private const val DRAGGING_LEFT_SIDE = 3
    }

    private var action = NONE

    var charts = ArrayList<LineChartData>()
        set(value) {
            field = value

            pointsCount = if (value.size > 0) {
                value[0].x.size - 1
            } else {
                0
            }

            chartRenderer.setData(charts)
        }

    private var sliderMinWidth = 0
    private var sliderMaxWidth = 0
    private var sliderWidth = 0

    private var chartPaint = Paint()
    private var sliderPaint = Paint()
    private var fgPaint = Paint()

    private val borderSideWidth = 4f.dpToPx()
    private val borderTopWidth = 1f.dpToPx()

    private val desireHeight = 48.dpToPx()

    private var startXPosition = 0f
    private val endXPosition: Float
        get() {
            return startXPosition + sliderWidth
        }

    private var pointsCount = 0

    private var isInit = true
    private var chartRenderer: LineChartRenderer

    private val margin = 2.dpToPx()

    var selectionListener: SelectionListener? = null

    init {
        val typedArray = context.obtainStyledAttributes(intArrayOf(R.attr.themeRectangle, R.attr.themeWindow))

        chartPaint.isAntiAlias = true
        chartPaint.strokeCap = Paint.Cap.ROUND

        fgPaint.color = context.resources.getColor(typedArray.getResourceId(0, R.color.colorRectangleNight))

        sliderPaint.isAntiAlias = true
        sliderPaint.color = context.resources.getColor(typedArray.getResourceId(1, R.color.colorWindowNight))
        sliderPaint.strokeWidth = borderTopWidth

        typedArray.recycle()

        chartRenderer = LineChartRenderer(this, true)
        chartRenderer.configPaint(1f.dpToPx())
        chartRenderer.getAnimator().duration = Renderer.ANIMATION_DURATION
        chartRenderer.getAnimator().interpolator = AccelerateInterpolator()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        sliderMinWidth = MeasureSpec.getSize(widthMeasureSpec) / 5
        sliderMaxWidth = measuredWidth

        if (sliderWidth == 0) sliderWidth = sliderMaxWidth

        setMeasuredDimension(widthMeasureSpec, MeasureSpec.makeMeasureSpec(desireHeight, MeasureSpec.EXACTLY))

        chartRenderer.setParentSize(measuredWidth, measuredHeight)
        chartRenderer.rangeChanged(0f, charts[0].x.size - 1f)
    }

    private var touchDeltaX = 0f
    private var previousX = 0f

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val inBoundaries =
            (event.x in (startXPosition - borderSideWidth)..(endXPosition + borderSideWidth)) || action != NONE

        if (inBoundaries && event.x > 0f && event.x <= width) {
            when {
                event.action == MotionEvent.ACTION_DOWN -> {
                    parent.requestDisallowInterceptTouchEvent(true)

                    touchDeltaX = event.x - startXPosition
                    previousX = event.x

                    if (event.x - startXPosition - borderSideWidth <= borderSideWidth * 2) {
                        action = DRAGGING_LEFT_SIDE
                    } else if (endXPosition - event.x - borderSideWidth <= borderSideWidth * 2) {
                        action = DRAGGING_RIGHT_SIDE
                    } else if (event.x >= startXPosition + borderSideWidth && event.x <= endXPosition - borderSideWidth) {
                        action = MOVE
                    }
                }
                event.action == MotionEvent.ACTION_MOVE -> {
                    val diffX = event.x - touchDeltaX

                    if (action == DRAGGING_LEFT_SIDE || action == DRAGGING_RIGHT_SIDE) {
                        if (action == DRAGGING_LEFT_SIDE) {
                            val offset = (previousX - event.x).toInt()

                            if ((sliderWidth + offset) in sliderMinWidth..sliderMaxWidth) {
                                startXPosition -= offset
                                sliderWidth += offset
                            }

                            if (startXPosition < 0f) startXPosition = 0f
                        } else if (action == DRAGGING_RIGHT_SIDE) {
                            sliderWidth += (event.x - previousX).toInt()
                        }

                        if (sliderWidth > sliderMaxWidth) {
                            sliderWidth = sliderMaxWidth
                        } else if (sliderWidth < sliderMinWidth) {
                            sliderWidth = sliderMinWidth
                        }
                    } else if (action == MOVE) {
                        startXPosition = if (diffX < 0) 0f else diffX

                        if (endXPosition > width) {
                            startXPosition = width - sliderWidth.toFloat()
                        }
                    }

                    previousX = event.x

                    postRangeToListener()

                    redraw()
                }
                event.action == MotionEvent.ACTION_UP -> {
                    action = NONE
                    parent.requestDisallowInterceptTouchEvent(false)
                }
            }
        }

        return inBoundaries
    }

    private fun postRangeToListener() {
        val startPosition = (startXPosition / width) * (pointsCount)
        val endPosition = (endXPosition / width) * (pointsCount)

        selectionListener?.changedSelection(startPosition, endPosition)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (isInit) {
            isInit = false
            postRangeToListener()
        }

        chartRenderer.render(canvas)

        canvas.clipRect(
            startXPosition + borderSideWidth, borderTopWidth, startXPosition + sliderWidth - borderSideWidth,
            height - borderTopWidth, Region.Op.DIFFERENCE
        )
        canvas.drawRect(startXPosition, 0f, startXPosition + sliderWidth, height.toFloat(), sliderPaint)

        canvas.drawRect(0f, 0f, startXPosition, height.toFloat(), fgPaint)
        canvas.drawRect(
            startXPosition + sliderWidth,
            0f,
            width.toFloat(),
            height.toFloat(),
            fgPaint
        )
    }

    fun showChart(chart: LineChartData, show: Boolean) {
        chartRenderer.showChart(chart, show)
        chartRenderer.getAnimator().start()
    }

    override fun parent(): ViewParent = parent

    override fun context(): Context = context

    override fun redraw() {
        invalidate()
    }

    override fun spacingLeft(): Int = margin

    override fun spacingRight(): Int = margin

    override fun spacingBottom(): Int = margin

    override fun spacingTop(): Int = margin

    interface SelectionListener {
        fun changedSelection(startIndex: Float, endIndex: Float)
    }
}