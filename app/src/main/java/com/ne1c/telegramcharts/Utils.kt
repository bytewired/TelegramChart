package com.ne1c.telegramcharts

import com.ne1c.telegramcharts.model.DataPoint
import java.util.*

fun numberToString(value: Long): String {
    if (value / 1_000_000 > 0) {
        return String.format(Locale.ENGLISH, "%.2fM", value.toFloat() / 1_000_000)
    }

    if (value / 1000 > 0) {
        return String.format(Locale.ENGLISH, "%.2fK", value.toFloat() / 1000)
    }

    return value.toString()
}

fun getMin(longArray: LongArray, start: Int = 0, end: Int = longArray.lastIndex): Long {
    var min = longArray[start]

    for (i in start..end) {
        if (longArray[i] < min) {
            min = longArray[i]
        }
    }

    return min
}

fun getMax(longArray: LongArray, start: Int = 0, end: Int = longArray.lastIndex): Long {
    var max = longArray[start]

    for (i in start..end) {
        if (longArray[i] > max) {
            max = longArray[i]
        }
    }

    return max
}

fun getClosestValue(x: Float, list: List<DataPoint>): DataPoint {
    var closest:DataPoint = list[0]

    for (i in 1 until list.size) {
        if (Math.abs(x - list[i].xFitted) < Math.abs(x - closest.xFitted)) {
            closest = list[i]
        }
    }

    return closest
}