package com.bartex.adresslentawifi.dynamiccolor

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.bartex.adresslentawifi.R
import com.bartex.adresslentawifi.Server
import com.bartex.adresslentawifi.SharedViewModel
import com.bartex.adresslentawifi.constants.Constants
import com.bartex.adresslentawifi.constants.Constants.BASE_URL
import com.bartex.adresslentawifi.constants.Constants.ITEM_NAME
import com.bartex.adresslentawifi.constants.Constants.KEY_WORD
import com.bartex.adresslentawifi.databinding.FragmentActionBinding
import com.bartex.adresslentawifi.databinding.FragmentDynamicsBinding

class DynamicColorsFragment : Fragment(), Server.MessageListener{
    private var _binding: FragmentDynamicsBinding? = null
    private val binding get() = _binding!!

    private val dynamicColorViewModel: DynamicColorViewModel by activityViewModels()
    private val model: SharedViewModel by activityViewModels()

    private var baseUrl:String = ""
    private  var nameOfEffect:String = ""
    private  var keyWord:String = ""
    private var brightness:String? = "50"
    private var speed:String? = "200"

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
        _binding = FragmentDynamicsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        getSpeedAndBrightness()
        initSeekBarBrightness()
        initSeekBarSpeed()

        sendMessageToServer()
    }

    override fun onPause() {
        super.onPause()
        dynamicColorViewModel.saveSpeedBrightness(nameOfEffect,speed,brightness)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun initViews(view: View) {
        //если пламя, оставляем только яркость
        if (nameOfEffect == requireActivity().getString(R.string.flame) ){
            binding.cardViewSpeedStat.visibility = View.GONE
        }
    }

    private fun getSpeedAndBrightness() {
        speed =  dynamicColorViewModel.getSpeed(nameOfEffect)
        binding.seekBarSpDin.progress = binding.seekBarSpDin.max - Integer.parseInt(speed!!) //преобразуем шкалу

        brightness = dynamicColorViewModel.getBrightness(nameOfEffect)
        binding.seekBarBrDin.progress = Integer.parseInt(brightness!!)
        //Log.d(TAG, "DynamicColorsFragment getSpeedAndBrightness speed= $speed brightness = $brightness")
    }

    private fun initSeekBarBrightness() {
        binding.seekBarBrDin.setOnSeekBarChangeListener(
            object : OnSeekBarChangeListener{
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
        var br =  binding.seekBarBrDin.progress
        if (br < 5) {
            br = 5
            binding.seekBarBrDin.progress = 5
        }
        return br.toString()
    }

    private fun initSeekBarSpeed() {
        binding.seekBarSpDin.setOnSeekBarChangeListener(
            object : OnSeekBarChangeListener{
                override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {}
                override fun onStartTrackingTouch(p0: SeekBar?) {}
                override fun onStopTrackingTouch(p0: SeekBar?) {
                    speed = getSpeed()
                    sendMessageToServer()
                }
            }
        )
    }

    private fun sendMessageToServer() {
        Server(baseUrl,
            getString(R.string.postDynamicPos, keyWord, speed, brightness, nameOfEffect),
            this@DynamicColorsFragment).start()
//        Log.d(TAG, "DynamicColorsFragment sendMessageToServer " +
//                "speed= $speed brightness = $brightness nameOfEffect = $nameOfEffect")
    }

    private fun getSpeed(): String {
        var sp =binding.seekBarSpDin.max - binding.seekBarSpDin.progress
        if (sp < 5) {
            sp = 5
            binding.seekBarSpDin.progress = binding.seekBarSpDin.max - 5
        }
        return sp.toString()
    }

    override fun onMessage(message: String) {
        try{
        requireActivity().runOnUiThread {
            model.updateNameOfEffect(message)
        }
            }catch (e:IllegalStateException){
                Log.d(Constants.TAG, "DynamicColorsFragment Exception message= $e.message")
            }
    }
}