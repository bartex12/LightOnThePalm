package com.bartex.adresslentawifi.flame

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import com.bartex.adresslentawifi.R
import com.bartex.adresslentawifi.Server
import com.bartex.adresslentawifi.constants.Constants
import com.bartex.adresslentawifi.constants.Constants.FLAME_BRIGHTNESS
import com.bartex.adresslentawifi.constants.Constants.HEIGHT_FLAME
import com.bartex.adresslentawifi.constants.Constants.SPARK_FLAME

class FlameFragment: Fragment(), Server.MessageListener {
    private var brightness:String? = "50"
    private var heightFlame:String? = "55"
    private var sparkFlame:String? = "120"

    private lateinit var baseUrl:String
    private  var nameOfEffect:String = ""
    private  var keyWord:String = ""
    private lateinit var pref: SharedPreferences
    private lateinit var seekBarBrFlame:SeekBar
    private lateinit var seekBarHeightFlame: SeekBar
    private lateinit var seekBarSparkFlame: SeekBar

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
        return inflater.inflate(R.layout.fragment_flame,container, false )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        pref = requireActivity().getSharedPreferences("MyPref", Context.MODE_PRIVATE)

        initView(view)
        getSparkHeightBrightness()
        initSeekBarBrightness()
        initSeekBarHeight()
        initSeekBarSpark()

        sendMessageToServer()
    }

    private fun initSeekBarSpark() {
        seekBarSparkFlame.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {}
                override fun onStartTrackingTouch(p0: SeekBar?) {}
                override fun onStopTrackingTouch(p0: SeekBar?) {
                    sparkFlame = getSparkFlame()
                    sendMessageToServer()
                }
            }
        )
    }

    private fun getSparkFlame(): String {
        var sf = seekBarSparkFlame.progress
        if (sf < 5) {
            sf = 5
            seekBarSparkFlame.progress = 5
        }
        return sf.toString()
    }

    private fun initView(view: View) {
        seekBarBrFlame = view.findViewById(R.id.seekBarBrFlame)
        seekBarHeightFlame = view.findViewById(R.id.seekBarHightFlame)
        seekBarSparkFlame = view.findViewById(R.id.seekBarSparkFlame)
    }

    override fun onPause() {
        super.onPause()
        val editor = pref.edit()
        editor.putString(FLAME_BRIGHTNESS, brightness)
        editor.putString(HEIGHT_FLAME, heightFlame)
        editor.putString(SPARK_FLAME, sparkFlame)
        editor.apply()
    }

    private fun getSparkHeightBrightness() {
        brightness = pref.getString(FLAME_BRIGHTNESS, "50")
        seekBarBrFlame.progress = Integer.parseInt(brightness!!)
        heightFlame = pref.getString(HEIGHT_FLAME, "55")
        seekBarHeightFlame.progress =Integer.parseInt(heightFlame!!)
        sparkFlame = pref.getString(SPARK_FLAME, "120")
        seekBarSparkFlame.progress =Integer.parseInt(sparkFlame!!)
    }

    private fun initSeekBarBrightness() {
        seekBarBrFlame.setOnSeekBarChangeListener(
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
        var br = seekBarBrFlame.progress
        if (br < 5) {
            br = 5
            seekBarBrFlame.progress = 5
        }
        return br.toString()
    }

    private fun initSeekBarHeight() {
        seekBarHeightFlame.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {}
                override fun onStartTrackingTouch(p0: SeekBar?) {}
                override fun onStopTrackingTouch(p0: SeekBar?) {
                    heightFlame = getHeightFlame()
                    sendMessageToServer()
                }
            }
        )
    }

    private fun getHeightFlame(): String {
        var hf = seekBarHeightFlame.progress
        if (hf < 5) {
            hf = 5
            seekBarHeightFlame.progress = 5
        }
        return hf.toString()
    }

    private fun sendMessageToServer() {
        Server(baseUrl,
            getString(R.string.postFlame, keyWord, brightness, heightFlame, sparkFlame, nameOfEffect),
            this@FlameFragment).start()
    }

    override fun onMessage(message: String) {
        requireActivity().runOnUiThread {
            Log.d(Constants.TAG, "FlameFragment  onMessage message= $message")
        }
    }
}