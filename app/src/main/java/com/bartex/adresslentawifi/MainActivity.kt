package com.bartex.adresslentawifi

import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.bartex.adresslentawifi.databinding.ActivityMainBinding
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
//приложение для управления адресной лентой с несколькими эффектами
// библиотека ESP8266WiFi + режим STA
//приложение андроид F:\AndroidStudioProjects\Android11_SelfLearning\EspWiFiNeco ветка adressLent
//скетч для esp F:\Arduino\004 АРДУИНО с диска E\010 Адресная лента\_003Esp_WiFi_6Knobs_sketch_may08b
//варианты - бег строка , pride, flame, синий, циан, маджента, радуга

class MainActivity : AppCompatActivity() {
    private lateinit var request: Request
    private lateinit var binding: ActivityMainBinding
    private lateinit var pref: SharedPreferences
    private val client = OkHttpClient()

    companion object{
        const val TAG = "33333"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        pref = getSharedPreferences("MyPref", MODE_PRIVATE)
        onClickSaveIp()
        getIp()
        binding.apply {
            bLed11.setOnClickListener(onClickListener())
            bLed12.setOnClickListener(onClickListener())
            bLed21.setOnClickListener(onClickListener())
            bLed22.setOnClickListener(onClickListener())
            bLed31.setOnClickListener(onClickListener())
            bLed32.setOnClickListener(onClickListener())
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(item.itemId == R.id.sync) post("off")
        return true
    }

    private fun onClickListener(): View.OnClickListener{
        return View.OnClickListener {
            when(it.id){
                R.id.bLed11 -> { post("pride") }
                R.id.bLed21 -> { post("flame") }
                R.id.bLed31 -> { post("blue") }
                R.id.bLed12 -> { post("rainbow") }
                R.id.bLed22 -> { post("cyan") }
                R.id.bLed32 -> { post("magenta") }
            }
        }
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
                        val status = array?.get(1)
                        binding.tvStatus.text =String.format("%s%s",text,status)
                    }
                }
            }catch (i: IOException){
            }
        }.start()
    }
}