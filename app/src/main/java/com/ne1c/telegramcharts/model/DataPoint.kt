package com.ne1c.telegramcharts.model

open class DataPoint(
    val x: Long,
    val y: Long,
    var xFitted: Float,
    var yFitted: Float,
    val chartColor: Int,
    val chartName: String
)