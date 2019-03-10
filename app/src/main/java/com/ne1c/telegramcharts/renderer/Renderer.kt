package com.ne1c.telegramcharts.renderer

import android.animation.Animator
import android.graphics.Canvas
import com.ne1c.telegramcharts.model.LineChartData

abstract class Renderer(protected val rootRenderer: RootRenderer) {
    companion object {
       const val ANIMATION_DURATION = 300L
    }

    protected var dataArray = ArrayList<LineChartData>()

    protected var parentWidth = 0
    protected var parentHeight = 0

    protected var startRangeF = 0f
    protected var endRangeF = 0f

    protected var startRange = 0
    protected var endRange = 0

    var needAnimate = false

    open fun setData(list: ArrayList<LineChartData>) {
        dataArray.clear()
        dataArray.addAll(list)
    }

    open fun setParentSize(width: Int, height: Int) {
        this.parentWidth = width
        this.parentHeight = height
    }

    open fun rangeChanged(start: Float, end: Float) {
        startRangeF = start
        endRangeF = end

        startRange = start.toInt()
        endRange = end.toInt()
    }

    open fun showChart(chart: LineChartData, show: Boolean) {
        dataArray.find { it.name == chart.name }?.isActive = show

        calculate()

        needAnimate = true
    }

    open fun width() = parentWidth

    open fun height() = parentHeight

    abstract fun getAnimator(): Animator

    abstract fun calculate()

    abstract fun render(canvas: Canvas)
}