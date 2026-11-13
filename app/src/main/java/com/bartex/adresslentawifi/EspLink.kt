package com.bartex.adresslentawifi

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiNetworkSpecifier
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.bartex.adresslentawifi.constants.Constants.TAG

/**
 * Прямое подключение телефона к точке доступа ленты (NodeMCU) из приложения,
 * без перехода в настройки WiFi и без разрешения на геолокацию. Android 10+.
 *
 * Как это работает:
 * - connect() показывает системный диалог с найденной сетью, пользователь подтверждает;
 * - соединение принадлежит только нашему приложению и живёт, пока не вызван release()
 *   и пока жив процесс приложения;
 * - после release() телефон сам возвращается в обычную сеть WiFi;
 * - у такой сети нет интернета, поэтому запросы к ленте надо отправлять именно через неё -
 *   это делает EspApi, когда видит, что [network] не null.
 *
 * Вызывать connect() можно только когда приложение на экране (система отклоняет запросы из фона).
 */
object EspLink {

    enum class State {
        IDLE,        //соединения нет и его не запрашивали
        CONNECTING,  //показан системный диалог / идёт подключение
        CONNECTED,   //телефон подключен к точке доступа ленты
        FAILED,      //не нашли сеть, неверный пароль или пользователь отказался
        LOST         //соединение было и пропало (лента выключена или ушла в другую сеть)
    }

    /** true, если на этом телефоне можно подключаться из приложения (Android 10 и новее) */
    val isSupported: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    private val stateLiveData = MutableLiveData(State.IDLE)
    val state: LiveData<State> get() = stateLiveData

    /** Сеть точки доступа ленты, пока соединение установлено, иначе null */
    @Volatile
    var network: Network? = null
        private set

    private var callback: ConnectivityManager.NetworkCallback? = null

    /**
     * Запросить подключение к точке доступа с именем [ssid] и паролем [password].
     * Результат приходит в [state]. Повторный вызов при активном запросе ничего не делает.
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    @Synchronized
    fun connect(context: Context, ssid: String, password: String) {
        if (callback != null) {
            Log.d(TAG, "EspLink connect: запрос уже активен")
            return
        }
        val cm = context.applicationContext
            .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val specifier = WifiNetworkSpecifier.Builder()
            .setSsid(ssid)
            .setWpa2Passphrase(password)
            .build()
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) //у ленты интернета нет
            .setNetworkSpecifier(specifier)
            .build()

        val cb = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(net: Network) {
                Log.d(TAG, "EspLink onAvailable")
                network = net
                stateLiveData.postValue(State.CONNECTED)
            }

            override fun onLost(net: Network) {
                Log.d(TAG, "EspLink onLost")
                //снимаем запрос, чтобы телефон вернулся в обычную сеть WiFi
                finish(cm, this, State.LOST)
            }

            override fun onUnavailable() {
                Log.d(TAG, "EspLink onUnavailable")
                finish(cm, this, State.FAILED)
            }
        }
        callback = cb
        stateLiveData.postValue(State.CONNECTING)
        try {
            cm.requestNetwork(request, cb)
        } catch (e: RuntimeException) {
            //SecurityException (нет разрешения) или превышен лимит запросов
            Log.d(TAG, "EspLink requestNetwork ошибка: ${e.message}")
            callback = null
            stateLiveData.postValue(State.FAILED)
        }
    }

    /** Отключиться от точки доступа ленты. Телефон вернётся в обычную сеть WiFi. */
    @Synchronized
    fun release(context: Context) {
        val cb = callback ?: return
        val cm = context.applicationContext
            .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        finish(cm, cb, State.IDLE)
    }

    @Synchronized
    private fun finish(
        cm: ConnectivityManager,
        cb: ConnectivityManager.NetworkCallback,
        newState: State
    ) {
        if (callback !== cb) return //это колбэк от старого, уже снятого запроса
        try {
            cm.unregisterNetworkCallback(cb)
        } catch (e: RuntimeException) {
            Log.d(TAG, "EspLink unregister ошибка: ${e.message}")
        }
        callback = null
        network = null
        stateLiveData.postValue(newState)
    }
}
