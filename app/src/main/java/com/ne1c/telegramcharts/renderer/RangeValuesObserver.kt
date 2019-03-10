package com.ne1c.telegramcharts.renderer

import com.ne1c.telegramcharts.model.DataPoint

interface RangeValuesObserver {
    fun update(values: HashMap<String, ArrayList<DataPoint>>)
}