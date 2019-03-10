package com.ne1c.telegramcharts.renderer

import android.animation.Animator
import android.animation.ValueAnimator
import android.graphics.*
import android.graphics.Paint.ANTI_ALIAS_FLAG
import android.util.TypedValue
import android.view.MotionEvent
import com.ne1c.telegramcharts.R
import com.ne1c.telegramcharts.dpToPx
import com.ne1c.telegramcharts.getClosestValue
import com.ne1c.telegramcharts.model.DataPoint
import com.ne1c.telegramcharts.model.UiDataPoint

class SelectedPointsRenderer(rangeValuesObservable: RangeValuesObservable, rootRenderer: RootRenderer) : Renderer(rootRenderer),
    Touchable {
    private val paint = Paint(ANTI_ALIAS_FLAG)
    private val textPaint = Paint(ANTI_ALIAS_FLAG)

    private var isLineShowed = false
    private var pickedPoint: Pair<PointF, MutableList<UiDataPoint>>? = null
    private var rangeValues = hashMapOf<String, ArrayList<DataPoint>>()

    private val colorInsideCircle: Int
    private val lineColor: Int
    private val floatWindowBg: Int
    private val floatWindowBorder: Int
    private val textColor: Int

    private val textHeight: Int
    private val leftTextMargin = 8f.dpToPx()
    private val topTextMargin = 8f.dpToPx()
    private val lineWidth: Float = 1f.dpToPx()
    private val circleRadius: Float = 5f.dpToPx()

    private var alpha = 255

    private val floatWindowTextSpace = 16f.dpToPx()
    private val floatWindowHeight = 68f.dpToPx()
    private val floatWindowRadius = 4f.dpToPx()
    private val floatWindowPadding = 8f.dpToPx()

    private val alphaAnimator = ValueAnimator.ofInt(255)

     init {
        rangeValuesObservable.addObserverRangeValues(object : RangeValuesObserver {
            override fun update(values: HashMap<String, ArrayList<DataPoint>>) {
                rangeValues = values
                calculate()
            }
        })

        val typedArray = rootRenderer.context().obtainStyledAttributes(
            intArrayOf(
                R.attr.themeGrid, R.attr.themeColorCardBackground, R.attr.themeFloatWindowBackground,
                R.attr.themeFloatWindowBorder, R.attr.themeLineName
            )
        )

        lineColor = rootRenderer.context().resources.getColor(typedArray.getResourceId(0, R.color.colorGridNight))

        colorInsideCircle =
            rootRenderer.context().resources.getColor(typedArray.getResourceId(1, R.color.colorCardBackgroundNight))
        floatWindowBg = rootRenderer.context().resources.getColor(
            typedArray.getResourceId(
                2,
                R.color.colorFloatWindowBackgroundNight
            )
        )
        floatWindowBorder =
            rootRenderer.context().resources.getColor(typedArray.getResourceId(3, R.color.colorPrimaryNightDark))
        textColor = rootRenderer.context().resources.getColor(typedArray.getResourceId(4, R.color.colorLineNameNight))

        textPaint.textSize =
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 14f, rootRenderer.context().resources.displayMetrics)

        val rect = Rect()
        paint.getTextBounds("22 Dec", 0, 5, rect)

        textHeight = rect.height() + 8.dpToPx()

        typedArray.recycle()

        alphaAnimator.duration = ANIMATION_DURATION
        alphaAnimator.addUpdateListener {
            alpha = if (isLineShowed) {
                it.animatedValue as Int
            } else {
                255 - it.animatedValue as Int
            }

            if (!isLineShowed && it.animatedFraction == 1f) {
                pickedPoint = null
            }

            rootRenderer.redraw()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            isLineShowed = !isLineShowed

            if (isLineShowed) {
                val list = mutableListOf<UiDataPoint>()
                val point = PointF(event.x, event.y)

                rangeValues.forEach {
                    list.add(UiDataPoint.map(rootRenderer.context(), textPaint, getClosestValue(event.x, it.value)))
                }

                pickedPoint = Pair(point, list)
            }

            alphaAnimator.start()

            return true
        } else if (event.action == MotionEvent.ACTION_UP) {
            rootRenderer.parent().requestDisallowInterceptTouchEvent(false)

            rootRenderer.redraw()
        } else if (event.action == MotionEvent.ACTION_MOVE) {
            rootRenderer.parent().requestDisallowInterceptTouchEvent(true)

            if (isLineShowed) {
                val list = mutableListOf<UiDataPoint>()
                val point = PointF(event.x, event.y)

                rangeValues.forEach {
                    list.add(UiDataPoint.map(rootRenderer.context(), textPaint, getClosestValue(event.x, it.value)))
                }

                pickedPoint = Pair(point, list)

                rootRenderer.redraw()
            }
        }

        return false
    }

    override fun getAnimator(): Animator = ValueAnimator()

    override fun calculate() {
        if (isLineShowed && pickedPoint != null) {
            pickedPoint!!.second.clear()

            rangeValues.forEach {
                pickedPoint!!.second.add(UiDataPoint.map(rootRenderer.context(), textPaint, getClosestValue(pickedPoint!!.first.x, it.value)))
            }
        }
    }

    override fun render(canvas: Canvas) {
        pickedPoint?.let {
            val x = it.second.first().dataPoint.xFitted

            paint.style = Paint.Style.FILL_AND_STROKE
            paint.strokeWidth = 1.5f.dpToPx()
            paint.color = lineColor

            paint.alpha = alpha

            canvas.drawLine(
                x, rootRenderer.spacingTop().toFloat(),
                x + lineWidth, parentHeight.toFloat(), paint
            )

            var floatWindowWidth = 0f

            it.second.forEach { dataPoint ->
                paint.style = Paint.Style.FILL
                paint.color = colorInsideCircle
                paint.alpha = alpha

                canvas.drawCircle(x + lineWidth, dataPoint.dataPoint.yFitted, circleRadius, paint)

                paint.style = Paint.Style.STROKE
                paint.color = dataPoint.dataPoint.chartColor
                paint.alpha = alpha

                canvas.drawCircle(x + lineWidth, dataPoint.dataPoint.yFitted, circleRadius, paint)

                floatWindowWidth += dataPoint.dataTextWidth
            }

            paint.color = floatWindowBg
            paint.alpha = alpha

            var left: Float
            val top: Float

            floatWindowWidth += floatWindowPadding * 2 + floatWindowTextSpace * it.second.size

            if (x + floatWindowWidth > parentWidth) {
                paint.style = Paint.Style.FILL

                left = x - floatWindowWidth + floatWindowPadding
                top = rootRenderer.spacingTop().toFloat()

                if (left < 0) left = 0f

                canvas.drawRoundRect(
                    left, top, left + floatWindowWidth,
                    rootRenderer.spacingTop() + floatWindowHeight, floatWindowRadius, floatWindowRadius, paint
                )

                paint.style = Paint.Style.STROKE
                paint.color = floatWindowBorder
                paint.strokeWidth = 1f.dpToPx()
                paint.alpha = alpha

                canvas.drawRoundRect(
                    left, top, left + floatWindowWidth,
                    rootRenderer.spacingTop() + floatWindowHeight, floatWindowRadius, floatWindowRadius, paint
                )
            } else {
                paint.style = Paint.Style.FILL

                left = x - floatWindowPadding
                top = rootRenderer.spacingTop().toFloat()

                if (left < 0) left = 0f

                canvas.drawRoundRect(
                    left, top, x + floatWindowWidth - floatWindowPadding * 2,
                    top + floatWindowHeight, floatWindowRadius, floatWindowRadius, paint
                )

                paint.style = Paint.Style.STROKE
                paint.color = floatWindowBorder
                paint.strokeWidth = 1f.dpToPx()
                paint.alpha = alpha

                canvas.drawRoundRect(
                    left, top, x + floatWindowWidth - floatWindowPadding * 2,
                    top + floatWindowHeight,
                    floatWindowRadius,
                    floatWindowRadius,
                    paint
                )
            }

            textPaint.color = textColor
            textPaint.alpha = alpha

            canvas.drawText(pickedPoint!!.second.first().date, left + leftTextMargin, top + textHeight + topTextMargin, textPaint)

            var offset = 0f

            pickedPoint?.second?.forEachIndexed { index, dataPoint ->
                textPaint.color = dataPoint.dataPoint.chartColor
                textPaint.alpha = alpha

                canvas.drawText(
                    dataPoint.dataPoint.chartName,
                    left + leftTextMargin + floatWindowTextSpace * index + offset,
                    top + textHeight * 2.5f + topTextMargin * 1.5f,
                    textPaint
                )

                canvas.drawText(
                    dataPoint.dataText,
                    left + leftTextMargin + floatWindowTextSpace * index + offset,
                    top + textHeight * 4 + topTextMargin * 1.5f,
                    textPaint
                )

                offset += dataPoint.dataTextWidth
            }
        }
    }
}