package com.ne1c.telegramcharts.renderer

import android.content.Context
import android.view.ViewParent

interface RootRenderer {
    fun redraw()

    fun spacingLeft(): Int

    fun spacingRight(): Int

    fun spacingBottom(): Int

    fun spacingTop(): Int

    fun context(): Context

    fun parent(): ViewParent
}