package com.bartex.adresslentawifi.staticcolor

import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.bartex.adresslentawifi.adapter.StaticColorAdapter
import com.bartex.adresslentawifi.databinding.ActivityStaticColorsRvBinding
import com.bartex.adresslentawifi.entity.ButtonColor
import com.bartex.adresslentawifi.main.MainActivity
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

class StaticColorsRvActivity : AppCompatActivity() {

    companion object{
        const val TAG = "33333"
    }

    private lateinit var extraIp:String
    private lateinit var binding: ActivityStaticColorsRvBinding
    private lateinit var adapter: StaticColorAdapter
    private val viewModel: StaticColorsRvViewModel by lazy {
        ViewModelProvider(this).get(StaticColorsRvViewModel::class.java)
    }
    private lateinit var request: Request
    private val client = OkHttpClient()
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
            //Log.d(TAG, "onCreate: $it")
        })
    }

    private fun getColorAndBrightness() {
        color = pref.getString("color", "0x00FFFF")
        brightness = pref.getString("brightness", "5")
        binding.seekBarStatic.progress = Integer.parseInt(brightness!!)
        post("static?color=$color&brightness=$brightness")
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
                    post("static?color=$color&brightness=$brightness")
                }
            }
        )
    }
    private fun setBrightness(): String {
        var br = binding.seekBarStatic.progress
        if (br < 5) {
            br = 5;binding.seekBarStatic.progress = 5
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
                //Log.d(TAG, "getOnClickListener: ${item.color}   ${item.text}")
                brightness = setBrightness()
                when(item.text.toInt()){
                   1->  color="0xFF0000"   //Red
                   2->  color="0xFF4500"   //OrangeRed
                   3->  color="0xFFA500"   //Orange
                   4->  color="0xFFD700"   //Gold
                   5->  color="0xFFFF00"   //Yellow
                   6->  color="0x7FFF00"   //Chartreuse
                   7->  color="0x00FF00"   //Lime
                   8->  color="0x00FF7F"   //SpringGreen
                   9->  color="0x00FFFF"   //Cyan
                   10-> color="0x00BFFF"   //DeepSkyBlue
                   11-> color="0x1E90FF"   //DodgerBlue
                   12-> color="0x0000CD"   //MediumBlue
                   13-> color="0x0000FF"   //Blue
                   14-> color="0xEE82EE"   //Violet
                   15-> color="0xFF00FF"   //Magenta
                   16-> color="0xFF1493"   //DeepPink
                   17-> color="0xFFFFFF"   //White
                   18-> color="0x000000"  //Black
                }
                post("static?color=$color&brightness=$brightness")
            }
        }

    private fun post(post: String){
        Thread{
            request = Request.Builder().url("$extraIp/$post").build()
            try {
                val response = client.newCall(request).execute()
                Log.d(TAG, "StaticColorsRvActivity request = $request")
                Log.d(TAG, "StaticColorsRvActivity response = $response")
                if(response.isSuccessful){
                    val resultText = response.body()?.string()
                    Log.d(MainActivity.TAG, "StaticColorsRvActivity resultText = $resultText")
                    val array = resultText?.split(",")
                    runOnUiThread {
                        val text = array?.get(0)
                    }
                }
            }catch (i: IOException){
            }
        }.start()
    }
}