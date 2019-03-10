package com.ne1c.telegramcharts.renderer

import android.animation.Animator
import android.animation.ValueAnimator
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Paint.ANTI_ALIAS_FLAG
import android.graphics.Rect
import android.text.format.DateUtils
import android.util.TypedValue
import com.ne1c.telegramcharts.R
import com.ne1c.telegramcharts.dpToPx
import com.ne1c.telegramcharts.model.DataPoint
import kotlin.math.roundToInt

class XAxisRenderer(rangeValuesObservable: RangeValuesObservable, rootRenderer: RootRenderer) : Renderer(rootRenderer) {
    private val COUNT_DATES = 6

    private val textWidth: Int
    private var textHeight: Int

    private var spaceBetweenText = 0

    private val paint = Paint(ANTI_ALIAS_FLAG)

    private var rangeValues = hashMapOf<String, ArrayList<DataPoint>>()
    private var displayedData = Array(COUNT_DATES) { "" }

    private var height = 0
    private val top = 8.dpToPx()

    private val animator = ValueAnimator.ofInt(0, 255)

    init {
        val typedArray = rootRenderer.context().obtainStyledAttributes(intArrayOf(R.attr.themeColorValues))

        paint.color = rootRenderer.context().resources.getColor(typedArray.getResourceId(0, R.color.colorValuesNight))
        paint.textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 14f, rootRenderer.context().resources.displayMetrics)

        typedArray.recycle()

        val rect = Rect()

        paint.getTextBounds("22 Dec", 0, 5, rect)
        textWidth = rect.width()
        textHeight = rect.height() + 8.dpToPx()

        height = 24.dpToPx() + top

        rangeValuesObservable.addObserverRangeValues(object : RangeValuesObserver {
            override fun update(values: HashMap<String, ArrayList<DataPoint>>) {
                rangeValues = values
                calculate()
            }
        })

        animator.addUpdateListener {
            paint.alpha = it.animatedValue as Int
        }
    }

    override fun height(): Int = height

    override fun setParentSize(width: Int, height: Int) {
        super.setParentSize(width, height)

        spaceBetweenText = (width - textWidth * COUNT_DATES) / COUNT_DATES
    }

    override fun getAnimator(): Animator = animator

    override fun rangeChanged(start: Float, end: Float) {
        super.rangeChanged(start, end)

        calculate()
    }

    override fun calculate() {
        for (chart in dataArray) {
            if (!chart.isActive) continue

            val values = rangeValues[chart.name]
            val xOffset = (values!!.size / COUNT_DATES.toFloat()).toInt()

            for (i in 0 until displayedData.size) {
                val index = xOffset * (i)

                val x = values[if (index > values.size) values.lastIndex else index].x
                displayedData[i] = DateUtils.formatDateTime(
                    rootRenderer.context(),
                    x,
                    DateUtils.FORMAT_ABBREV_MONTH or DateUtils.FORMAT_NO_YEAR
                )
            }
        }
    }

    override fun render(canvas: Canvas) {
        displayedData.forEachIndexed { index, item ->
            canvas.drawText(
                item,
                rootRenderer.spacingLeft().toFloat() + textWidth * index + spaceBetweenText * index,
                parentHeight.toFloat() - height + top + textHeight,
                paint
            )
        }
    }
}