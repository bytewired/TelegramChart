package com.ne1c.telegramcharts

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.view.Menu
import android.view.MenuItem
import com.ne1c.telegramcharts.parser.Parser
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONArray
import java.io.ByteArrayOutputStream
import java.io.IOException

class MainActivity : AppCompatActivity() {
    private var nightTheme = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) nightTheme = savedInstanceState.getBoolean("theme")

        setTheme(if (nightTheme) R.style.AppTheme_Night else R.style.AppTheme_Day)
        setContentView(R.layout.activity_main)

        // In this app parsing in UI thread doesn't affect performance
        val list = Parser().parse(JSONArray(readFileFromRawDirectory(R.raw.chart_data)))

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = ChartAdapter(list)
    }

    private fun readFileFromRawDirectory(resourceId: Int): String {
        val iStream = resources.openRawResource(resourceId)
        var byteStream: ByteArrayOutputStream? = null

        try {
            val buffer = ByteArray(iStream.available())
            iStream.read(buffer)
            byteStream = ByteArrayOutputStream()
            byteStream.write(buffer)
            byteStream.close()
            iStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return byteStream?.toString() ?: ""
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main, menu)

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.itemId == R.id.changeTheme) {
            nightTheme = !nightTheme

            recreate()
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        outState?.putBoolean("theme", nightTheme)

        super.onSaveInstanceState(outState)
    }
}
