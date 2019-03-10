package com.ne1c.telegramcharts.renderer

import android.animation.Animator
import android.animation.FloatEvaluator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Paint.ANTI_ALIAS_FLAG
import android.util.TypedValue
import com.ne1c.telegramcharts.R
import com.ne1c.telegramcharts.dpToPx
import com.ne1c.telegramcharts.model.DataPoint
import com.ne1c.telegramcharts.numberToString

class YAxisRenderer(rootRenderer: RootRenderer) : Renderer(rootRenderer) {
    private val UP_TRANSITION = "left_transition"
    private val DOWN_TRANSITION = "right_transition"
    private val ALPHA = "alpha"

    private val DIRECTION_UP = 0
    private val DIRECTION_DOWN = 1

    private val paint = Paint(ANTI_ALIAS_FLAG)
    private val linePaint = Paint(ANTI_ALIAS_FLAG)

    private val animator: ValueAnimator
    private val bottomMargin = 8f.dpToPx()

    // animation stuff
    private var transition = 1f
    private var alpha = 255 // 0..255

    private var spaceBetweenLines = 0f
    private var countLines = 6

    private val lineHeight: Float

    private var displayedData = Array(countLines) { "" }

    private var direction = -1

    private var previousYMax = -1L
    private var yMax = -1L

    private val floatEvaluator = FloatEvaluator()

    init {
        val typedArray = rootRenderer.context().obtainStyledAttributes(intArrayOf(R.attr.themeColorValues, R.attr.themeGrid))

        paint.textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 14f, rootRenderer.context().resources.displayMetrics)
        paint.color = rootRenderer.context().resources.getColor(typedArray.getResourceId(0, R.color.colorValuesNight))

        linePaint.color = rootRenderer.context().resources.getColor(typedArray.getResourceId(1, R.color.colorGridNight))
        lineHeight = 1f.dpToPx()

        typedArray.recycle()

        val propertyAlpha = PropertyValuesHolder.ofInt(ALPHA, 0, 255)
        val propertyUpTransition = PropertyValuesHolder.ofFloat(UP_TRANSITION, 1f, 1f)
        val propertyDownTransition = PropertyValuesHolder.ofFloat(DOWN_TRANSITION, 1f, 1f)

        animator = ValueAnimator()
        animator.setValues(propertyAlpha, propertyUpTransition, propertyDownTransition)
        animator.addUpdateListener {
            if (!needAnimate) return@addUpdateListener

            transition = when (direction) {
                DIRECTION_UP -> {
                    it.getAnimatedValue(UP_TRANSITION) as Float
                }
                DIRECTION_DOWN -> {

                    it.getAnimatedValue(DOWN_TRANSITION) as Float// / spaceBetweenLines
                }
                else -> 1f
            }

            alpha = it.getAnimatedValue(ALPHA) as Int
        }
    }

    override fun setParentSize(width: Int, height: Int) {
        super.setParentSize(width, height)

        spaceBetweenLines = (height / countLines).toFloat()
    }

    fun setCountLines(count: Int) {
        if (count != countLines) {
            countLines = count
            displayedData = Array(count) { "" }

            calculate()
        }
    }

    override fun getAnimator(): Animator = animator

    override fun rangeChanged(start: Float, end: Float) {
        super.rangeChanged(start, end)

        calculate()
    }

    override fun calculate() {
        previousYMax = yMax

        var max = 0L

        for (chart in dataArray) {
            if (!chart.isActive) continue

            for (i in startRange..endRange) {
                if (max < chart.y[i]) max = chart.y[i]
            }
        }

        val step = max / countLines
        for (i in 0 until displayedData.size) {
            displayedData[i] = numberToString(step * i)
        }

        yMax = step * countLines

        if (previousYMax == -1L) previousYMax = yMax

        if (previousYMax != yMax) {
            direction = if (previousYMax < yMax) {
                DIRECTION_DOWN
            } else {
                DIRECTION_UP
            }

            animator.setValues(
                PropertyValuesHolder.ofFloat(DOWN_TRANSITION, 2f, 1f),
                PropertyValuesHolder.ofFloat(UP_TRANSITION, 0f, 1f).apply {
                    setEvaluator { fraction, _, _ ->
                        return@setEvaluator if (fraction <= 0.5f) {
                            floatEvaluator.evaluate(fraction, 1f, 2f)
                        } else {
                            floatEvaluator.evaluate(fraction, 0.64, 1f)
                        }
                    }
                },
                PropertyValuesHolder.ofInt(ALPHA, 0, 255)
            )

            needAnimate = true
        }
    }

    override fun render(canvas: Canvas) {
        for (i in 0 until countLines) {
            val animTransition: Float

            if (i == 0) {
                animTransition = 0f
                paint.alpha = 255
                linePaint.alpha = 255
            } else {
                animTransition = transition
                paint.alpha = alpha
                linePaint.alpha = alpha
            }

            canvas.drawText(
                displayedData[i],
                rootRenderer.spacingLeft().toFloat(),
                parentHeight - bottomMargin - i * spaceBetweenLines * animTransition,
                paint
            )

            canvas.drawLine(
                rootRenderer.spacingLeft().toFloat(),
                parentHeight - i * spaceBetweenLines * animTransition,
                parentWidth - rootRenderer.spacingRight().toFloat(),
                parentHeight - i * spaceBetweenLines * animTransition + lineHeight,
                linePaint
            )
        }
    }
}