package com.bartex.adresslentawifi.dynamiccolor

import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.fragment.app.Fragment
import com.bartex.adresslentawifi.R
import com.bartex.adresslentawifi.Server
import com.bartex.adresslentawifi.constants.Constants.BASE_URL
import com.bartex.adresslentawifi.constants.Constants.PLAY_BRIGHTNESS
import com.bartex.adresslentawifi.constants.Constants.PLAY_SPEED
import com.bartex.adresslentawifi.constants.Constants.TAG

class DynamicColorsFragment : Fragment(), Server.MessageListener{
    private var brightness:String? = "50"
    private var speed:String? = "200"
    private lateinit var baseUrl:String
    private lateinit var pref: SharedPreferences
    private lateinit var seekBarPlayBrightness: SeekBar
    private lateinit var seekBarPlaySpeed: SeekBar

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_dinamic_colors,container, false )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.getString(BASE_URL)?. let{baseUrl = it }
        pref = requireActivity().getSharedPreferences("MyPref", MODE_PRIVATE)

        initViews(view)
        getSpeedAndBrightness()
        initSeekBarBrightness()
        initSeekBarSpeed()

        Server(baseUrl, getString(R.string.postPlayColors,speed, brightness),
            this@DynamicColorsFragment).start()
    }

    private fun initViews(view: View) {
        seekBarPlayBrightness = view.findViewById(R.id.seekBarPlayBrightness)
        seekBarPlaySpeed = view.findViewById(R.id.seekBarPlaySpeed)
    }

    override fun onPause() {
        super.onPause()
        val editor = pref.edit()
        editor.putString(PLAY_SPEED, speed)
        editor.putString(PLAY_BRIGHTNESS, brightness)
        editor.apply()
    }

    private fun getSpeedAndBrightness() {
        speed = pref.getString(PLAY_SPEED, "200")
        seekBarPlaySpeed.progress = Integer.parseInt(speed!!)
        brightness = pref.getString(PLAY_BRIGHTNESS, "50")
        seekBarPlayBrightness.progress = Integer.parseInt(brightness!!)

        Server(baseUrl, getString(R.string.postPlayColors, speed, brightness),
            this@DynamicColorsFragment).start()
    }

    private fun initSeekBarBrightness() {
        seekBarPlayBrightness.setOnSeekBarChangeListener(
            object : OnSeekBarChangeListener{
                override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {}
                override fun onStartTrackingTouch(p0: SeekBar?) {}
                override fun onStopTrackingTouch(p0: SeekBar?) {
                    brightness = getBrightness()
                    speed = getSpeed()
                    Server(baseUrl, getString(R.string.postPlayColors, speed, brightness),
                        this@DynamicColorsFragment).start()
                }
            }
        )
    }

    private fun getBrightness(): String {
        var br = seekBarPlayBrightness.progress
        if (br < 5) {
            br = 5
            seekBarPlayBrightness.progress = 5
        }
        return br.toString()
    }

    private fun initSeekBarSpeed() {
       seekBarPlaySpeed.setOnSeekBarChangeListener(
            object : OnSeekBarChangeListener{
                override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {}
                override fun onStartTrackingTouch(p0: SeekBar?) {}
                override fun onStopTrackingTouch(p0: SeekBar?) {
                    speed = getSpeed()
                    brightness = getBrightness()
                    Server(baseUrl, getString(R.string.postPlayColors,speed, brightness),
                        this@DynamicColorsFragment).start()
                }
            }
        )
    }

    private fun getSpeed(): String {
        var br = seekBarPlaySpeed.progress
        if (br < 10) {
            br = 10
            seekBarPlayBrightness.progress = 10
        }
        return br.toString()
    }

    override fun onMessage(message: String) {
        requireActivity().runOnUiThread {
            Log.d(TAG, "DynamicColorsFragment onMessage message= $message")
        }
    }
}