package com.bartex.adresslentawifi.running

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bartex.adresslentawifi.R
import com.bartex.adresslentawifi.Server
import com.bartex.adresslentawifi.SharedViewModel
import com.bartex.adresslentawifi.adapter.StaticColorAdapter
import com.bartex.adresslentawifi.constants.Constants
import com.bartex.adresslentawifi.constants.Constants.TAG
import com.bartex.adresslentawifi.entity.ButtonColor

class RunningColorsFragment: Fragment(), Server.MessageListener {

    private val runningViewModel: RunningViewModel  by activityViewModels()
    private val model: SharedViewModel by activityViewModels()

    private lateinit var baseUrl:String
    private  var nameOfEffect:String = ""
    private  var keyWord:String = ""

    private var speed:String? = "50"
    private var progress:String? = "50"
    private var brightness:String? = "50"
    private  var number:String? = "1"
    private var color:String? = "0x00FFFF"

    private lateinit var seekBarRunBrightness: SeekBar
    private lateinit var seekBarRunSpeed: SeekBar
    private lateinit var seekBarRunNumber: SeekBar
    private lateinit var rvColors: RecyclerView
    private lateinit var adapter: StaticColorAdapter



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
        return inflater.inflate(R.layout.fragment_running, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        getSpeedBrightnessNumberColor() // получаем из преференсис
        initSeekBarBrightness()
        initSeekBarSpeed()
        initSeekBarNumber()
        initAdapter()

        sendMessageToServer()

        runningViewModel.loadStaticColors()
        runningViewModel.getStaticColors().observe(this, {
            renderData(it)
        })
    }

    override fun onPause() {
        super.onPause()
        val progress = seekBarRunSpeed.progress.toString()
        runningViewModel.saveSpeedBrightness(nameOfEffect, progress, brightness)
        runningViewModel.saveNumbers(nameOfEffect, number)
        runningViewModel.saveColor(nameOfEffect, color)
    }

    private fun renderData(data: List<ButtonColor>?) {
        data?. let{ adapter.listOfButtonColor = it}
    }

    private fun initAdapter() {
        rvColors.layoutManager = GridLayoutManager(requireActivity(), 6)
        adapter = StaticColorAdapter(getOnClickListener())
        rvColors.adapter = adapter
    }

    private fun getOnClickListener(): StaticColorAdapter.OnItemClickListener =
        object : StaticColorAdapter.OnItemClickListener{
            override fun onClick(item: ButtonColor) {
                color = item.color.replace("#", "0x")
                sendMessageToServer()
            }
        }

    private fun initViews(view: View) {
        rvColors = view.findViewById(R.id.rvColorsRunning)
        seekBarRunBrightness = view.findViewById(R.id.seekBarBrRunning)
        seekBarRunBrightness.max = 255
        seekBarRunSpeed = view.findViewById(R.id.seekBarSpeedRunning)
        seekBarRunSpeed.max = 70
        seekBarRunNumber = view.findViewById(R.id.seekBarNumberRunning)
        seekBarRunNumber.max = 10
    }

    private fun getSpeedBrightnessNumberColor() {
        progress =  runningViewModel.getSpeed(nameOfEffect)
        seekBarRunSpeed.progress = Integer.parseInt(progress!!) //преобразуем шкалу

        brightness = runningViewModel.getBrightness(nameOfEffect)
        seekBarRunBrightness.progress = Integer.parseInt(brightness!!)

        number = runningViewModel.getNumbers(nameOfEffect)
        seekBarRunNumber.progress = Integer.parseInt(number!!)

        color = runningViewModel.getColor(nameOfEffect)
        //Log.d(TAG, "RunningColorsFragment  speed= $speed brightness = $brightness number = $number")
    }

    private fun initSeekBarBrightness() {
        seekBarRunBrightness.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {}
                override fun onStartTrackingTouch(p0: SeekBar?) {}
                override fun onStopTrackingTouch(p0: SeekBar?) {
                    brightness = getBrightnessFromSeekBar()
                    sendMessageToServer()
                }
            }
        )
    }

    private fun initSeekBarSpeed() {
        seekBarRunSpeed.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {}
                override fun onStartTrackingTouch(p0: SeekBar?) {}
                override fun onStopTrackingTouch(p0: SeekBar?) {
                    speed = getSpeedFromSeekBar()
                    sendMessageToServer()
                }
            }
        )
    }

    private fun initSeekBarNumber() {
        seekBarRunNumber.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {}
                override fun onStartTrackingTouch(p0: SeekBar?) {}
                override fun onStopTrackingTouch(p0: SeekBar?) {
                    number = getNumberFromSeekBar()
                    sendMessageToServer()
                }
            }
        )
    }

    fun sendMessageToServer(){
        Server(baseUrl,  getString(R.string.postRunPos, keyWord, speed, brightness, number, color,nameOfEffect),
            this@RunningColorsFragment).start()
    }
    private fun getSpeedFromSeekBar(): String {
        seekBarRunSpeed.max = 70 //чтобы посылать на сервер от 5 до 70 мс задержку
        if (seekBarRunSpeed.progress > 65) {
            seekBarRunSpeed.progress = 65
        }
        val sp = seekBarRunSpeed.max - seekBarRunSpeed.progress
        return sp.toString()
    }
    private fun getBrightnessFromSeekBar(): String {
        var br = seekBarRunBrightness.progress
        if (br < 5) {
            br = 5
            seekBarRunBrightness.progress = br
        }
        return br.toString()
    }

    private fun getNumberFromSeekBar(): String {
        val num = seekBarRunNumber.progress
        return num.toString()
    }

    override fun onMessage(message: String) {
        try{
            requireActivity().runOnUiThread {
                //Log.d(TAG, "RunningColorsFragment onMessage message= $message")
                model.updateNameOfEffect(message)
            }
        }catch (e:IllegalStateException){
            Log.d(TAG, "RunningColorsFragment Exception message= $e.message")
        }
    }
}