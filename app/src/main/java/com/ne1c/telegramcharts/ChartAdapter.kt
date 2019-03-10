package com.ne1c.telegramcharts

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.ne1c.telegramcharts.model.LineChartData
import com.ne1c.telegramcharts.view.ChartsSelectorLayout
import kotlinx.android.synthetic.main.view_chart.view.*

class ChartAdapter(private val charts: ArrayList<ArrayList<LineChartData>>) :
    RecyclerView.Adapter<ChartAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.view_chart, parent, false))
    }

    override fun getItemCount(): Int = charts.size

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        viewHolder.bind(charts[position])
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(chart: ArrayList<LineChartData>) {
            with(itemView) {
                chartView.charts = chart
                chartSliderView.charts = chart
                chartSelector.charts = chart

                chartView.attach(chartSliderView)

                chartSelector.listener = object : ChartsSelectorLayout.ShowChartListener {
                    override fun showChart(chart: LineChartData, show: Boolean) {
                        chartView.showChart(chart, show)
                        chartSliderView.showChart(chart, show)
                    }
                }
            }
        }
    }
}