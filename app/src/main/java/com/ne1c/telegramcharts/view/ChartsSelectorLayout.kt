package com.ne1c.telegramcharts.view

import android.content.Context
import android.content.res.ColorStateList
import android.support.annotation.ColorInt
import android.support.v4.widget.CompoundButtonCompat
import android.util.AttributeSet
import android.view.View
import android.widget.CheckBox
import android.widget.LinearLayout
import com.ne1c.telegramcharts.R
import com.ne1c.telegramcharts.dpToPx
import com.ne1c.telegramcharts.model.LineChartData
import java.util.*

class ChartsSelectorLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {
    @ColorInt
    private val textColor: Int
    @ColorInt
    private val lineColor: Int

    init {
        val typedArray = context.obtainStyledAttributes(intArrayOf(R.attr.themeLineName, R.attr.themeGrid))
        textColor = context.resources.getColor(typedArray.getResourceId(0, R.color.colorLineNameNight))
        lineColor = context.resources.getColor(typedArray.getResourceId(1, R.color.colorGridNight))

        orientation = VERTICAL
    }

    var listener: ShowChartListener? = null

    var charts = ArrayList<LineChartData>()
        set(value) {
            field = value

            removeAllViews()

            value.forEachIndexed { index, lineChartData ->
                addView(buildCheckBox(lineChartData))

                if (index != value.lastIndex) {
                    addView(View(context).apply {
                        setBackgroundColor(lineColor)
                    }, LayoutParams(LayoutParams.MATCH_PARENT, 1.dpToPx()).apply {
                        leftMargin = 32.dpToPx()
                        rightMargin = 4.dpToPx()
                        topMargin = 2.dpToPx()
                        bottomMargin = 2.dpToPx()
                    })
                }
            }

            invalidate()
        }

    private fun buildCheckBox(chart: LineChartData): CheckBox {
        val checkBox = CheckBox(context)
        checkBox.text = chart.name
        checkBox.isChecked = chart.isActive
        checkBox.layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = 4.dpToPx()
            leftMargin = 4.dpToPx()
            rightMargin = 4.dpToPx()
            topMargin = 4.dpToPx()
        }

        checkBox.setTextColor(textColor)

        val states = arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf())
        val colors = intArrayOf(chart.color, chart.color)
        CompoundButtonCompat.setButtonTintList(checkBox, ColorStateList(states, colors))

        checkBox.setOnCheckedChangeListener { _, isChecked ->
            listener?.showChart(chart, isChecked)
        }

        return checkBox
    }

    interface ShowChartListener {
        fun showChart(chart: LineChartData, show: Boolean)
    }
}