package com.bartex.adresslentawifi.main

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
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
import com.bartex.adresslentawifi.constants.Constants
import com.bartex.adresslentawifi.constants.Constants.TAG
import com.bartex.adresslentawifi.databinding.ActivityMainBinding
import com.bartex.adresslentawifi.databinding.AppBarMainBinding
import com.google.android.material.navigation.NavigationView
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient
import com.hivemq.client.mqtt.mqtt3.message.connect.connack.Mqtt3ConnAck
import java.io.File
import java.util.concurrent.CompletableFuture

import kotlin.text.Charsets.UTF_8
//управление адресной лентой через NodeMCU с WiFi в режиме STA и AT
//две библиотеки, + режим STA через параметры достаём цвет, яркость, скорость, количество точек
//в loop исполняем несколько вариантов , которые определены в setup в функции server.on()
// код Андроид   F:\AndroidStudioProjects\Android14 Wifi\AdressLentaWiFi   ветка 13_WiFi_Scan
//скетч F:\Arduino\004 АРДУИНО с диска E\010 Адресная лента\_012_AP_STA_MQTT_EEPROM_ALL_sketch_jun09b
//варианты - бег строка , pride, flame, статич цвета,конфетти,радуги
//добавлено обращение на MQTT сервер для передачи ip адреса - используется HiveMQ но есть класс и для mqtt.by
//при этом делается 1 обращение в 5 сек  - в loop
//добавлено чтение и запись в EEPROM имени и пароля сети через функции set_String(0,1,ssidHome) и set_String(31,32,passHome)
//https://russianblogs.com/article/26471580690/  для создания подключения к домашней сети
//включено создание точки доступа по команде с телефона, а всего 7 сценариев работы с WiFi

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener{

    lateinit var binding:ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var  appBarConfiguration:AppBarConfiguration
    private val model: SharedViewModel by lazy {
        ViewModelProvider(this)[SharedViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

        //получаем ip адрес сервера через MQTT
       // getIpFromMqtt(MqttBy().client, MqttBy().connection, getString(R.string.topic_user_bartex_test))
        getIpFromMqtt(MqttHive().client, MqttHive().connection, getString(R.string.topic_user_bartex_test_hive))
        //запрашиваем разрешение на определение местоположения
        if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.ACCESS_NETWORK_STATE), 100)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        disconnectMqtt( MqttHive().client)
    }

    // Этот метод вызывается всякий раз, когда пользователь выбирает переход вверх
    // в иерархии действий приложения из панели действий (экшенбара)
    override fun onSupportNavigateUp(): Boolean {
       // Log.d(TAG, "MainActivity onSupportNavigateUp ")
        if (navController.currentDestination?.id ==  R.id.syncroFragment){
            // после перехода из фрагмента синхронизации перезапускаем связь с Mqtt сервером
            //иначе он сам не перезапустится - а почему он встаёт?
           //getIpFromMqtt(MqttBy().client, MqttBy().connection, getString(R.string.topic_user_bartex_test))
            getIpFromMqtt(MqttHive().client, MqttHive().connection, getString(R.string.topic_user_bartex_test_hive))
        }
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
        connection.whenComplete { _, error ->
                if (error != null) {
                    // Обрабатываем ошибку подключения
                    Log.d(TAG, "MainActivity onCreate: throwable.conn = ${error.message} ")
                    //если в локальной сети - будет повторяться снова и снова
                    if (model.getSSID().value != Constants.NODE_MCU){
                        //если нет соединения со стороны ESP , будет повторяться снова и снова
                        //getIpFromMqtt(MqttBy().client, MqttBy().connection, getString(R.string.topic_user_bartex_test))
                    }
                } else {
                    // подключение успешно -> можно подписаться на топик testtt или опубликовать что то
                    client.subscribeWith()
                        .topicFilter(topicFilter)
                        .callback { publish ->
                            val addressIp = String(publish.payloadAsBytes, UTF_8)
                            // Обрабатываем принятое сообщение
                            //Log.d(TAG, "***MainActivity onCreate: topic = ${publish.topic} ip = $addressIp\n")
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        //Log.d(TAG, "MainActivity onCreateOptionsMenu ")
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
       // Log.d(TAG, "MainActivity onPrepareOptionsMenu ")
        menu?.findItem(R.id.sync)?.isVisible =
            navController.currentDestination?.id ==  R.id.listFragment

        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        //Log.d(TAG, "MainActivity onOptionsItemSelected ")
        when(item.itemId){
           //R.id.sync -> navController.navigate(R.id.action_listFragment_to_syncroFragment)
            R.id.sync -> navController.navigate(R.id.action_listFragment_to_actionFragment)
        }
        return super.onOptionsItemSelected(item)
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
//            if ((checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
//                        == PackageManager.PERMISSION_GRANTED)
//            ){
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

}