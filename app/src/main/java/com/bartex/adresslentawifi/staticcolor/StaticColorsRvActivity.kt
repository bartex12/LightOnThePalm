package com.bartex.adresslentawifi.staticcolor

import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.bartex.adresslentawifi.R
import com.bartex.adresslentawifi.Server
import com.bartex.adresslentawifi.adapter.StaticColorAdapter
import com.bartex.adresslentawifi.databinding.ActivityStaticColorsRvBinding
import com.bartex.adresslentawifi.entity.ButtonColor
import com.bartex.adresslentawifi.main.MainActivity
import okhttp3.OkHttpClient
import okhttp3.Request

class StaticColorsRvActivity : AppCompatActivity(), Server.MessageListener {

    companion object{
        const val TAG = "33333"
    }

    private lateinit var extraIp:String
    private lateinit var binding: ActivityStaticColorsRvBinding
    private lateinit var adapter: StaticColorAdapter
    private val viewModel: StaticColorsRvViewModel by lazy {
        ViewModelProvider(this).get(StaticColorsRvViewModel::class.java)
    }
    private var color:String? = "0x00FFFF"
    var brightness:String? = "5"
    private lateinit var pref: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStaticColorsRvBinding.inflate(layoutInflater)
        setContentView(binding.root)
        intent.getStringExtra(MainActivity.IP)?. let{extraIp = it }
        pref = getSharedPreferences("MyPref", MODE_PRIVATE)
        getColorAndBrightness()

        initAdapter()
        initSeekBar()

        viewModel.loadStaticColors()
        viewModel.getStaticColors().observe(this, Observer { 
            renderData(it)
        })
    }

    private fun getColorAndBrightness() {
        color = pref.getString("color", "0x00FFFF")
        brightness = pref.getString("brightness", "5")
        binding.seekBarStatic.progress = Integer.parseInt(brightness!!)

        Server(extraIp, getString(R.string.postStaticColors,color, brightness), this).start()
    }

    override fun onPause() {
        super.onPause()
        val editor = pref.edit()
        editor.putString("color", color)
        editor.putString("brightness", brightness)
        editor.apply()
    }

    private fun initSeekBar() {
        binding.seekBarStatic.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener{
                override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {}
                override fun onStartTrackingTouch(p0: SeekBar?) {}
                override fun onStopTrackingTouch(p0: SeekBar?) {
                    brightness = setBrightness()

                    Server(extraIp, getString(R.string.postStaticColors,color, brightness),
                        this@StaticColorsRvActivity).start()
                }
            }
        )
    }
    private fun setBrightness(): String {
        var br = binding.seekBarStatic.progress
        if (br < 5) {
            br = 5
            binding.seekBarStatic.progress = 5
        }
        return br.toString()
    }

    private fun renderData(data: List<ButtonColor>?) {
        data?. let{ adapter.listOfButtonColor = it}
    }

    private fun initAdapter() {
        binding.rvColors.layoutManager = GridLayoutManager(this, 6)
        adapter = StaticColorAdapter(getOnClickListener())
        binding.rvColors.adapter = adapter
    }

    private fun getOnClickListener(): StaticColorAdapter.OnItemClickListener =
        object : StaticColorAdapter.OnItemClickListener{
            override fun onClick(item: ButtonColor) {
                brightness = setBrightness()
                color = item.color.replace("#", "0x")

                Server(extraIp, getString(R.string.postStaticColors,color, brightness),
                    this@StaticColorsRvActivity ).start()
            }
        }

    override fun onMessage(message: String) {
        runOnUiThread {
            Log.d(TAG, "StaticColorsRvActivity onMessage message= $message")
        }
    }
}