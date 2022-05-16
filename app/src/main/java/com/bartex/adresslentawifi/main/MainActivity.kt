package com.bartex.adresslentawifi.main

import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.bartex.adresslentawifi.entity.ListOfEffects
import com.bartex.adresslentawifi.R
import com.bartex.adresslentawifi.adapter.RvAdapter
import com.bartex.adresslentawifi.staticcolor.StaticColorsRvActivity
import com.bartex.adresslentawifi.databinding.ActivityMainBinding
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
//управление адресной лентой через NodeMCU с WiFi в режиме STA
//две библиотеки, + режим STA     статич цвета - через параметры достаём цвет
//в loop исполняем несколько вариантов , которые определены в setup в функции server.on()
// код Андроид F:\AndroidStudioProjects\Android15 Bluetooth\AdressLentaWiFi  ветка listMVVM
// скетч F:\Arduino\004 АРДУИНО с диска E\010 Адресная лента\_005EspWiFi_2lib_STA_sketch_may14a
//варианты - бег строка , pride, flame, статич цвета

class MainActivity : AppCompatActivity() {
    private lateinit var request: Request
    private lateinit var binding: ActivityMainBinding
    private lateinit var pref: SharedPreferences
    private val client = OkHttpClient()
    private val viewModel: MainViewModel by lazy {
        ViewModelProvider(this).get(MainViewModel::class.java)
    }
    private var rvAdapter: RvAdapter? = null

    companion object{
        const val TAG = "33333"
        const val IP = "IP"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        pref = getSharedPreferences("MyPref", MODE_PRIVATE)
        onClickSaveIp()
        getIp()

        viewModel.loadData()
        viewModel.getListOfEffects().observe(this, Observer{
            renderData(it)
        })
        initAdapter()
    }

    override fun onResume() {
        super.onResume()
        post("brightness") //яркость=50 - это установлено в скетче ардуино
        //Log.d(TAG, "MainActivity onResume")
    }

    private fun initAdapter()  =
        with(binding){
            rvEffects.layoutManager = LinearLayoutManager(this@MainActivity)
            rvAdapter =  RvAdapter(getOnClickListener())
            rvEffects.adapter = rvAdapter
        }



    private fun getOnClickListener(): RvAdapter.OnItemClickListener =
        object : RvAdapter.OnItemClickListener{
            override fun onClick(item: ListOfEffects) {
                when(item.name){
                    getString(R.string.staticColors) -> {
                        val intent = Intent(this@MainActivity, StaticColorsRvActivity::class.java)
                        intent.putExtra(IP, "http://${binding.edIp.text}")
                        startActivity(intent)
                    }
                    getString(R.string.flame) -> post("flame")
                    getString(R.string.pride) -> post("pride")
                    getString(R.string.rainbow) -> post("rainbow")
                }
            }

        }
    private fun renderData(data: List<ListOfEffects>?) {
        data?. let{ rvAdapter?.listOfEffects = it}
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(item.itemId == R.id.sync) post("off")
        return true
    }

    private fun getIp() = with(binding){
        val ip = pref.getString("ip", "")
        if(ip != null){
            if(ip.isNotEmpty()) edIp.setText(ip)
        }
    }

    private fun onClickSaveIp() = with(binding){
        bSave.setOnClickListener {
            if(edIp.text.isNotEmpty())saveIp(edIp.text.toString())
        }
    }

    private fun saveIp(ip: String){
        val editor = pref.edit()
        editor.putString("ip", ip)
        editor.apply()
    }

    private fun post(post: String){
        Thread{
            request = Request.Builder().url("http://${binding.edIp.text}/$post").build()
            try {
                val response = client.newCall(request).execute()
                Log.d(TAG, "MainActivity request = $request")
                Log.d(TAG, "MainActivity response = $response")
                if(response.isSuccessful){
                    val resultText = response.body()?.string()
                    Log.d(TAG, "MainActivity resultText = $resultText")
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