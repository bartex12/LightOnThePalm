package com.bartex.adresslentawifi

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import com.bartex.adresslentawifi.constants.Constants
import com.bartex.adresslentawifi.constants.Constants.TEST_BRIGHTNESS
import com.bartex.adresslentawifi.constants.Constants.TEST_SPEED

class TestColorFragment: Fragment(), Server.MessageListener{
    private var brightness:String? = "50"
    private var speed:String? = "200"
    private lateinit var baseUrl:String
    private  var nameOfEffect:String = ""
    private  var keyWord:String = ""
    private lateinit var pref: SharedPreferences
    private lateinit var seekBarTestBrightness:SeekBar
    private lateinit var seekBarTestSpeed: SeekBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.getString(Constants.BASE_URL)?. let{baseUrl = it }
        arguments?.getString(Constants.ITEM_NAME)?. let{nameOfEffect = it }
        arguments?.getString(Constants.KEY_WORD)?. let{keyWord = it }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_test_color,container, false )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        pref = requireActivity().getSharedPreferences("MyPref", Context.MODE_PRIVATE)

        initView(view)
        getSpeedAndBrightness()
        initSeekBarBrightness()
        initSeekBarSpeed()

        sendMessageToServer()

        Server(baseUrl, getString(R.string.postDynamicPos,keyWord,speed, brightness, nameOfEffect),
            this@TestColorFragment).start()
    }

    private fun initView(view: View) {
        seekBarTestBrightness = view.findViewById(R.id.seekBarBri)
        seekBarTestSpeed = view.findViewById(R.id.seekBarSp)
    }

    override fun onPause() {
        super.onPause()
        val editor = pref.edit()
        editor.putString(TEST_SPEED, speed)
        editor.putString(TEST_BRIGHTNESS, brightness)
        editor.apply()
    }

    private fun getSpeedAndBrightness() {
        speed = pref.getString(TEST_SPEED, "200")
        seekBarTestSpeed.progress =seekBarTestSpeed.max - Integer.parseInt(speed!!)
        brightness = pref.getString(TEST_BRIGHTNESS, "50")
        seekBarTestBrightness.progress = Integer.parseInt(brightness!!)
    }

    private fun initSeekBarBrightness() {
        seekBarTestBrightness.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {}
                override fun onStartTrackingTouch(p0: SeekBar?) {}
                override fun onStopTrackingTouch(p0: SeekBar?) {
                    brightness = getBrightness()
                    sendMessageToServer()
                }
            }
        )
    }

    private fun getBrightness(): String {
        var br = seekBarTestBrightness.progress
        if (br < 5) {
            br = 5
            seekBarTestBrightness.progress = 5
        }
        return br.toString()
    }

    private fun initSeekBarSpeed() {
        seekBarTestSpeed.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {}
                override fun onStartTrackingTouch(p0: SeekBar?) {}
                override fun onStopTrackingTouch(p0: SeekBar?) {
                    speed = getSpeed()
                    sendMessageToServer()
                }
            }
        )
    }

    private fun getSpeed(): String {
        var sp = seekBarTestSpeed.max - seekBarTestSpeed.progress
        if (sp < 5) {
            sp = 5
            seekBarTestSpeed.progress = seekBarTestSpeed.max - 5
        }
        return sp.toString()
    }

    private fun sendMessageToServer() {
        Server(baseUrl,
            getString(R.string.postDynamicPos, keyWord, speed, brightness, nameOfEffect),
            this@TestColorFragment).start()
    }

    override fun onMessage(message: String) {
        requireActivity().runOnUiThread {
            Log.d(Constants.TAG, "TestColorFragment  onMessage message= $message")
        }
    }
}