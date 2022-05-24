package com.bartex.adresslentawifi.main

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import com.bartex.adresslentawifi.R
import com.google.android.material.navigation.NavigationView

//управление адресной лентой через NodeMCU с WiFi в режиме STA
//две библиотеки, + режим STA через параметры достаём цвет, яркость, скорость, количество точек
//в loop исполняем несколько вариантов , которые определены в setup в функции server.on()
// код Андроид F:\AndroidStudioProjects\Android15 Bluetooth\AdressLentaWiFi  ветка listMVVM_GoogleNavigation
// скетч F:\Arduino\004 АРДУИНО с диска E\010 Адресная лента\_006EspWiFi_2lib_STA_EVERY_sketch_may22a
//варианты - бег строка , pride, flame, статич цвета,конфетти,радуги

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener{

    //private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var toolbar: Toolbar
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var  appBarConfiguration:AppBarConfiguration

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