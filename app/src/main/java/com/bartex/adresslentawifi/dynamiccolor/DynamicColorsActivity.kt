package com.bartex.adresslentawifi.dynamiccolor

import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import com.bartex.adresslentawifi.R
import com.bartex.adresslentawifi.Server
import com.bartex.adresslentawifi.databinding.ActivityDinamicColorsBinding
import com.bartex.adresslentawifi.main.MainActivity

class DynamicColorsActivity : AppCompatActivity(), Server.MessageListener {
    private lateinit var binding:ActivityDinamicColorsBinding
    private var brightness:String? = "50"
    private var speed:String? = "200"
    private lateinit var extraIp:String
    private lateinit var pref: SharedPreferences

    companion object{
        const val TAG = "33333"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDinamicColorsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        intent.getStringExtra(MainActivity.IP)?. let{extraIp = it }
        pref = getSharedPreferences("MyPref", MODE_PRIVATE)
        getSpeedAndBrightness()

        initSeekBarBrightness()
        initSeekBarSpeed()

        Server(extraIp, getString(R.string.postPlayColors,speed, brightness),
            this@DynamicColorsActivity).start()
    }

    override fun onPause() {
        super.onPause()
        val editor = pref.edit()
        editor.putString("playSpeed", speed)
        editor.putString("playBrightness", brightness)
        editor.apply()
    }

    private fun getSpeedAndBrightness() {
        speed = pref.getString("playSpeed", "200")
        binding.seekBarPlaySpeed.progress = Integer.parseInt(speed!!)
        brightness = pref.getString("playBrightness", "50")
        binding.seekBarPlayBrightness.progress = Integer.parseInt(brightness!!)

        Server(extraIp, getString(R.string.postPlayColors, speed, brightness),
            this@DynamicColorsActivity).start()
    }

    private fun initSeekBarBrightness() {
        binding.seekBarPlayBrightness.setOnSeekBarChangeListener(
            object : OnSeekBarChangeListener{
                override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {}
                override fun onStartTrackingTouch(p0: SeekBar?) {}
                override fun onStopTrackingTouch(p0: SeekBar?) {
                    brightness = getBrightness()
                    speed = getSpeed()
                    Server(extraIp, getString(R.string.postPlayColors, speed, brightness),
                        this@DynamicColorsActivity).start()
                }
            }
        )
    }

    private fun getBrightness(): String {
        var br = binding.seekBarPlayBrightness.progress
        if (br < 5) {
            br = 5
            binding.seekBarPlayBrightness.progress = 5
        }
        return br.toString()
    }

    private fun initSeekBarSpeed() {
        binding.seekBarPlaySpeed.setOnSeekBarChangeListener(
            object : OnSeekBarChangeListener{
                override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {}
                override fun onStartTrackingTouch(p0: SeekBar?) {}
                override fun onStopTrackingTouch(p0: SeekBar?) {
                    speed = getSpeed()
                    brightness = getBrightness()
                    Server(extraIp, getString(R.string.postPlayColors,speed, brightness),
                        this@DynamicColorsActivity).start()
                }
            }
        )
    }

    private fun getSpeed(): String {
        var br = binding.seekBarPlaySpeed.progress
        if (br < 10) {
            br = 10
            binding.seekBarPlayBrightness.progress = 10
        }
        return br.toString()
    }

    override fun onMessage(message: String) {
        runOnUiThread {
            Log.d(TAG, "DynamicColorsActivity onMessage message= $message")
        }
    }
}