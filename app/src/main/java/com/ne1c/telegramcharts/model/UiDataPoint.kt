package com.ne1c.telegramcharts.model

import android.content.Context
import android.graphics.Paint
import android.text.format.DateUtils
import com.ne1c.telegramcharts.numberToString

class UiDataPoint(
    val date: String,
    val dataText: String,
    val dataPoint: DataPoint,
    val dataTextWidth: Float
) {
    companion object {
        fun map(context: Context, paint: Paint, dataPoint: DataPoint): UiDataPoint {
            val date = DateUtils.formatDateTime(
                context, dataPoint.x,
                DateUtils.FORMAT_ABBREV_ALL or DateUtils.FORMAT_NO_YEAR
            )

            val yValue = numberToString(dataPoint.y)

            return UiDataPoint(date, yValue, dataPoint, paint.measureText(yValue))
        }
    }
}