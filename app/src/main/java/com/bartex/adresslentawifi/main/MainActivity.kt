package com.bartex.adresslentawifi.main

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
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
//две библиотеки, + режим STA     статич цвета - через параметры достаём цвет
//в loop исполняем несколько вариантов , которые определены в setup в функции server.on()
// код Андроид F:\AndroidStudioProjects\Android15 Bluetooth\AdressLentaWiFi  ветка listMVVM_GoogleNavigation
// скетч F:\Arduino\004 АРДУИНО с диска E\010 Адресная лента\_005EspWiFi_2lib_STA_sketch_may14a
//варианты - бег строка , pride, flame, статич цвета

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener{

    //private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var toolbar: Toolbar
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var  appBarConfiguration:AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(R.layout.activity_main)

        initViews()

        //находим NavController
        navController = Navigation.findNavController(this, R.id.nav_host_fragment)
        toolbar.setTitleTextAppearance(this, R.style.ToolbarTitleStyle)
        toolbar.title = "Мир цвета"
        //поддержка экшенбара для создания строки поиска
        setSupportActionBar(toolbar)
        //отключаем показ заголовка
        supportActionBar?.setDisplayShowTitleEnabled(false)
        appBarConfiguration = AppBarConfiguration.Builder(navController.graph)
            //Отображать кнопку навигации как гамбургер , когда она не отображается как кнопка вверх
            .setOpenableLayout(drawerLayout)
            .build()
        //обработка событий экшенбара -гамбургер, кнопка вверх
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration)

        //слушатель меню шторки -для обработки пунктов шторки
        navView.setNavigationItemSelectedListener(this)

    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)
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
        drawerLayout.closeDrawer(GravityCompat.START)
        return false
    }

}