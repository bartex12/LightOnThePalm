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
                   1-> post("static?color=0xF44336")
                   2-> post("static?color=0xE91E63")
                   3-> post("static?color=0x9C27B0")
                   4-> post("static?color=0x673AB7")
                   5-> post("static?color=0x3949AB")
                   6-> post("static?color=0x1E88E5")
                   7-> post("static?color=0x039BE5")
                   8-> post("static?color=0x00ACC1")
                   9-> post("static?color=0x00897B")
                   10-> post("static?color=0x43A047")
                   11-> post("static?color=0x7CB342")
                   12-> post("static?color=0xC0CA33")
                   13-> post("static?color=0xFDD835")
                   14-> post("static?color=0xFFB300")
                   15-> post("static?color=0xFB8C00")
                   16-> post("static?color=0xF4511E")
                   17-> post("static?color=0xFFFFFF")
                    18-> post("static?color=0x000000")
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