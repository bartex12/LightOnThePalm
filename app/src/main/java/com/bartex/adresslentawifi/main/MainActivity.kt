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
import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.datatypes.MqttQos

import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient
import com.hivemq.client.mqtt.mqtt3.message.connect.connack.Mqtt3ConnAck
import java.util.*
import com.hivemq.client.mqtt.mqtt3.message.publish.Mqtt3Publish
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
    private lateinit var client: Mqtt3AsyncClient
    private val model: SharedViewModel by lazy {
        ViewModelProvider(this)[SharedViewModel::class.java]
    }

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

        //Подключение к брокеру
//         val connAck:CompletableFuture<Mqtt3ConnAck> = client.connectWith()
//            // here you can specify multiple properties described below
//            .cleanSession(true)
//             .noKeepAlive()
//             .simpleAuth()
//             .username("bartex")
//             .password("Kukamon@1".toByteArray())
//             .applySimpleAuth()
//            .send()

        //Создание клиента
        client = MqttClient.builder()
            .useMqttVersion3()
            .identifier(UUID.randomUUID().toString())
            .serverHost("broker.hivemq.com")
            .serverPort(1883)
            .buildAsync()

        //создание публикации
        val  publishMessage: Mqtt3Publish = Mqtt3Publish.builder()
            .topic("test333")
            .payload("hello world".toByteArray())
            .qos(MqttQos.AT_LEAST_ONCE)
            .build()

        //Подключение к брокеру
         client.connectWith()
            .simpleAuth()
            .username("bartex")
            .password("Kukamon@1".toByteArray())
            .applySimpleAuth()
            .send()
            .whenComplete { connAck: Mqtt3ConnAck?, throwable: Throwable? ->
                if (throwable != null) {
                    // handle failure
                    Log.d(TAG, "MainActivity onCreate: throwable.conn = ${throwable.message} ")
                } else {
                    // setup subscribes or start publishing
                    Log.d(TAG, "MainActivity onCreate: connAck = $connAck ")
                    // так как подключение успешно - подписываемся на топик testtt
                    client.subscribeWith()
                        .topicFilter("testtt")
                        .callback { publish ->
                            // Process the received message
                            Log.d(TAG, "***MainActivity onCreate: received publish topic = ${publish.topic}" +
                                    "\nreceived publish payloadAsBytes = ${String(publish.payloadAsBytes, UTF_8)}\n")
                       runOnUiThread {
                           val addressIp = String(publish.payloadAsBytes, UTF_8)
                           model. updateIpAddress(addressIp)
                         }
                        }
                        .send()
                        .whenComplete { subAck, e ->
                            if (e != null) {
                                // Handle failure to subscribe
                                Log.d(TAG, "MainActivity onCreate: throwable = ${e.message} ")
                            } else {
                                // Handle successful subscription, e.g. logging or incrementing a metric
                                Log.d(TAG, "MainActivity onCreate: subAck = $subAck ")
                            }
                        }
                    //так как подключение успешно - публикуем сообщение в топике test333
                    client.publish(publishMessage)
                        .whenComplete { publish, e ->
                            if (e != null) {
                                // handle failure to publish
                                Log.d(TAG, "MainActivity onCreate: publish.e = ${e.message} ")
                            } else {
                                // Handle successful subscription, e.g. logging or incrementing a metric
                                Log.d(TAG, "###MainActivity onCreate: publishMessage = " +
                                        "${String(publish.payloadAsBytes, UTF_8)} ")
                            }
                        }
                }
            }
    }

    override fun onDestroy() {
        super.onDestroy()
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

    // Этот метод вызывается всякий раз, когда пользователь выбирает переход вверх
    // в иерархии действий приложения из панели действий (экшенбара)
    override fun onSupportNavigateUp(): Boolean {
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                ||super.onSupportNavigateUp()
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