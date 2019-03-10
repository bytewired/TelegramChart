package com.ne1c.telegramcharts.renderer

interface RangeValuesObservable {
    fun addObserverRangeValues(observer: RangeValuesObserver)

    fun removeObserveRangeValues(observer: RangeValuesObserver)
}