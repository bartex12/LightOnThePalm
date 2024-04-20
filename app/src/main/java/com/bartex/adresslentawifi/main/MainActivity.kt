package com.bartex.adresslentawifi.main

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import com.bartex.adresslentawifi.R
import com.bartex.adresslentawifi.Server
import com.bartex.adresslentawifi.SharedViewModel
import com.bartex.adresslentawifi.constants.Constants
import com.bartex.adresslentawifi.constants.Constants.TAG
import com.bartex.adresslentawifi.databinding.ActivityMainBinding
import com.google.android.material.navigation.NavigationView

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

        //запрашиваем разрешение на определение местоположения
        if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.ACCESS_NETWORK_STATE), 100)
        }
    }

    // Этот метод вызывается всякий раз, когда пользователь выбирает переход вверх
    // в иерархии действий приложения из панели действий (экшенбара)
    override fun onSupportNavigateUp(): Boolean {
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                ||super.onSupportNavigateUp()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu?.findItem(R.id.sync)?.isVisible =
            navController.currentDestination?.id ==  R.id.listFragment
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
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