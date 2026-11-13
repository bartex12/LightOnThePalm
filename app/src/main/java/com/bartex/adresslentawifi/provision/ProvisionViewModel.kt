package com.bartex.adresslentawifi.provision

import android.app.Application
import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.bartex.adresslentawifi.EspApi
import com.bartex.adresslentawifi.EspLink
import com.bartex.adresslentawifi.R
import com.bartex.adresslentawifi.constants.Constants
import com.bartex.adresslentawifi.constants.Constants.TAG

/**
 * Привязка ленты к сети WiFi, когда лента работает как точка доступа NodeMCU (Android 10+).
 *
 * Шаги:
 * 1. CONNECTING - подключаем телефон к NodeMCU через EspLink (системный диалог);
 * 2. SENDING    - передаём ленте имя и пароль сети (команда /home);
 * 3. SWITCHING  - ждём, пока лента выключит точку доступа (если ответ на /home не дошёл);
 * 4. SEARCHING  - телефон сам возвращается в обычную сеть WiFi, ищем в ней ленту
 *                 через NSD и проверяем, что она отвечает.
 *
 * Скетч отвечает на /home ДО попытки подключения и результат не сообщает, поэтому успех
 * определяем по факту: лента нашлась в сети и ответила на запрос /esp.
 *
 * Все поля меняются только в главном потоке.
 */
@RequiresApi(Build.VERSION_CODES.Q)
class ProvisionViewModel(app: Application) : AndroidViewModel(app) {

    enum class Step { FORM, CONNECTING, SENDING, SWITCHING, SEARCHING, SUCCESS, ERROR }

    enum class Failure {
        AP_NOT_FOUND,        //не удалось подключиться к NodeMCU
        NOT_DELIVERED,       //лента не приняла команду
        NOT_FOUND_IN_NETWORK //лента не появилась в сети за отведённое время
    }

    data class UiState(
        val step: Step = Step.FORM,
        val failure: Failure? = null,
        val ssid: String = "",     //имя сети, к которой привязываем
        val espIp: String = "",    //адрес ленты в сети (при успехе)
        val espSsid: String = ""   //имя сети, которое сообщила сама лента (при успехе)
    )

    private val stateLiveData = MutableLiveData(UiState())
    val state: LiveData<UiState> get() = stateLiveData

    private val context: Context get() = getApplication<Application>()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val nsdManager = app.getSystemService(Context.NSD_SERVICE) as NsdManager

    private var ssid = ""
    private var password = ""
    private var discoveryListener: NsdManager.DiscoveryListener? = null

    private val step: Step get() = stateLiveData.value?.step ?: Step.FORM

    //следим за подключением к точке доступа ленты
    private val linkObserver = Observer<EspLink.State> { onLinkState(it) }

    private fun onLinkState(linkState: EspLink.State) {
        if (step == Step.CONNECTING) {
            if (linkState == EspLink.State.CONNECTED) {
                send()
            } else if (linkState == EspLink.State.FAILED || linkState == EspLink.State.LOST) {
                fail(Failure.AP_NOT_FOUND)
            }
        } else if (step == Step.SWITCHING && linkState == EspLink.State.LOST) {
            //лента выключила точку доступа - значит, команду приняла и пошла подключаться
            startSearch()
        }
    }

    /** Запустить привязку ленты к сети [name] с паролем [pass]. */
    fun start(name: String, pass: String) {
        if (step != Step.FORM && step != Step.ERROR) return //уже идёт или завершена
        ssid = name
        password = pass
        if (EspLink.network != null) {
            send() //телефон уже подключен к точке доступа ленты
        } else {
            setStep(Step.CONNECTING)
            EspLink.connect(context,
                context.getString(R.string.localNetworkName),
                context.getString(R.string.localNetworkPass))
        }
    }

    //шаг 2: передаём ленте имя и пароль
    private fun send() {
        setStep(Step.SENDING)
        //порядок параметров важен: скетч читает их по номеру
        EspApi.get(context.getString(R.string.urlHotpoint), context.getString(R.string._home),
            listOf("name" to ssid, "password" to password),
            onError = {
                if (step == Step.SENDING) {
                    //Ответ мог не дойти, потому что лента уже выключила точку доступа -
                    //тогда команда выполнена. Если точка доступа пропала - идём дальше,
                    //иначе ждём её пропадания; не дождались - команда не доставлена.
                    if (EspLink.network == null) {
                        startSearch()
                    } else {
                        setStep(Step.SWITCHING)
                        mainHandler.postDelayed(switchTimeout, SWITCH_TIMEOUT_MS)
                    }
                }
            },
            onSuccess = {
                if (step == Step.SENDING) {
                    //ответ получен - отключаемся, телефон возвращается в обычную сеть WiFi
                    EspLink.release(context)
                    startSearch()
                }
            })
    }

    private val switchTimeout = Runnable {
        if (step == Step.SWITCHING) fail(Failure.NOT_DELIVERED)
    }

    //шаг 4: ищем ленту в сети, в которую вернулся телефон
    private fun startSearch() {
        mainHandler.removeCallbacks(switchTimeout)
        setStep(Step.SEARCHING)
        mainHandler.postDelayed(searchTimeout, SEARCH_TIMEOUT_MS)
        restartDiscovery.run()
    }

    private val searchTimeout = Runnable {
        if (step == Step.SEARCHING) fail(Failure.NOT_FOUND_IN_NETWORK)
    }

    //Поиск перезапускаем каждые несколько секунд: телефон в это время меняет сеть WiFi,
    //и поиск, начатый до переключения, на некоторых версиях Android ничего не находит.
    private val restartDiscovery = object : Runnable {
        override fun run() {
            if (step != Step.SEARCHING) return
            startDiscovery()
            mainHandler.postDelayed(this, DISCOVERY_RESTART_MS)
        }
    }

    private fun startDiscovery() {
        stopDiscovery()
        val listener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) {}
            override fun onDiscoveryStopped(serviceType: String) {}
            override fun onServiceLost(serviceInfo: NsdServiceInfo) {}
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {}

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.d(TAG, "Provision: поиск не запустился, код $errorCode")
                val failed = this
                mainHandler.post { if (discoveryListener === failed) discoveryListener = null }
            }

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                if (serviceInfo.serviceName.contains(ESP_SERVICE_NAME)) {
                    Log.d(TAG, "Provision: найден сервис ${serviceInfo.serviceName}")
                    mainHandler.post { resolve(serviceInfo) }
                }
            }
        }
        discoveryListener = listener
        try {
            nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, listener)
        } catch (e: RuntimeException) {
            Log.d(TAG, "Provision: discoverServices ошибка ${e.message}")
            discoveryListener = null
        }
    }

    private fun stopDiscovery() {
        val listener = discoveryListener ?: return
        discoveryListener = null
        try {
            nsdManager.stopServiceDiscovery(listener)
        } catch (e: RuntimeException) {
            //слушатель не был зарегистрирован - поиск не запускался
        }
    }

    @Suppress("DEPRECATION")
    private fun resolve(serviceInfo: NsdServiceInfo) {
        if (step != Step.SEARCHING) return
        try {
            nsdManager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                override fun onResolveFailed(info: NsdServiceInfo, errorCode: Int) {
                    Log.d(TAG, "Provision: адрес не получен, код $errorCode")
                }

                override fun onServiceResolved(info: NsdServiceInfo) {
                    val ip = info.host?.hostAddress ?: return
                    mainHandler.post { checkEsp(ip) }
                }
            })
        } catch (e: RuntimeException) {
            Log.d(TAG, "Provision: resolveService ошибка ${e.message}")
        }
    }

    //лента найдена - проверяем, что она действительно отвечает из этой сети
    private fun checkEsp(ip: String) {
        if (step != Step.SEARCHING) return
        Log.d(TAG, "Provision: проверяем ленту по адресу $ip")
        EspApi.get("http://$ip", context.getString(R.string._esp),
            onSuccess = { espSsid ->
                //если лента отвечает именем NodeMCU - это старая запись о точке доступа
                if (step == Step.SEARCHING && espSsid != Constants.NODE_MCU) {
                    succeed(ip, espSsid)
                }
            })
    }

    private fun succeed(ip: String, espSsid: String) {
        stopAll()
        //запоминаем адрес ленты - его читает главный экран
        context.getSharedPreferences("MyPref", Context.MODE_PRIVATE)
            .edit().putString(Constants.IP, ip).apply()
        stateLiveData.value = UiState(Step.SUCCESS, null, ssid, ip, espSsid)
    }

    private fun fail(failure: Failure) {
        Log.d(TAG, "Provision: ошибка $failure")
        stopAll()
        EspLink.release(context) //если ещё подключены к точке доступа - отключаемся
        EspLink.clearResult()
        stateLiveData.value = UiState(Step.ERROR, failure, ssid)
    }

    private fun setStep(newStep: Step) {
        stateLiveData.value = UiState(newStep, null, ssid)
    }

    private fun stopAll() {
        mainHandler.removeCallbacks(switchTimeout)
        mainHandler.removeCallbacks(searchTimeout)
        mainHandler.removeCallbacks(restartDiscovery)
        stopDiscovery()
    }

    init {
        //в самом конце, когда все поля класса уже созданы
        EspLink.state.observeForever(linkObserver)
    }

    override fun onCleared() {
        //пользователь ушёл с экрана - прекращаем всё начатое
        EspLink.state.removeObserver(linkObserver)
        stopAll()
        if (step == Step.CONNECTING || step == Step.SENDING || step == Step.SWITCHING) {
            EspLink.release(context)
        }
        EspLink.clearResult()
    }

    companion object {
        private const val SERVICE_TYPE = "_http._tcp"
        private const val ESP_SERVICE_NAME = "esp8266"

        //сколько ждём пропадания точки доступа, если ответ на /home не дошёл
        private const val SWITCH_TIMEOUT_MS = 15_000L
        //лента подключается к сети до 15 с, плюс возврат телефона в сеть и поиск
        private const val SEARCH_TIMEOUT_MS = 60_000L
        private const val DISCOVERY_RESTART_MS = 8_000L
    }
}
