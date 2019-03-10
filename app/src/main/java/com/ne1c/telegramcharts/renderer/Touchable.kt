package com.ne1c.telegramcharts.renderer

import android.view.MotionEvent

interface Touchable {
    fun onTouchEvent(event: MotionEvent): Boolean
}