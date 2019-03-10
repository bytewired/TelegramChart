package com.ne1c.telegramcharts.model

class LineChartData(
    val name: String,
    val color: Int,
    val x: LongArray,
    val y: LongArray,
    var isActive: Boolean,
    var alpha: Int = 255
)