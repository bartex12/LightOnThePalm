package com.bartex.adresslentawifi.main

import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import com.bartex.adresslentawifi.R
import com.bartex.adresslentawifi.SharedViewModel
import com.bartex.adresslentawifi.constants.Constants.TAG
import com.google.android.material.navigation.NavigationView
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient
import com.hivemq.client.mqtt.mqtt3.message.connect.connack.Mqtt3ConnAck
import java.util.concurrent.CompletableFuture

import kotlin.text.Charsets.UTF_8


//управление адресной лентой через NodeMCU с WiFi в режиме STA
//две библиотеки, + режим STA через параметры достаём цвет, яркость, скорость, количество точек
//в loop исполняем несколько вариантов , которые определены в setup в функции server.on()
// код Андроид F:\AndroidStudioProjects\Android15 Bluetooth\AdressLentaWiFi  ветка 5_mvvm_disign
// скетч F:\Arduino\004 АРДУИНО с диска E\010 Адресная лента\_006EspWiFi_2lib_STA_EVERY_sketch_may22a
//варианты - бег строка , pride, flame, статич цвета,конфетти,радуги

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener{

    private lateinit var navController: NavController
    private lateinit var toolbar: Toolbar
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var  appBarConfiguration:AppBarConfiguration
    private val model: SharedViewModel by lazy {
        ViewModelProvider(this)[SharedViewModel::class.java]
    }
    private val client = MqttHive().client
    private val connection = MqttHive().connection

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()

        //находим NavController
        navController = Navigation.findNavController(this, R.id.nav_host_fragment)
        //задаём шрифт и размер для title тулбара
        toolbar.setTitleTextAppearance(this, R.style.ToolbarTitleStyle)
        //поддержка экшенбара
        setSupportActionBar(toolbar)
        //отключаем показ заголовка - если будет свой - у нас своего textView нет
        //supportActionBar?.setDisplayShowTitleEnabled(false)
        appBarConfiguration = AppBarConfiguration.Builder(navController.graph)
            //Отображать кнопку навигации как гамбургер , когда она не отображается как кнопка вверх
            .setOpenableLayout(drawerLayout)
            .build()
        //обработка событий экшенбара -гамбургер, кнопка вверх
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration)
        //слушатель меню шторки -для обработки пунктов шторки
        navView.setNavigationItemSelectedListener(this)

        //получаем ip адрес сервера через MQTT
        //getIpFromMqtt(MqttHive().client, MqttHive().connection, "testtt")
        getIpFromMqtt(MqttBy().client, MqttBy().connection, "/user/bartex/test")

    }

    override fun onDestroy() {
        super.onDestroy()
        disconnectMqtt( MqttHive().client)
    }

    // Этот метод вызывается всякий раз, когда пользователь выбирает переход вверх
    // в иерархии действий приложения из панели действий (экшенбара)
    override fun onSupportNavigateUp(): Boolean {
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                ||super.onSupportNavigateUp()
    }

    private fun disconnectMqtt(client: Mqtt3AsyncClient){
        client.disconnect()
            .whenComplete { cl, e ->
                if (e != null) {
                    // Handle failure to subscribe
                    Log.d(TAG, "MainActivity onDestroy: e = ${e.message} ")
                } else {
                    // Handle successful subscription, e.g. logging or incrementing a metric
                    Log.d(TAG, "MainActivity onDestroy: cl = $cl ")
                }
            }
    }

    private fun getIpFromMqtt(client: Mqtt3AsyncClient, connection: CompletableFuture<Mqtt3ConnAck>, topicFilter:String ){
        connection
            .whenComplete { _, error ->
                if (error != null) {
                    // Обрабатываем ошибку подключения
                    Log.d(TAG, "MainActivity onCreate: throwable.conn = ${error.message} ")
                } else {
                    // подключение успешно -> можно подписаться на топик testtt или опубликовать что то
                    client.subscribeWith()
                        .topicFilter(topicFilter)
                        .callback { publish ->
                            val addressIp = String(publish.payloadAsBytes, UTF_8)
                            // Обрабатываем принятое сообщение
                           // Log.d(TAG, "***MainActivity onCreate: topic = ${publish.topic} ip = $addressIp\n")
                            runOnUiThread {
                                model.updateIpAddress(addressIp)
                            }
                        }
                        .send()
                        .whenComplete { _, err ->
                            if (err != null) {
                                // Обрабатываем ошибку подписки
                                Log.d(TAG, "MainActivity onCreate: throwable = ${err.message} ")
                            }
                        }
                }
            }
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)
    }

    //меню шторки
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        //todo
        // Выделяем выбранный пункт меню в шторке
        item.isChecked = true
        drawerLayout.closeDrawer(GravityCompat.START)
        return false
    }

}