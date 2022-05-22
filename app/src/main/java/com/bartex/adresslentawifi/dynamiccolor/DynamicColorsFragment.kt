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
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.bartex.adresslentawifi.R
import com.bartex.adresslentawifi.Server
import com.bartex.adresslentawifi.constants.Constants
import com.bartex.adresslentawifi.constants.Constants.BASE_URL
import com.bartex.adresslentawifi.constants.Constants.FLAME_BRIGHTNESS
import com.bartex.adresslentawifi.constants.Constants.FLAME_SPEED
import com.bartex.adresslentawifi.constants.Constants.ITEM_NAME
import com.bartex.adresslentawifi.constants.Constants.KEY_WORD
import com.bartex.adresslentawifi.constants.Constants.PLAY_BRIGHTNESS
import com.bartex.adresslentawifi.constants.Constants.PLAY_SPEED
import com.bartex.adresslentawifi.constants.Constants.PRIDE_BRIGHTNESS
import com.bartex.adresslentawifi.constants.Constants.PRIDE_SPEED
import com.bartex.adresslentawifi.constants.Constants.RAINBOW_BIAS_BRIGHTNESS
import com.bartex.adresslentawifi.constants.Constants.RAINBOW_BIAS_SPEED
import com.bartex.adresslentawifi.constants.Constants.RAINBOW_BRIGHTNESS
import com.bartex.adresslentawifi.constants.Constants.RAINBOW_SPEED
import com.bartex.adresslentawifi.constants.Constants.RANDOM_COLOR_BRIGHTNESS
import com.bartex.adresslentawifi.constants.Constants.RANDOM_COLOR_SPEED
import com.bartex.adresslentawifi.constants.Constants.TAG

class DynamicColorsFragment : Fragment(), Server.MessageListener{
    private  var nameOfEffect:String = ""
    private  var keyWord:String = ""
    private lateinit var name: TextView
    private var brightness:String? = "50"
    private var speed:String? = "200"
    private lateinit var baseUrl:String
    private lateinit var pref: SharedPreferences
    private lateinit var seekBarPlayBrightness: SeekBar
    private lateinit var seekBarPlaySpeed: SeekBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.getString(BASE_URL)?. let{baseUrl = it }
        arguments?.getString(ITEM_NAME)?. let{nameOfEffect = it }
        arguments?.getString(KEY_WORD)?. let{keyWord = it }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_dinamic_colors,container, false )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        pref = requireActivity().getSharedPreferences("MyPref", MODE_PRIVATE)

        initViews(view)
        getSpeedAndBrightness()
        initSeekBarBrightness()
        initSeekBarSpeed()

        Server(baseUrl,  getString(R.string.post, keyWord, speed, brightness),
            this@DynamicColorsFragment).start()
    }

    private fun initViews(view: View) {
        seekBarPlayBrightness = view.findViewById(R.id.seekBarPlayBrightness)
        seekBarPlaySpeed = view.findViewById(R.id.seekBarPlaySpeed)
        //если пламя, только яркость
        if (nameOfEffect == requireActivity().getString(R.string.flame) ){
            seekBarPlaySpeed.visibility = View.GONE
            view.findViewById<TextView>(R.id.tvSpeed).visibility = View.GONE
        }
        name = view.findViewById(R.id.tvVariantPlay)
        name.text = nameOfEffect
    }

    override fun onPause() {
        super.onPause()
        val editor = pref.edit()
        when(nameOfEffect){
            requireActivity().getString(R.string.playOfColors) -> {
                editor.putString(PLAY_SPEED, speed)
                editor.putString(PLAY_BRIGHTNESS, brightness)
            }
                requireActivity().getString(R.string.flame) ->{
                    editor.putString(FLAME_SPEED, speed)
                    editor.putString(FLAME_BRIGHTNESS, brightness)
                }
            requireActivity().getString(R.string.rainbow) ->{
                editor.putString(RAINBOW_SPEED, speed)
                editor.putString(RAINBOW_BRIGHTNESS, brightness)
            }
            requireActivity().getString(R.string.pride) ->{
                editor.putString(PRIDE_SPEED, speed)
                editor.putString(PRIDE_BRIGHTNESS, brightness)
            }
            requireActivity().getString(R.string.rainbow_bias) ->{
                editor.putString(RAINBOW_BIAS_SPEED, speed)
                editor.putString(RAINBOW_BIAS_BRIGHTNESS, brightness)
            }
            requireActivity().getString(R.string.random_color) -> {
                editor.putString(RANDOM_COLOR_SPEED, speed)
                editor.putString(RANDOM_COLOR_BRIGHTNESS, brightness)
            }
        }
        editor.apply()
        Log.d(TAG, "DynamicColorsFragment onPause nameOfEffect= $nameOfEffect $speed $brightness")
    }

    private fun getSpeedAndBrightness() {
        speed = when(nameOfEffect){
            requireActivity().getString(R.string.playOfColors) -> pref.getString(PLAY_SPEED, "200")
            requireActivity().getString(R.string.flame) -> pref.getString(FLAME_SPEED, "200")
            requireActivity().getString(R.string.rainbow) -> pref.getString(RAINBOW_SPEED, "200")
            requireActivity().getString(R.string.pride) -> pref.getString(PRIDE_SPEED, "200")
            requireActivity().getString(R.string.rainbow_bias) -> pref.getString(RAINBOW_BIAS_SPEED, "200")
            requireActivity().getString(R.string.random_color) -> pref.getString(RANDOM_COLOR_SPEED, "200")
            else ->  pref.getString(PLAY_SPEED, "200")
        }
        seekBarPlaySpeed.progress = seekBarPlaySpeed.max - Integer.parseInt(speed!!) //преобразуем шкалу

        brightness = when(nameOfEffect){
            requireActivity().getString(R.string.playOfColors) -> pref.getString(PLAY_BRIGHTNESS, "50")
            requireActivity().getString(R.string.flame) -> pref.getString(FLAME_BRIGHTNESS, "50")
            requireActivity().getString(R.string.rainbow) -> pref.getString(RAINBOW_BRIGHTNESS, "50")
            requireActivity().getString(R.string.pride) -> pref.getString(PRIDE_BRIGHTNESS, "50")
            requireActivity().getString(R.string.rainbow_bias) -> pref.getString(RAINBOW_BIAS_BRIGHTNESS, "50")
            requireActivity().getString(R.string.random_color) -> pref.getString(RANDOM_COLOR_BRIGHTNESS, "50")
            else ->  pref.getString(PLAY_BRIGHTNESS, "50")
        }
        seekBarPlayBrightness.progress = Integer.parseInt(brightness!!)
        Log.d(TAG, "DynamicColorsFragment getSpeedAndBrightness speed= $speed brightness = $brightness")

        Server(baseUrl, getString(R.string.post, keyWord, speed, brightness),
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
                    Server(baseUrl,  getString(R.string.post, keyWord, speed, brightness),
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
                    Server(baseUrl, getString(R.string.post, keyWord, speed, brightness),
                        this@DynamicColorsFragment).start()
                }
            }
        )
    }

    private fun getSpeed(): String {
        var sp =seekBarPlaySpeed.max - seekBarPlaySpeed.progress
        if (sp < 5) {
            sp = 5
            seekBarPlaySpeed.progress = seekBarPlaySpeed.max - 5
        }
        return sp.toString()
    }

    override fun onMessage(message: String) {
        requireActivity().runOnUiThread {
            Log.d(TAG, "DynamicColorsFragment onMessage message= $message")
        }
    }
}