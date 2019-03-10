package com.ne1c.telegramcharts.parser

import android.graphics.Color
import com.ne1c.telegramcharts.model.LineChartData
import org.json.JSONArray

// TODO: clean up
class Parser {
    private val COLUMNS_KEY = "columns"
    private val TYPES_KEY = "types"
    private val NAMES_KEY = "names"
    private val COLORS_KEY = "colors"

    fun parse(array: JSONArray): ArrayList<ArrayList<LineChartData>> {
        val mainList = arrayListOf<ArrayList<LineChartData>>()
        for (i in 0 until array.length()) {
            val subList = arrayListOf<LineChartData>()

            val obj = array.getJSONObject(i)

            val names = obj.getJSONObject(NAMES_KEY)
            val colors = obj.getJSONObject(COLORS_KEY)
            val columns = obj.getJSONArray(COLUMNS_KEY)

            val x = getX(columns)
            for (j in 0 until columns.length()) {
                val d = columns.getJSONArray(j)
                val key = d.getString(0)

                if (key == "x") continue

                val xArray = jsonArrayToLongArray(x!!)
                xArray.sort()

                val chartModel = LineChartData(
                    names.getString(key), Color.parseColor(colors.getString(key)),
                    xArray, jsonArrayToLongArray(d), true
                )

                subList.add(chartModel)
            }

            mainList.add(subList)
        }

        return mainList
    }

    private fun getX(columns: JSONArray): JSONArray? {
        for (i in 0 until columns.length()) {
            if (columns.getJSONArray(i)[0] == "x") return columns.getJSONArray(i)
        }

        return null
    }

    private fun jsonArrayToLongArray(array: JSONArray): LongArray {
        val longArray = LongArray(array.length() - 1)

        for (i in 1 until array.length()) {
            longArray[i - 1] = array.getLong(i)
        }

        return longArray
    }
}