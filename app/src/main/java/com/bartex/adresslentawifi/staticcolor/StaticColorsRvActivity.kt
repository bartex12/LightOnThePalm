package com.bartex.adresslentawifi.staticcolor

import android.os.Bundle
import android.util.Log
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStaticColorsRvBinding.inflate(layoutInflater)
        setContentView(binding.root)
        intent.getStringExtra(MainActivity.IP)?. let{extraIp = it }

        initAdapter()

        viewModel.loadStaticColors()
        viewModel.getStaticColors().observe(this, Observer { 
            renderData(it)
            Log.d(TAG, "onCreate: $it")
        })
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
              binding.tvCodeColor.text =  item.color
                Log.d(TAG, "getOnClickListener: ${item.color}   ${item.text}")
                when(item.text.toInt()){
                   1-> post("static?color=0xFF0000") //Red
                   2-> post("static?color=0xFF4500") //OrangeRed
                   3-> post("static?color=0xFFA500") //Orange
                   4-> post("static?color=0xFFD700") //Gold
                   5-> post("static?color=0xFFFF00") //Yellow
                   6-> post("static?color=0x7FFF00") //Chartreuse
                   7-> post("static?color=0x00FF00") //Lime
                   8-> post("static?color=0x00FF7F") //SpringGreen
                   9-> post("static?color=0x00FFFF") //Cyan
                   10-> post("static?color=0x00BFFF") //DeepSkyBlue
                   11-> post("static?color=0x1E90FF") //DodgerBlue
                   12-> post("static?color=0x0000CD") //MediumBlue
                   13-> post("static?color=0x0000FF") //Blue
                   14-> post("static?color=0xEE82EE") //Violet
                   15-> post("static?color=0xFF00FF") //Magenta
                   16-> post("static?color=0xFF1493") //DeepPink
                   17-> post("static?color=0xFFFFFF") //White
                    18-> post("static?color=0x000000") //Black
                }
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