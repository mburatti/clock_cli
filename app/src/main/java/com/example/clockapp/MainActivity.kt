package com.example.clockapp

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
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
    private lateinit var cityInput: EditText
    private lateinit var color1Input: EditText
    private lateinit var color2Input: EditText
    private lateinit var color3Input: EditText
    private lateinit var ssidInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var ntpInput: EditText
    private lateinit var weatherKeyInput: EditText
    private lateinit var timeFormatSpinner: Spinner
    private lateinit var gifSpinner: Spinner
    private lateinit var startTimeSpinner: Spinner
    private lateinit var stopTimeSpinner: Spinner
    private lateinit var celsiusSwitch: Switch
    private lateinit var hour12Switch: Switch
    private lateinit var mileSwitch: Switch
    private lateinit var syncSwitch: Switch
    private lateinit var nightModeSwitch: Switch

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        brightnessSeekBar = findViewById(R.id.brightnessSeekBar)
        nightBrightnessSeekBar = findViewById(R.id.nightBrightnessSeekBar)
        cityInput = findViewById(R.id.cityInput)
        color1Input = findViewById(R.id.color1Input)
        color2Input = findViewById(R.id.color2Input)
        color3Input = findViewById(R.id.color3Input)
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

        setupSpinners()

        findViewById<Button>(R.id.connectButton).setOnClickListener {
            baseUrl = "http://" + findViewById<EditText>(R.id.ipInput).text.toString().trim()
            statusText.text = "Connecting to $baseUrl..."
            fetchConfig()
        }

        brightnessSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {}
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                sendSetting("lcd_brightness", seekBar.progress.toString())
            }
        })

        nightBrightnessSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {}
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                sendSetting("nightbrightness", seekBar.progress.toString())
            }
        })

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
            if (color1 != null) sendSetting("color1", hexToRgb565(color1).toString())
            if (color2 != null) sendSetting("color2", hexToRgb565(color2).toString())
            if (color3 != null) sendSetting("color3", hexToRgb565(color3).toString())
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
    }

    private fun fetchConfig() {
        CoroutineScope(Dispatchers.IO).launch {
            val request = Request.Builder()
                .url("$baseUrl/config")
                .build()
            try {
                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val json = JSONObject(body)
                    runOnUiThread {
                        statusText.text = "Connected to $baseUrl"
                        applyConfig(json)
                    }
                } else {
                    runOnUiThread {
                        statusText.text = "Connection failed"
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    statusText.text = "Error: ${e.message}"
                }
            }
        }
    }

    private fun applyConfig(config: JSONObject) {
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

        color1Input.setText(rgb565ToHex(config.optInt("color1", 0)))
        color2Input.setText(rgb565ToHex(config.optInt("color2", 0)))
        color3Input.setText(rgb565ToHex(config.optInt("color3", 0)))

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
                val body = response.body?.string() ?: ""
                runOnUiThread {
                    statusText.text = if (response.isSuccessful) {
                        "Set $key: ${body.ifBlank { "ok" }}"
                    } else {
                        "Failed to set $key"
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    statusText.text = "Error: ${e.message}"
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
                    statusText.text = if (response.isSuccessful || response.code == 200 || response.code == 204) {
                        "Action sent to $path"
                    } else {
                        "Action failed"
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    statusText.text = "Error: ${e.message}"
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

    private companion object {
        val startTimeValues = listOf(18, 19, 20, 21, 22, 23)
        val stopTimeValues = listOf(5, 6, 7, 8, 9, 10, 11, 12)
    }

    private class SimpleSpinnerListener(
        private val onSelected: (Int) -> Unit
    ) : android.widget.AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
            onSelected(position)
        }

        override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
    }
}
