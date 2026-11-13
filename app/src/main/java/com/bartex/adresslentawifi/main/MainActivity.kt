package com.bartex.adresslentawifi.main

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
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

        //запрашиваем разрешение на определение местоположения
        if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.CHANGE_WIFI_STATE,
                Manifest.permission.CHANGE_WIFI_MULTICAST_STATE,
                Manifest.permission.CHANGE_NETWORK_STATE,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.ACCESS_NETWORK_STATE), 100)
        }
    }

    override fun onStart() {
        super.onStart()
        mainHandler.removeCallbacks(releaseLinkInBackground)
        autoConnectLocalMode()
    }

    override fun onStop() {
        super.onStop()
        //Пока телефон подключен к NodeMCU, обычного WiFi на нём нет. Если приложение надолго
        //убрали с экрана - отключаемся, чтобы телефон вернулся в свою сеть.
        //При возврате в приложение подключимся снова (onStart).
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
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
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

    //метод обратного вызова после запроса разрешений в onCreate
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>,
                                            grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 100) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)  {
                Toast.makeText(this, resources.getString(R.string.permission),
                    Toast.LENGTH_SHORT).show()
                //повторный вызов onCreate
                recreate()
            }else {
                Toast.makeText(this, resources.getString(R.string.notPermission),
                    Toast.LENGTH_SHORT ).show()
            }
        }
    }


    companion object {
        //через сколько после ухода приложения с экрана отключаться от NodeMCU
        private const val BACKGROUND_RELEASE_MS = 5 * 60 * 1000L
    }
}
