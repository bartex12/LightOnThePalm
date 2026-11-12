package com.bartex.adresslentawifi.staticcolor

import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bartex.adresslentawifi.R
import com.bartex.adresslentawifi.EspApi
import com.bartex.adresslentawifi.toastNoConnection
import com.bartex.adresslentawifi.SharedViewModel
import com.bartex.adresslentawifi.adapter.StaticColorAdapter
import com.bartex.adresslentawifi.constants.Constants
import com.bartex.adresslentawifi.constants.Constants.BASE_URL
import com.bartex.adresslentawifi.constants.Constants.BRIGHTNESS
import com.bartex.adresslentawifi.constants.Constants.COLOR
import com.bartex.adresslentawifi.constants.Constants.ITEM_NAME
import com.bartex.adresslentawifi.constants.Constants.KEY_WORD
import com.bartex.adresslentawifi.entity.ButtonColor

class StaticColorFragment: Fragment() {

    private val viewModel: StaticColorsViewModel by activityViewModels()
    private val model: SharedViewModel by activityViewModels()

    private lateinit var baseUrl:String
    private  var nameOfEffect:String = ""
    private  var keyWord:String = ""
    private lateinit var adapter: StaticColorAdapter
    private var color:String? = "0x00FFFF"
    private var brightness:String? = "5"
    private lateinit var pref: SharedPreferences
    private lateinit var seekBarStatic: SeekBar
    private lateinit var rvColors: RecyclerView


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
        return inflater.inflate(R.layout.fragment_static, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        pref = requireActivity().getSharedPreferences("MyPref", AppCompatActivity.MODE_PRIVATE)

        initViews(view)
        getColorAndBrightness()
        initAdapter()
        initSeekBar()
        sendMessageToServer()

        viewModel.loadStaticColors()
        viewModel.getStaticColors().observe(viewLifecycleOwner) {
            renderData(it)
        }
    }

    override fun onPause() {
        super.onPause()
        val editor = pref.edit()
        editor.putString(COLOR, color)
        editor.putString(BRIGHTNESS, brightness)
        editor.apply()
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

    private fun renderData(data: List<ButtonColor>?) {
        data?. let{ adapter.listOfButtonColor = it}
    }

    private fun initSeekBar() {
        seekBarStatic.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener{
                override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {}
                override fun onStartTrackingTouch(p0: SeekBar?) {}
                override fun onStopTrackingTouch(p0: SeekBar?) {
                    brightness = setBrightness()
                    sendMessageToServer()
                }
            }
        )
    }

    private fun setBrightness(): String {
        var br = seekBarStatic.progress
        if (br < 5) {
            br = 5
            seekBarStatic.progress = 5
        }
        return br.toString()
    }

    private fun getColorAndBrightness() {
        color = pref.getString(COLOR, "0x00FFFF")
        brightness = pref.getString(BRIGHTNESS, "5")
        seekBarStatic.progress = Integer.parseInt(brightness!!)
    }

    private fun initViews(view: View) {
        seekBarStatic = view.findViewById(R.id.seekBarBrStat)
        rvColors = view.findViewById(R.id.rvColorsStat)
    }

    private fun sendMessageToServer() {
        //порядок параметров важен: скетч читает их по номеру
        EspApi.get(baseUrl, keyWord,
            listOf("color" to color, "brightness" to brightness, "position" to nameOfEffect),
            onError = { toastNoConnection() },
            onSuccess = { if (isAdded) model.updateNameOfEffect(it) })
    }
}