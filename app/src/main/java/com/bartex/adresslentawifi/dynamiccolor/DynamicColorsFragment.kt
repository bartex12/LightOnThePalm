package com.bartex.adresslentawifi.dynamiccolor

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
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import com.bartex.adresslentawifi.App
import com.bartex.adresslentawifi.R
import com.bartex.adresslentawifi.Server
import com.bartex.adresslentawifi.constants.Constants.BASE_URL
import com.bartex.adresslentawifi.constants.Constants.ITEM_NAME
import com.bartex.adresslentawifi.constants.Constants.KEY_WORD
import com.bartex.adresslentawifi.constants.Constants.TAG

class DynamicColorsFragment : Fragment(), Server.MessageListener{
    private  var nameOfEffect:String = ""
    private  var keyWord:String = ""
    private var brightness:String? = "50"
    private var speed:String? = "200"
    private lateinit var baseUrl:String
    private lateinit var seekBarPlayBrightness: SeekBar
    private lateinit var seekBarPlaySpeed: SeekBar
    private val dynamicColorViewModel: DynamicColorViewModel by lazy{
        ViewModelProvider(this).get(DynamicColorViewModel::class.java)
    }

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

        initViews(view)
        getSpeedAndBrightness()
        initSeekBarBrightness()
        initSeekBarSpeed()

        Server(baseUrl,  getString(R.string.post, keyWord, speed, brightness),
            this@DynamicColorsFragment).start()
    }

    override fun onPause() {
        super.onPause()
        dynamicColorViewModel.saveSpeedBrightness(nameOfEffect,speed,brightness)
    }

    private fun initViews(view: View) {
        seekBarPlayBrightness = view.findViewById(R.id.seekBarPlayBrightness)
        seekBarPlaySpeed = view.findViewById(R.id.seekBarSpeedRun)
        //если пламя, только яркость
        if (nameOfEffect == requireActivity().getString(R.string.flame) ){
            seekBarPlaySpeed.visibility = View.GONE
            view.findViewById<TextView>(R.id.tvSpeedRun).visibility = View.GONE
        }
    }

    private fun getSpeedAndBrightness() {
        speed =  dynamicColorViewModel.getSpeed(nameOfEffect)
        seekBarPlaySpeed.progress = seekBarPlaySpeed.max - Integer.parseInt(speed!!) //преобразуем шкалу

        brightness = dynamicColorViewModel.getBrightness(nameOfEffect)
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