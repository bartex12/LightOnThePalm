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
                   1-> post("cyan")
                   2-> post("magenta")
                   3-> post("blue")
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
                        val status = array?.get(1)
                    }
                }
            }catch (i: IOException){
            }
        }.start()
    }
}