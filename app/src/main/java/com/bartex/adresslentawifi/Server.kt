package com.bartex.adresslentawifi

import android.util.Log
import com.bartex.adresslentawifi.constants.Constants
import com.bartex.adresslentawifi.constants.Constants.TAG
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


    override fun run() {
        try {
            val response = client.newCall(request).execute()
            //Log.d(TAG, "Server request = $request")
            //Log.d(TAG, "Server response = $response")
            if(response.isSuccessful){
                val resultText = response.body()?.string()
                //Log.d(TAG, "Server resultText = $resultText")
                resultText?. let{listener.onMessage(it)}
            }
        }catch (i: IOException){
            Log.d(TAG, "Server IOException = ${i.message}")
        }
    }
    interface MessageListener{
        fun onMessage(message:String)
    }

}