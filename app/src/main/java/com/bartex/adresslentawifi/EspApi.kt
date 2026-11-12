package com.bartex.adresslentawifi

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.bartex.adresslentawifi.constants.Constants.TAG
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Единая точка отправки HTTP-запросов на ESP8266.
 *
 * - один OkHttpClient на всё приложение (общий пул потоков и соединений);
 * - параметры запроса кодируются (пробелы, &, #, кириллица в имени сети/пароле/эффекта);
 * - порядок параметров сохраняется: скетч читает их по номеру — server.arg(0), server.arg(1)...;
 * - onSuccess и onError всегда вызываются в главном потоке;
 * - в лог попадает только путь запроса, без параметров (в них бывает пароль WiFi).
 */
object EspApi {

    //эффекты в скетче блокируют loop() до секунды и больше, поэтому таймауты с запасом
    private const val CONNECT_TIMEOUT_SEC = 5L
    private const val READ_TIMEOUT_SEC = 7L

    private val mainHandler = Handler(Looper.getMainLooper())

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(CONNECT_TIMEOUT_SEC, TimeUnit.SECONDS)
        .readTimeout(READ_TIMEOUT_SEC, TimeUnit.SECONDS)
        .writeTimeout(READ_TIMEOUT_SEC, TimeUnit.SECONDS)
        .retryOnConnectionFailure(false) //команды не повторяем автоматически
        .build()

    /**
     * GET http://<baseUrl>/<path>?<params>
     *
     * @param baseUrl   адрес вида "http://192.168.1.1"
     * @param path      путь без слэша: "static", "home", "esp"...
     * @param params    пары имя-значение в том порядке, в котором их ждёт скетч
     * @param onError   вызывается в главном потоке, если запрос не удался
     * @param onSuccess вызывается в главном потоке с текстом ответа ESP
     */
    fun get(
        baseUrl: String,
        path: String,
        params: List<Pair<String, String?>> = emptyList(),
        onError: (message: String) -> Unit = {},
        onSuccess: (body: String) -> Unit = {}
    ) {
        val base = HttpUrl.parse(baseUrl.trim())
        if (base == null) {
            Log.d(TAG, "EspApi неверный адрес: $baseUrl")
            mainHandler.post { onError("Неверный адрес: $baseUrl") }
            return
        }
        val urlBuilder = base.newBuilder().addPathSegment(path)
        for ((name, value) in params) {
            urlBuilder.addQueryParameter(name, value ?: "")
        }
        val request = Request.Builder().url(urlBuilder.build()).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.d(TAG, "EspApi /$path ошибка: ${e.message}")
                mainHandler.post { onError(e.message ?: "нет ответа") }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (it.isSuccessful) {
                        val body = try {
                            it.body()?.string() ?: ""
                        } catch (e: IOException) {
                            Log.d(TAG, "EspApi /$path ошибка чтения ответа: ${e.message}")
                            mainHandler.post { onError(e.message ?: "ошибка чтения ответа") }
                            return
                        }
                        Log.d(TAG, "EspApi /$path ответ получен")
                        mainHandler.post { onSuccess(body) }
                    } else {
                        Log.d(TAG, "EspApi /$path код ответа ${it.code()}")
                        mainHandler.post { onError("код ответа ${it.code()}") }
                    }
                }
            }
        })
    }
}
