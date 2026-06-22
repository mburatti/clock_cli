package com.example.clockapp

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.graphics.toColorInt
import com.github.dhaval2404.colorpicker.ColorPickerDialog
import com.github.dhaval2404.colorpicker.model.ColorShape
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.progressindicator.LinearProgressIndicator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder

class MainActivity : AppCompatActivity() {
    private val client = OkHttpClient()
    private var baseUrl = "http://172.30.153.123"

    private lateinit var statusText: TextView
    private lateinit var brightnessSeekBar: SeekBar
    private lateinit var nightBrightnessSeekBar: SeekBar
    private lateinit var themeCheckboxContainer: LinearLayout
    private lateinit var themeIntervalSpinner: Spinner
    private lateinit var storageProgress: LinearProgressIndicator
    private lateinit var storageText: TextView
    private lateinit var cityInput: EditText
    private lateinit var color1Input: EditText
    private lateinit var color2Input: EditText
    private lateinit var color3Input: EditText
    private lateinit var color1Preview: View
    private lateinit var color2Preview: View
    private lateinit var color3Preview: View
    private lateinit var ssidInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var ntpInput: EditText
    private lateinit var weatherKeyInput: EditText
    private lateinit var timeFormatSpinner: Spinner
    private lateinit var gifSpinner: Spinner
    private lateinit var startTimeSpinner: Spinner
    private lateinit var stopTimeSpinner: Spinner
    private lateinit var celsiusSwitch: MaterialSwitch
    private lateinit var hour12Switch: MaterialSwitch
    private lateinit var mileSwitch: MaterialSwitch
    private lateinit var syncSwitch: MaterialSwitch
    private lateinit var nightModeSwitch: MaterialSwitch
    
    private lateinit var connectionCard: View
    private lateinit var mainContentContainer: View

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        statusText = findViewById(R.id.statusText)
        brightnessSeekBar = findViewById(R.id.brightnessSeekBar)
        nightBrightnessSeekBar = findViewById(R.id.nightBrightnessSeekBar)
        themeCheckboxContainer = findViewById(R.id.themeCheckboxContainer)
        themeIntervalSpinner = findViewById(R.id.themeIntervalSpinner)
        storageProgress = findViewById(R.id.storageProgress)
        storageText = findViewById(R.id.storageText)
        cityInput = findViewById(R.id.cityInput)
        color1Input = findViewById(R.id.color1Input)
        color2Input = findViewById(R.id.color2Input)
        color3Input = findViewById(R.id.color3Input)
        color1Preview = findViewById(R.id.color1Preview)
        color2Preview = findViewById(R.id.color2Preview)
        color3Preview = findViewById(R.id.color3Preview)
        ssidInput = findViewById(R.id.ssidInput)
        passwordInput = findViewById(R.id.passwordInput)
        ntpInput = findViewById(R.id.ntpInput)
        weatherKeyInput = findViewById(R.id.weatherKeyInput)
        timeFormatSpinner = findViewById(R.id.timeFormatSpinner)
        gifSpinner = findViewById(R.id.gifSpinner)
        startTimeSpinner = findViewById(R.id.startTimeSpinner)
        stopTimeSpinner = findViewById(R.id.stopTimeSpinner)
        celsiusSwitch = findViewById(R.id.celsiusSwitch)
        hour12Switch = findViewById(R.id.hour12Switch)
        mileSwitch = findViewById(R.id.mileSwitch)
        syncSwitch = findViewById(R.id.syncSwitch)
        nightModeSwitch = findViewById(R.id.nightModeSwitch)
        
        connectionCard = findViewById(R.id.connectionCard)
        mainContentContainer = findViewById(R.id.mainContentContainer)
        
        setupSpinners()
        setupColorPickers()
        
        themeIntervalSpinner.onItemSelectedListener = SimpleSpinnerListener { position ->
            sendSetting("theme_interval", themeIntervalValues[position].toString())
        }

        findViewById<Button>(R.id.connectButton).setOnClickListener {
            val ip = findViewById<EditText>(R.id.ipInput).text.toString().trim()
            baseUrl = "http://$ip"
            statusText.text = getString(R.string.connecting_to, baseUrl)
            fetchConfig()
        }

        brightnessSeekBar.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {}
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar) {
                    sendSetting("lcd_brightness", seekBar.progress.toString())
                }
            },
        )

        nightBrightnessSeekBar.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {}
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar) {
                    sendSetting("nightbrightness", seekBar.progress.toString())
                }
            },
        )

        celsiusSwitch.setOnCheckedChangeListener { _, checked -> sendSetting("celsius", if (checked) "1" else "0") }
        hour12Switch.setOnCheckedChangeListener { _, checked -> sendSetting("time12_24", if (checked) "1" else "0") }
        mileSwitch.setOnCheckedChangeListener { _, checked -> sendSetting("mile", if (checked) "1" else "0") }
        syncSwitch.setOnCheckedChangeListener { _, checked -> sendSetting("sync", if (checked) "1" else "0") }
        nightModeSwitch.setOnCheckedChangeListener { _, checked -> sendSetting("nightmode", if (checked) "1" else "0") }

        timeFormatSpinner.onItemSelectedListener = SimpleSpinnerListener { position ->
            sendSetting("timeformat", position.toString())
        }

        gifSpinner.onItemSelectedListener = SimpleSpinnerListener { position ->
            sendSetting("gifnum", (position + 1).toString())
        }

        startTimeSpinner.onItemSelectedListener = SimpleSpinnerListener { position ->
            sendSetting("starttime", startTimeValues[position].toString())
        }

        stopTimeSpinner.onItemSelectedListener = SimpleSpinnerListener { position ->
            sendSetting("stoptime", stopTimeValues[position].toString())
        }

        findViewById<Button>(R.id.updateCityButton).setOnClickListener {
            sendSetting("city", cityInput.text.toString())
        }

        findViewById<Button>(R.id.applyColorsButton).setOnClickListener {
            val color1 = normalizeHex(color1Input.text.toString())
            val color2 = normalizeHex(color2Input.text.toString())
            val color3 = normalizeHex(color3Input.text.toString())
            color1?.let { sendSetting("color1", hexToRgb565(it).toString()) }
            color2?.let { sendSetting("color2", hexToRgb565(it).toString()) }
            color3?.let { sendSetting("color3", hexToRgb565(it).toString()) }
        }

        findViewById<Button>(R.id.saveWifiButton).setOnClickListener {
            sendSetting("ssid", ssidInput.text.toString())
            sendSetting("password", passwordInput.text.toString())
        }

        findViewById<Button>(R.id.saveNtpButton).setOnClickListener {
            sendSetting("ntp", ntpInput.text.toString())
        }

        findViewById<Button>(R.id.saveWeatherKeyButton).setOnClickListener {
            sendSetting("weatherkey", weatherKeyInput.text.toString())
        }

        findViewById<Button>(R.id.restartButton).setOnClickListener {
            sendAction("restart")
        }

        // Automatic connection on start
        statusText.text = getString(R.string.connecting_to, baseUrl)
        fetchConfig()
    }

    private fun setupSpinners() {
        val timeFormatOptions = listOf("MM/DD/YYYY", "DD/MM/YYYY", "YYYY/MM/DD")
        val gifOptions = listOf("spaceman", "divergent", "bird", "wave", "Customization")
        val startOptions = listOf("18:00", "19:00", "20:00", "21:00", "22:00", "23:00")
        val stopOptions = listOf("05:00", "06:00", "07:00", "08:00", "09:00", "10:00", "11:00", "12:00")

        timeFormatSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, timeFormatOptions)
        gifSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, gifOptions)
        startTimeSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, startOptions)
        stopTimeSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, stopOptions)

        val intervalOptions = listOf("Disable Interval", "5 Seconds", "10 Seconds", "15 Seconds", "30 Seconds", "1 Minute", "5 Minutes", "30 Minutes")
        themeIntervalSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, intervalOptions)
    }

    private fun setupColorPickers() {
        color1Input.setOnClickListener { pickColor(color1Input, color1Preview) }
        color2Input.setOnClickListener { pickColor(color2Input, color2Preview) }
        color3Input.setOnClickListener { pickColor(color3Input, color3Preview) }
    }

    private fun pickColor(editText: EditText, preview: View) {
        val currentColor = try {
            editText.text.toString().toColorInt()
        } catch (_: Exception) {
            android.graphics.Color.WHITE
        }

        ColorPickerDialog.Builder(this)
            .setTitle("Pick Color")
            .setColorShape(ColorShape.SQAURE)
            .setDefaultColor(currentColor)
            .setColorListener { color, colorHex ->
                editText.setText(colorHex)
                preview.setBackgroundColor(color)
            }
            .show()
    }

    private fun fetchConfig() {
        CoroutineScope(Dispatchers.IO).launch {
            val request = Request.Builder()
                .url("$baseUrl/config")
                .build()
            try {
                val response = client.newCall(request).execute()
                val body = response.body.string()
                if (response.isSuccessful) {
                    val json = JSONObject(body)
                    runOnUiThread {
                        statusText.text = getString(R.string.connected_to, baseUrl)
                        connectionCard.visibility = View.GONE
                        showMainContent(true)
                        applyConfig(json)
                    }
                } else {
                    runOnUiThread {
                        statusText.text = getString(R.string.connection_failed)
                        connectionCard.visibility = View.VISIBLE
                        showMainContent(false)
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    statusText.text = getString(R.string.error_status, e.message ?: "Unknown error")
                    connectionCard.visibility = View.VISIBLE
                    showMainContent(false)
                }
            }
        }
    }

    private fun showMainContent(show: Boolean) {
        val visibility = if (show) View.VISIBLE else View.GONE
        mainContentContainer.visibility = visibility
    }

    private fun applyConfig(config: JSONObject) {
        val themesArray = config.optJSONArray("themes")
        val activeThemes = config.optString("active_themes", "") // e.g. "0,1,3"
        
        if (themesArray != null) {
            themeCheckboxContainer.removeAllViews()
            val activeList = activeThemes.split(",").filter { it.isNotEmpty() }
            
            for (i in 0 until themesArray.length()) {
                val themeName = themesArray.getString(i)
                val cb = MaterialCheckBox(this).apply {
                    text = themeName
                    isChecked = activeList.contains(i.toString())
                    setOnCheckedChangeListener { _, _ ->
                        updateThemeLoop()
                    }
                }
                themeCheckboxContainer.addView(cb)
            }
        }

        val interval = config.optInt("theme_interval", 0)
        val intervalIndex = themeIntervalValues.indexOf(interval).coerceAtLeast(0)
        themeIntervalSpinner.setSelection(intervalIndex)

        val storageTotal = config.optLong("storage_total", 0)
        val storageUsed = config.optLong("storage_used", 0)
        if (storageTotal > 0) {
            val percent = ((storageUsed * 100) / storageTotal).toInt()
            storageProgress.progress = percent
            val freeMb = (storageTotal - storageUsed) / (1024 * 1024)
            val totalMb = storageTotal / (1024 * 1024)
            storageText.text = getString(R.string.used_storage, percent, freeMb, totalMb)
        }

        brightnessSeekBar.progress = config.optInt("brightness", brightnessSeekBar.progress).coerceIn(2, 99)
        nightBrightnessSeekBar.progress = config.optInt("nightbrightness", nightBrightnessSeekBar.progress).coerceIn(2, 99)
        cityInput.setText(config.optString("city", ""))
        celsiusSwitch.isChecked = config.optInt("celsius") == 1
        hour12Switch.isChecked = config.optInt("hour12") == 1
        mileSwitch.isChecked = config.optInt("mile") == 1
        syncSwitch.isChecked = config.optInt("sync") == 1
        nightModeSwitch.isChecked = config.optInt("nightmode") == 1

        val timeFormat = config.optInt("timeformat", 0).coerceIn(0, 2)
        timeFormatSpinner.setSelection(timeFormat)

        val gifIndex = (config.optInt("gifnum", 1) - 1).coerceIn(0, 4)
        gifSpinner.setSelection(gifIndex)

        val startTime = config.optInt("starttime", 18).coerceIn(18, 23)
        startTimeSpinner.setSelection(startTime - 18)

        val stopTime = config.optInt("stoptime", 5).coerceIn(5, 12)
        stopTimeSpinner.setSelection(stopTime - 5)

        val c1 = rgb565ToHex(config.optInt("color1", 0))
        val c2 = rgb565ToHex(config.optInt("color2", 0))
        val c3 = rgb565ToHex(config.optInt("color3", 0))

        color1Input.setText(c1)
        color2Input.setText(c2)
        color3Input.setText(c3)

        color1Preview.setBackgroundColor(c1.toColorInt())
        color2Preview.setBackgroundColor(c2.toColorInt())
        color3Preview.setBackgroundColor(c3.toColorInt())

        ssidInput.setText(config.optString("ssid", ""))
        passwordInput.setText(config.optString("password", ""))
        ntpInput.setText(config.optString("ntp", ""))
        weatherKeyInput.setText(config.optString("weatherkey", ""))
    }

    private fun sendSetting(key: String, value: String) {
        val encoded = URLEncoder.encode(value, "UTF-8")
        val url = "$baseUrl/api/set?key=$key&value=$encoded"
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                val body = response.body.string()
                runOnUiThread {
                    statusText.text = if (response.isSuccessful) {
                        getString(R.string.set_status, key, body.ifBlank { "ok" })
                    } else {
                        getString(R.string.failed_set, key)
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    statusText.text = getString(R.string.error_status, e.message ?: "Unknown error")
                }
            }
        }
    }

    private fun sendAction(path: String) {
        val url = "$baseUrl/$path"
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                runOnUiThread {
                    statusText.text = if (response.isSuccessful || (response.code == 200) || (response.code == 204)) {
                        getString(R.string.action_sent, path)
                    } else {
                        getString(R.string.action_failed)
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    statusText.text = getString(R.string.error_status, e.message ?: "Unknown error")
                }
            }
        }
    }

    private fun normalizeHex(value: String): String? {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return null
        val withHash = if (trimmed.startsWith("#")) trimmed else "#$trimmed"
        return if (withHash.length == 7 && withHash.substring(1).all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) {
            withHash
        } else {
            null
        }
    }

    private fun hexToRgb565(hex: String): Int {
        val color = hex.removePrefix("#")
        val r = color.substring(0, 2).toInt(16)
        val g = color.substring(2, 4).toInt(16)
        val b = color.substring(4, 6).toInt(16)
        return ((r shr 3) shl 11) or ((g shr 2) shl 5) or (b shr 3)
    }

    private fun rgb565ToHex(value: Int): String {
        val r = ((value shr 11) and 0x1F) * 255 / 31
        val g = ((value shr 5) and 0x3F) * 255 / 63
        val b = (value and 0x1F) * 255 / 31
        return String.format("#%02X%02X%02X", r, g, b)
    }

    private fun updateThemeLoop() {
        val selected = mutableListOf<Int>()
        for (i in 0 until themeCheckboxContainer.childCount) {
            val cb = themeCheckboxContainer.getChildAt(i) as? MaterialCheckBox
            if (cb?.isChecked == true) {
                selected.add(i)
            }
        }
        sendSetting("active_themes", selected.joinToString(","))
    }

    private companion object {
        val startTimeValues = listOf(18, 19, 20, 21, 22, 23)
        val stopTimeValues = listOf(5, 6, 7, 8, 9, 10, 11, 12)
        val themeIntervalValues = listOf(0, 5, 10, 15, 30, 60, 300, 1800)
    }

    private class SimpleSpinnerListener(
        private val onSelected: (Int) -> Unit
    ) : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            onSelected(position)
        }

        override fun onNothingSelected(parent: AdapterView<*>?) {}
    }
}
