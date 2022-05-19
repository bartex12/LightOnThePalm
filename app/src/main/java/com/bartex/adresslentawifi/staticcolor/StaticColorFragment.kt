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
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bartex.adresslentawifi.R
import com.bartex.adresslentawifi.Server
import com.bartex.adresslentawifi.adapter.StaticColorAdapter
import com.bartex.adresslentawifi.constants.Constants
import com.bartex.adresslentawifi.constants.Constants.BASE_URL
import com.bartex.adresslentawifi.constants.Constants.BRIGHTNESS
import com.bartex.adresslentawifi.constants.Constants.COLOR
import com.bartex.adresslentawifi.constants.Constants.TAG
import com.bartex.adresslentawifi.entity.ButtonColor

class StaticColorFragment: Fragment(), Server.MessageListener {

    private lateinit var baseUrl:String
    private lateinit var adapter: StaticColorAdapter
    private val viewModel: StaticColorsViewModel by lazy {
        ViewModelProvider(this).get(StaticColorsViewModel::class.java)
    }
    private var color:String? = "0x00FFFF"
    var brightness:String? = "5"
    private lateinit var pref: SharedPreferences
    private lateinit var seekBarStatic: SeekBar
    private lateinit var rvColors: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_static_colors, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.getString(BASE_URL)?. let{baseUrl = it }
        pref = requireActivity().getSharedPreferences("MyPref", AppCompatActivity.MODE_PRIVATE)

        initViews(view)
        getColorAndBrightness()
        initAdapter()
        initSeekBar()

        viewModel.loadStaticColors()
        viewModel.getStaticColors().observe(this, {
            renderData(it)
        })
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
                brightness = setBrightness()
                color = item.color.replace("#", "0x")

                Server(baseUrl, getString(R.string.postStaticColors,color, brightness),
                    this@StaticColorFragment ).start()
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

                    Server(baseUrl, getString(R.string.postStaticColors,color, brightness),
                        this@StaticColorFragment).start()
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

        Server(baseUrl, getString(R.string.postStaticColors,color, brightness), this).start()
    }

    private fun initViews(view: View) {
        seekBarStatic = view.findViewById(R.id.seekBarStatic)
        rvColors = view.findViewById(R.id.rvColors)
    }

    override fun onMessage(message: String) {
       requireActivity(). runOnUiThread {
            Log.d(TAG, "StaticColorFragment onMessage message= $message")
        }
    }
}