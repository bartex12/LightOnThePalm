package com.bartex.adresslentawifi.main

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.Group
import androidx.core.view.GravityCompat
import androidx.core.view.MenuHost
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import com.bartex.adresslentawifi.EspApi
import com.bartex.adresslentawifi.EspLink
import com.bartex.adresslentawifi.LocalMode
import com.bartex.adresslentawifi.R
import com.bartex.adresslentawifi.isVpnActive
import com.bartex.adresslentawifi.showMessage
import com.bartex.adresslentawifi.SharedViewModel
import com.bartex.adresslentawifi.constants.Constants
import com.bartex.adresslentawifi.constants.Constants.TAG
import com.bartex.adresslentawifi.databinding.ActivityMainBinding
import com.google.android.material.navigation.NavigationView

//управление адресной лентой через NodeMCU с WiFi в режиме STA и AT
//две библиотеки, + режим STA через параметры достаём цвет, яркость, скорость, количество точек
//в loop исполняем несколько вариантов , которые определены в setup в функции server.on()
// код Андроид   F:\AndroidStudioProjects\Android14 Wifi\AdressLentaWiFi   ветка 15_WiFi_DNS-SD_in_List
//скетч F:\Arduino\004 АРДУИНО с диска E\010 Адресная лента\_013_AP_STA_MQTT_NSD_EEPROM_ALL_sketch_170424_ino
//варианты - бег строка , pride, flame, статич цвета,конфетти,радуги
//Вместо обращения  на MQTT сервер для передачи ip адреса используется NsdManager, который выполняет
//поиск службы, зарегистрированной ESP8266, и получения IP-адреса ESP8266, который ей был присвоен в сети.
//Таким образом, ESP8266 будет регистрировать службу с использованием NSD (Network Service Discovery),
// а приложение на Android будет обнаруживать эту службу и получать её IP-адрес
//при этом делается 1 обращение в 5 сек  - в loop
//добавлено чтение и запись в EEPROM имени и пароля сети через функции set_String(0,1,ssidHome) и set_String(31,32,passHome)
//https://russianblogs.com/article/26471580690/  для создания подключения к домашней сети
//включено создание точки доступа по команде с телефона, а всего 7 сценариев работы с WiFi

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener{

    private lateinit var binding:ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var  appBarConfiguration:AppBarConfiguration

    private val model: SharedViewModel by lazy {
        ViewModelProvider(this)[SharedViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "@@@MainActivity onCreate")

        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        //находим NavController
        navController = Navigation.findNavController(this, R.id.nav_host_fragment)
        //пока не понятно как через binding
        val  toolbar:Toolbar = findViewById(R.id.toolbar)
        //задаём шрифт и размер для title тулбара
        toolbar.setTitleTextAppearance(this, R.style.ToolbarTitleStyle)
        //поддержка экшенбара
        setSupportActionBar(toolbar)
        //отключаем показ заголовка - если будет свой - у нас своего textView нет
        //supportActionBar?.setDisplayShowTitleEnabled(false)

        appBarConfiguration = AppBarConfiguration.Builder(navController.graph)
            //Отображать кнопку навигации как гамбургер , когда она не отображается как кнопка вверх
            .setOpenableLayout(binding.drawerLayout)
            .build()
        //обработка событий экшенбара -гамбургер, кнопка вверх
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration)
        //слушатель меню шторки -для обработки пунктов шторки
        binding.navView.setNavigationItemSelectedListener(this)

        //следим за прямым подключением к ленте (точка доступа NodeMCU)
        EspLink.state.observe(this) { onLinkState(it) }

        //Разрешение на местоположение необязательное: оно нужно только чтобы показать имя
        //сети WiFi телефона. Спрашиваем один раз, при первом запуске; позже его можно
        //выдать нажатием на имя сети на главном экране.
        val pref = getSharedPreferences("MyPref", Context.MODE_PRIVATE)
        if (savedInstanceState == null && !hasFineLocation() &&
            !pref.getBoolean(Constants.LOCATION_ASKED, false)) {
            pref.edit().putBoolean(Constants.LOCATION_ASKED, true).apply()
            requestLocationPermission()
        }
    }

    override fun onStart() {
        super.onStart()
        mainHandler.removeCallbacks(releaseLinkInBackground)
        autoConnectLocalMode()
        //следим за сменой сети, чтобы имя сети телефона на главном экране было актуальным
        try {
            connectivityManager.registerDefaultNetworkCallback(networkCallback)
        } catch (e: RuntimeException) {
            Log.d(TAG, "MainActivity registerDefaultNetworkCallback ошибка: ${e.message}")
        }
        refreshPhoneSsid()
    }

    private val connectivityManager: ConnectivityManager by lazy {
        getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    //колбэки приходят не в главном потоке; имя сети обновляется чуть позже самой сети
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) = scheduleSsidRefresh()
        override fun onLost(network: Network) = scheduleSsidRefresh()
        override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) =
            scheduleSsidRefresh()
    }

    private val ssidRefresh = Runnable { refreshPhoneSsid() }

    private fun scheduleSsidRefresh() {
        mainHandler.removeCallbacks(ssidRefresh)
        mainHandler.postDelayed(ssidRefresh, SSID_REFRESH_DELAY_MS)
    }

    private fun hasFineLocation(): Boolean =
        checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED

    /**
     * Определить имя сети WiFi телефона и показать его на главном экране.
     * Android сообщает имя сети только при разрешении "Точное местоположение"
     * и включённой геолокации; иначе показываем, почему имени нет.
     */
    fun refreshPhoneSsid() {
        val name: String = if (EspLink.network != null) {
            getString(R.string.localNetworkName) //прямое подключение к ленте - имя известно точно
        } else {
            val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            @Suppress("DEPRECATION")
            val ssid = wifiManager.connectionInfo?.ssid?.removeSurrounding("\"") ?: ""
            when {
                ssid.isNotEmpty() && ssid != getString(R.string.unknownSsid) -> ssid
                !hasFineLocation() -> getString(R.string.phoneSsidNoPermission)
                else -> getString(R.string.network_name)
            }
        }
        model.updateTlfSSID(name)
        //телефон в сети ленты - её адрес известен
        if (name == getString(R.string.localNetworkName)) {
            model.updateIpAddressEsp(getString(R.string.hotpointIp))
        }
    }

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        refreshPhoneSsid()
        if (!hasFineLocation()) showMessage(getString(R.string.phoneSsidDenied))
    }

    private fun requestLocationPermission() {
        locationPermissionLauncher.launch(arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION))
    }

    /** Нажатие на имя сети телефона на главном экране. */
    fun onPhoneSsidClick() {
        if (!hasFineLocation()) {
            requestLocationPermission()
        } else {
            refreshPhoneSsid()
            if (model.getTlfSSID().value == getString(R.string.network_name)) {
                showMessage(getString(R.string.phoneSsidUnknown))
            }
        }
    }

    override fun onStop() {
        super.onStop()
        //Пока телефон подключен к NodeMCU, обычного WiFi на нём нет. Если приложение надолго
        //убрали с экрана - отключаемся, чтобы телефон вернулся в свою сеть.
        //При возврате в приложение подключимся снова (onStart).
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        } catch (e: RuntimeException) {
            //колбэк не был зарегистрирован
        }
        mainHandler.removeCallbacks(ssidRefresh)
        if (!isChangingConfigurations && EspLink.network != null) {
            mainHandler.postDelayed(releaseLinkInBackground, BACKGROUND_RELEASE_MS)
        }
    }

    private val mainHandler = Handler(Looper.getMainLooper())

    private val releaseLinkInBackground = Runnable {
        Log.d(TAG, "MainActivity: приложение давно в фоне - отключаемся от NodeMCU")
        LocalMode.autoConnectBlocked = false //при возврате подключиться снова
        EspLink.release(applicationContext)
    }

    //В локальном режиме (лента - точка доступа NodeMCU) подключаемся к ленте сами
    private fun autoConnectLocalMode() {
        if (!LocalMode.isEnabled(this) || LocalMode.autoConnectBlocked) return
        //на экране мастера подключением управляет сам мастер
        if (navController.currentDestination?.id == R.id.provisionFragment) return
        val state = EspLink.state.value
        if (state == EspLink.State.CONNECTING || state == EspLink.State.CONNECTED) return
        LocalMode.autoConnectBlocked = true //одна попытка; см. описание флага
        LocalMode.connect(this)
    }

    //Состояние прямого подключения к ленте: сообщения и данные для главного экрана
    private fun onLinkState(state: EspLink.State) {
        invalidateOptionsMenu() //обновить текст пункта меню
        scheduleSsidRefresh()   //имя сети телефона могло измениться
        val previous = model.lastLinkState
        if (state == previous) return //LiveData повторно отдала то же значение
        model.lastLinkState = state
        if (previous == null && state == EspLink.State.IDLE) return //запуск приложения

        if (previous == EspLink.State.CONNECTED && state == EspLink.State.LOST) {
            LocalMode.autoConnectBlocked = false //связь была и пропала - можно пробовать снова
        }
        //на экране мастера всё показывает сам мастер
        if (navController.currentDestination?.id == R.id.provisionFragment) return

        when (state) {
            EspLink.State.CONNECTING -> showMessage(getString(R.string.linkConnecting))
            EspLink.State.CONNECTED -> {
                //лента в режиме точки доступа всегда на этом адресе
                model.updateIpAddressEsp(getString(R.string.hotpointIp))
                model.updateTlfSSID(getString(R.string.localNetworkName))
                EspApi.get(getString(R.string.urlHotpoint), getString(R.string._esp),
                    onError = {
                        showMessage(getString(
                            if (isVpnActive()) R.string.noConnectionVpn
                            else R.string.linkConnectedNoAnswer))
                    },
                    onSuccess = {
                        model.updateEspNetworkSsid(it)
                        showMessage(getString(R.string.linkConnected, it))
                    })
            }
            EspLink.State.FAILED -> showMessage(getString(R.string.linkFailed))
            EspLink.State.LOST -> showMessage(getString(R.string.linkLost))
            EspLink.State.IDLE ->
                if (previous == EspLink.State.CONNECTED) showMessage(getString(R.string.linkReleased))
        }
    }

    // Этот метод вызывается всякий раз, когда пользователь выбирает переход вверх
    // в иерархии действий приложения из панели действий (экшенбара)
    override fun onSupportNavigateUp(): Boolean {
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                ||super.onSupportNavigateUp()
    }

    //меню шторки
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        //todo
        // Выделяем выбранный пункт меню в шторке
        item.isChecked = true
        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return false
    }

    companion object {
        //через сколько после ухода приложения с экрана отключаться от NodeMCU
        private const val BACKGROUND_RELEASE_MS = 5 * 60 * 1000L
        //имя сети WiFi обновляется чуть позже, чем приходит событие о смене сети
        private const val SSID_REFRESH_DELAY_MS = 1500L
    }
}
