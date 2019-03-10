package com.ne1c.telegramcharts.renderer

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Paint.ANTI_ALIAS_FLAG
import android.graphics.PointF
import android.view.MotionEvent
import com.ne1c.telegramcharts.*
import com.ne1c.telegramcharts.model.LineChartData
import com.ne1c.telegramcharts.model.DataPoint
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class LineChartRenderer(rootRenderer: RootRenderer, private val selfInvalidate: Boolean = false) :
    Renderer(rootRenderer), RangeValuesObservable {

    private var xMax = 0L
    private var xMin = 0L

    private var yMax = -1f
    private var yMin = -1f

    private var targetMaxY = -1f
    private var targetMinY = -1f
    private var previousMinY = 0f
    private var previousMaxY = 0f

    private val paint = Paint(ANTI_ALIAS_FLAG)

    private var coordinates = HashMap<String, FloatArray>()
    private var rangeValues = HashMap<String, ArrayList<DataPoint>>()

    private var hidingChartName = ""
    private var showingChartName = ""
    private val animatorSet = AnimatorSet()

    private val observers = ArrayList<RangeValuesObserver>()

    init {
        paint.strokeWidth = 1.5f.dpToPx()
        paint.style = Paint.Style.STROKE

        val animator = ValueAnimator.ofFloat(0f, 1f)
        animator.addUpdateListener {
            if (!needAnimate) return@addUpdateListener

            val value = it.animatedValue as Float

            if (yMax != targetMaxY) {
                yMax = previousMaxY + (targetMaxY - previousMaxY) * value
            }

            if (yMin != targetMinY) {
                yMin = previousMinY + (targetMinY - previousMinY) * value
            }

            transformCoordinates()
        }

        val alphaAnimator = ValueAnimator.ofInt(0, 255)
        alphaAnimator.addUpdateListener {
            if (hidingChartName.isEmpty() && showingChartName.isEmpty()) return@addUpdateListener

            val value = it.animatedValue as Int

            if (hidingChartName.isNotEmpty()) {
                dataArray.find { it.name == hidingChartName }?.alpha = 255 - value
            } else if (showingChartName.isNotEmpty()) {
                dataArray.find { it.name == showingChartName }?.alpha = value
            }

            if (selfInvalidate) rootRenderer.redraw()
        }

        alphaAnimator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator?) {
                hidingChartName = ""
                showingChartName = ""
            }
        })

        animatorSet.playTogether(animator, alphaAnimator)
    }

    override fun setData(list: ArrayList<LineChartData>) {
        super.setData(list)

        rangeValues.clear()

        for (chart in list) {
            rangeValues[chart.name] = arrayListOf()
        }
    }


    fun configPaint(strokeWidth: Float) {
        paint.strokeWidth = strokeWidth
    }

    override fun rangeChanged(start: Float, end: Float) {
        if (endRange - startRange != end.toInt() - start.toInt()) coordinates.clear()

        super.rangeChanged(start, end)

        calculate()
    }

    override fun getAnimator(): Animator = animatorSet

    override fun calculate() {
        if (startRange == 0 && endRange == 0) return

        previousMinY = yMin
        previousMaxY = yMax

        xMin = -1
        xMax = -1
        targetMaxY = -1f
        targetMinY = -1f

        for (i in 0 until dataArray.size) {
            val chart = dataArray[i]
            if (!chart.isActive) continue

            val x = chart.x.copyOfRange(startRange, endRange)
            val y = chart.y.copyOfRange(startRange, endRange)

            val start = fixBoundaryValues(startRangeF - startRange, 0, x, y)
            val end = fixBoundaryValues(endRangeF - endRange, x.lastIndex - 1, x, y)

            x[0] = start.first
            x[x.lastIndex] = end.first

            y[0] = start.second
            y[y.lastIndex] = end.second

            val x_min = getMin(x)
            val x_max = getMax(x)

            val y_min = getMin(y).toFloat()
            val y_max = getMax(y).toFloat()

            if (x_min < xMin || xMin == -1L) xMin = x_min
            if (y_min < targetMinY || targetMinY == -1f) targetMinY = y_min

            if (x_max > xMax || xMax == -1L) xMax = x_max
            if (y_max > targetMaxY || targetMaxY == -1f) targetMaxY = y_max

            rangeValues[chart.name]?.clear()

            for (j in 0 until x.size) {
                rangeValues[chart.name]?.add(DataPoint(x[j], y[j], 0f, 0f, chart.color, chart.name))
            }
        }

        if (yMin == -1f && yMax == -1f) {
            yMax = targetMaxY
            yMin = targetMinY
        }

        transformCoordinates()

        needAnimate = yMax != targetMaxY
    }

    private fun transformCoordinates() {
        val diffY = yMax - yMin
        val diffX = (xMax - xMin).toFloat()

        for (i in 0 until dataArray.size) {
            rangeValues[dataArray[i].name]?.let {
                val name = dataArray[i].name
                val values = if (!coordinates.containsKey(name)) {
                    coordinates[name] = FloatArray(it.size * 4)
                    coordinates[name]
                } else {
                    coordinates[name]
                }!!

                var k = 0
                for (j in 0 until it.size) {
                    val currentPoint = it[j]
                    val nextPoint = it[if (j == it.lastIndex) j else j + 1]

                    val x1 = scaleX(currentPoint.x, xMin, diffX)
                    val x2 = scaleX(nextPoint.x, xMin, diffX)

                    val y1 = parentHeight - scaleY(currentPoint.y, yMin, diffY)
                    val y2 = parentHeight - scaleY(nextPoint.y, yMin, diffY)

                    values[k++] = x1
                    values[k++] = y1
                    values[k++] = x2
                    values[k++] = y2

                    currentPoint.xFitted = x1
                    currentPoint.yFitted = y1
                    nextPoint.xFitted = x2
                    nextPoint.yFitted = y2
                }
            }
        }

        observers.forEach {
            it.update(rangeValues)
        }
    }

    override fun render(canvas: Canvas) {
        if (coordinates.size == 0) return

        for (i in 0 until dataArray.size) {
            paint.color = dataArray[i].color

            if (hidingChartName == dataArray[i].name || showingChartName == dataArray[i].name) {
                paint.alpha = dataArray[i].alpha
            } else if (dataArray[i].isActive) {
                paint.alpha = 255
            } else if (!dataArray[i].isActive) {
                continue
            }

            canvas.drawLines(coordinates[dataArray[i].name], paint)
        }
    }

    override fun showChart(chart: LineChartData, show: Boolean) {
        super.showChart(chart, show)

        if (show) {
            showingChartName = chart.name
        } else {
            hidingChartName = chart.name
        }
    }

    override fun addObserverRangeValues(observer: RangeValuesObserver) {
        observers.add(observer)
    }

    override fun removeObserveRangeValues(observer: RangeValuesObserver) {
        observers.remove(observer)
    }

    private fun fixBoundaryValues(
        diff: Float,
        index: Int,
        x: LongArray,
        y: LongArray
    ): Pair<Long, Long> {
        val x1 = x[index]
        val y1 = y[index]
        val x2 = x[index + 1]
        val y2 = y[index + 1]

        val xCalculated = (x1 + (x2 - x1).toFloat() * diff).toLong()
        val yXCalculated = calcYFromOnLine(x1, y1, x2, y2, xCalculated)
        return xCalculated to yXCalculated
    }

    private fun calcYFromOnLine(x1: Long, y1: Long, x2: Long, y2: Long, x: Long): Long {
        if (x2 == x1) return (y2 - y1) / 2
        return (y1 + ((y2 - y1)) * (x - x1) / (x2 - x1).toFloat()).toLong()
    }

    private fun scaleX(now: Long, xMin: Long, diffX: Float): Float {
        val ratX = (now - xMin) / diffX
        return parentWidth * ratX
    }

    private fun scaleY(now: Long, yMin: Float, diffY: Float): Float {
        val ratY = (now - yMin) / diffY
        return (parentHeight - rootRenderer.spacingBottom() - rootRenderer.spacingTop()) * ratY
    }
}