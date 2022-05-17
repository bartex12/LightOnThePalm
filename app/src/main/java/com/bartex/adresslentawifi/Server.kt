package com.bartex.adresslentawifi

import android.util.Log
import com.bartex.adresslentawifi.main.MainActivity
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

class Server(
    baseUrl:String,
    post: String,
    private val listener: MessageListener
    ):Thread() {

    private var request: Request = Request.Builder().url("$baseUrl/$post").build()
    private val client = OkHttpClient()

    companion object{
        const val TAG = "33333"
    }

    override fun run() {
        try {
            val response = client.newCall(request).execute()
            Log.d(TAG, "Server request = $request")
            Log.d(TAG, "Server response = $response")
            if(response.isSuccessful){
                val resultText = response.body()?.string()
                Log.d(MainActivity.TAG, "Server resultText = $resultText")
                val array = resultText?.split(",")
                array?.get(0)?. let{listener.onMessage(it)}
            }
        }catch (i: IOException){
        }
    }
    interface MessageListener{
        fun onMessage(message:String)
    }

}