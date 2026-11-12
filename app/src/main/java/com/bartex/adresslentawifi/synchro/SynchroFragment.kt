package com.bartex.adresslentawifi.synchro

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.net.wifi.ScanResult
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.Group
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bartex.adresslentawifi.R
import com.bartex.adresslentawifi.EspApi
import com.bartex.adresslentawifi.SharedViewModel
import com.bartex.adresslentawifi.adapter.WiFiListAdapter
import com.bartex.adresslentawifi.constants.Constants
import com.bartex.adresslentawifi.constants.Constants.ACTION_NAME
import com.bartex.adresslentawifi.constants.Constants.TAG
import com.google.android.material.bottomsheet.BottomSheetBehavior


class SynchroFragment:Fragment() {

    private var wifiList: MutableList<String> = mutableListOf()
    private lateinit var rvWiFi: RecyclerView
    private lateinit var tvInfo: TextView
    private lateinit var btnPrev: Button
    private lateinit var btnNext: Button
    private lateinit var bottomSheetHome: ConstraintLayout
    private lateinit var headerHome:TextView
    private lateinit var descriptionHome:TextView
    private lateinit var nameWiFi:EditText
    private lateinit var passWiFi:EditText
    private lateinit var profileHome: Group
    private lateinit var sendNamePassToServerHome:Button
    private lateinit var buttonToSettingHome: Button

    private lateinit var wiFiListAdapter: WiFiListAdapter
    private lateinit var wifiManager: WifiManager
    private lateinit var wifiScanReceiver: BroadcastReceiver
    private val intentFilter = IntentFilter()
    private var isNodeMcu = false
    private var actionTag:String? = ""
    private val model: SharedViewModel by activityViewModels()
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<ConstraintLayout>
    private lateinit var pref: SharedPreferences


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        actionTag = arguments?.getString(ACTION_NAME, "")
        Log.d(TAG, "SynchroFragment onCreate actionTag = $actionTag")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d(TAG, "SynchroFragment onCreateView")
        return LayoutInflater.from(container?.context)
            .inflate(R.layout.fragment_syncro, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.d(TAG, "SynchroFragment onViewCreated ")
        pref = requireActivity().getSharedPreferences("MyPref", Context.MODE_PRIVATE)
        initView(view)
        wifiManager = requireActivity().getSystemService(Context.WIFI_SERVICE) as WifiManager
        //Проверяем включен ли WiFi, если нет то включаем
        enableWifi()
        //находим корневой лейаут и подключаем BottomSheet
        initBottomSheet(isNodeMcu)

        wifiScanReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)
                Log.d(TAG, "SynchroFragment onViewCreated onReceive success =$success")
                if (success) {
                    scanSuccess()  // обрабатываем успех
                } else {
                    scanFailure()  // обрабатываем неудачу
                }
            }
        }
        //приводим меню тулбара в соответствии с onPrepareOptionsMenu в MainActivity
        setHasOptionsMenu(true)
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "SynchroFragment onResume ")

        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        requireActivity().registerReceiver(wifiScanReceiver,intentFilter)
        val success = wifiManager.startScan()
        if (!success) {
            // обрабатываем неудачу
            scanFailure()
        }
        tvInfo.text = getString(R.string.StartScan)

        //проверяем текущую сеть WiFi
        val ssid = wifiManager.connectionInfo.ssid.removeSurrounding("\"")
        isNodeMcu = ssid == Constants.NODE_MCU
        model.updateTlfSSID(ssid)
        Log.d(TAG, "SynchroFragment onResume ssid =$ssid isNodeMcu =$isNodeMcu")
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "SynchroFragment onPause ")
        requireActivity(). unregisterReceiver(wifiScanReceiver)
    }

    private fun initView(view: View) {
        bottomSheetHome = view.findViewById(R.id.bottom_sheet_container_home)
        headerHome =view.findViewById(R.id.bottom_sheet_header_home)
        descriptionHome =view.findViewById(R.id.bottom_sheet_description_home)
        nameWiFi =view.findViewById(R.id.edName_home)
        passWiFi =view.findViewById(R.id.edPass_home)
        profileHome =view.findViewById(R.id.profile_home)
        sendNamePassToServerHome =view.findViewById(R.id.savePass_home)
        buttonToSettingHome = view.findViewById(R.id.button_to_setting_home)

        rvWiFi = view.findViewById(R.id.rvWiFi)
        tvInfo = view.findViewById(R.id.tvInfo)
        btnPrev = view.findViewById(R.id.btnPrev)
        btnPrev.setOnClickListener {
            profileHome.visibility = View.GONE
            headerHome.text = requireActivity().getString(R.string.bottom_shit_title12)
            descriptionHome.text = requireActivity().getString(R.string.bottom_shit_descr1)
        }
        btnNext = view.findViewById(R.id.btnNext)
        btnNext.setOnClickListener {
            model.updateIpAddressEsp(getString(R.string.hotpointIp))
            profileHome.visibility = View.VISIBLE
            headerHome.text = requireActivity().getString(R.string.bottom_shit_title22)
            descriptionHome.text = requireActivity().getString(R.string.bottom_shit_descr2)
        }
    }

    //находим корневой лейаут и подключаем BottomSheet
    private fun initBottomSheet(isNodeMCU:Boolean) {

        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetHome)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HALF_EXPANDED

        //подставляем имя сети, которое вводили в прошлый раз (пароль не храним)
        //для локальной сети ниже оно заменяется на NodeMCU
        nameWiFi.setText(pref.getString(ssidPrefKey(), ""))
        passWiFi.setText("")

        when(actionTag){
            //Привязка к сети WiFi при первом включении
            getString(R.string.firstInclusion),
            getString(R.string.reconnecting)->{
                profileHome.visibility = View.GONE
                headerHome.text = requireActivity().getString(R.string.bottom_shit_title12)
                descriptionHome.text = requireActivity().getString(R.string.bottom_shit_descr1)
            }

            //Переключиться на другую сеть WiFi
            getString(R.string.toDifferent) ->{
                btnPrev.visibility = View.GONE
                btnNext.visibility = View.GONE
                profileHome.visibility = View.VISIBLE
                headerHome.text = requireActivity().getString(R.string.bottom_shit_title_different)
                descriptionHome.text = requireActivity().getString(R.string.bottom_shit_descr_different)
            }

            //Переключиться на домашнюю сеть WiFi из другой сети
            getString(R.string.toHome) ->{
                btnPrev.visibility = View.GONE
                btnNext.visibility = View.GONE
                profileHome.visibility = View.VISIBLE
                headerHome.text = requireActivity().getString(R.string.bottom_shit_title_home)
                descriptionHome.text = requireActivity().getString(R.string.bottom_shit_descr_home )
            }

            //Создать локальную сеть WiFi
            getString(R.string.createHotpoint) ->{
                btnPrev.visibility = View.GONE
                btnNext.visibility = View.GONE
                nameWiFi.setText(R.string.localNetworkName)
                nameWiFi.isEnabled = false
                passWiFi.setText((R.string.localNetworkPass))
                passWiFi.isEnabled = false
                profileHome.visibility = View.VISIBLE
                headerHome.text = requireActivity().getString(R.string.bottom_shit_title_local)
                descriptionHome.text = requireActivity().getString(R.string.bottom_shit_descr_local)
            }

            //Переключиться на домашнюю сеть WiFi из локальной сети
            getString(R.string.toHomeFromLocal) -> {
                btnPrev.visibility = View.GONE
                btnNext.visibility = View.GONE
                profileHome.visibility = View.VISIBLE
                headerHome.text = requireActivity().getString(R.string.bottom_shit_title_home_from_local)
                descriptionHome.text = requireActivity().getString(R.string.bottom_shit_descr_home)
            }

            //Переключиться на другую сеть WiFi из локальной сети
            getString(R.string.toDifferentFromLocal) -> {
                btnPrev.visibility = View.GONE
                btnNext.visibility = View.GONE
                profileHome.visibility = View.VISIBLE
                headerHome.text = requireActivity().getString(R.string.bottom_shit_title_different_from_local)
                descriptionHome.text = requireActivity().getString(R.string.bottom_shit_descr_different)
            }
        }

        sendNamePassToServerHome.setOnClickListener {
            val name = nameWiFi.text.toString()
            val password = passWiFi.text.toString()
            Log.d(TAG, "SynchroFragment sendNamePassToServerHome name = $name") //пароль в лог не пишем
            when {
                name.isEmpty() || password.isEmpty() ->
                    Toast.makeText(requireActivity(), R.string.emptyNameOrPass, Toast.LENGTH_LONG).show()
                //в скетче под имя отведено 30 байт EEPROM, под пароль - 32
                name.toByteArray().size > MAX_SSID_BYTES ->
                    Toast.makeText(requireActivity(), R.string.tooLongName, Toast.LENGTH_LONG).show()
                password.toByteArray().size > MAX_PASS_BYTES ->
                    Toast.makeText(requireActivity(), R.string.tooLongPass, Toast.LENGTH_LONG).show()
                else -> sendNameAndPassword(name, password)
            }
        }

        buttonToSettingHome.setOnClickListener {
            startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
        }
    }

    @SuppressLint("MissingPermission")
    private fun scanSuccess() {
        val results:MutableList<ScanResult> = wifiManager.scanResults
        "Доступно WiFi сетей:  ${results.size}".also { tvInfo.text = it }
        Log.d(TAG, "scanSuccess: results size = ${results.size}")

        val listOfConfigurations: List<WifiConfiguration> = wifiManager.configuredNetworks
        val info = wifiManager.connectionInfo
        //val qq =info.ssid
        for (index in listOfConfigurations.indices) {
            val configuration = listOfConfigurations[index]
            if (configuration.networkId == info.networkId) {
                Log.d(TAG, "scanSuccess: имя текущей сети = ${configuration.SSID}")
            }
        }

        wifiList.clear() // чтобы при обновлении не накапливалось
        for (i in results.indices) {
            val ssid = results[i].SSID.toString()
            wifiList.add(ssid)
        }
        rvWiFi.layoutManager = LinearLayoutManager(requireActivity())
        wiFiListAdapter = WiFiListAdapter(getOnWiFiListener())
        rvWiFi.adapter = wiFiListAdapter
        wiFiListAdapter.list = wifiList

        //Settings.Global.putInt(contentResolver, "captive_portal_detection_enabled", 0)
    }

    @SuppressLint("MissingPermission")
    private fun scanFailure() {
        // handle failure: new scan did NOT succeed
        // consider using old scan results: these are the OLD results!
        val results = wifiManager.scanResults
        //... potentially use older scan results ...
        Log.d(TAG, "SynchroFragment scanFailure results = $results")
    }

    private fun enableWifi() {
        if (!wifiManager.isWifiEnabled) {
            wifiManager.isWifiEnabled = true
            val toast = Toast.makeText(requireActivity(), "Wifi Turned On", Toast.LENGTH_SHORT)
            toast.show()
        }
    }

    private fun getOnWiFiListener(): WiFiListAdapter.OnClickWiFiListener {
        return  object : WiFiListAdapter.OnClickWiFiListener{
            override fun onWiFiClick(name: String) {
                Log.d(TAG, "getOnWiFiListener name = $name")
                //nameTag = name
                startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
            }
        }
    }

    //ключ настроек, под которым запоминаем имя сети для текущего сценария
    private fun ssidPrefKey(): String = when(actionTag){
        getString(R.string.toDifferent),
        getString(R.string.toDifferentFromLocal) -> Constants.DIFFERENT_SSID
        else -> Constants.HOME_SSID
    }

    //передаём в блок управления имя и пароль сети
    private fun sendNameAndPassword(name: String, password: String) {
        val ip = model.getIpAddressEsp().value
        val baseUrl: String   //куда шлём
        val path: String      //команда скетча
        val newIp: String     //адрес ленты после выполнения команды
        val resetEffect: Boolean

        when(actionTag){
            //телефон подключен к точке доступа NodeMCU
            getString(R.string.firstInclusion),
            getString(R.string.reconnecting),
            getString(R.string.toHomeFromLocal),
            getString(R.string.toDifferentFromLocal)->{
                baseUrl = getString(R.string.urlHotpoint)
                path = getString(R.string._home)
                newIp = getString(R.string.hotpointZero)
                resetEffect = true
            }
            //телефон и лента в одной WiFi сети
            getString(R.string.toHome),
            getString(R.string.toDifferent) ->{
                baseUrl = getString(R.string.baseUrl, ip)
                path = getString(R.string._home)
                newIp = getString(R.string.hotpointZero)
                resetEffect = false
            }
            getString(R.string.createHotpoint) ->{
                baseUrl = getString(R.string.baseUrl, ip)
                path = getString(R.string._hotpoint)
                newIp = getString(R.string.hotpointIp)
                resetEffect = true
            }
            else -> return
        }

        if (actionTag != getString(R.string.createHotpoint)) {
            pref.edit().putString(ssidPrefKey(), name).apply()
        }
        sendNamePassToServerHome.isEnabled = false //от повторных нажатий, пока ждём ответ

        //порядок параметров важен: скетч читает их по номеру
        EspApi.get(baseUrl, path,
            listOf("name" to name, "password" to password),
            onError = {
                if (view == null) return@get
                sendNamePassToServerHome.isEnabled = true
                //форму не убираем - можно исправить и повторить
                Toast.makeText(requireActivity(), R.string.sendFailed, Toast.LENGTH_LONG).show()
            },
            onSuccess = {
                if (view == null) return@get
                sendNamePassToServerHome.isEnabled = true
                profileHome.visibility = View.GONE  //убираем с экрана чтобы не путаться
                buttonToSettingHome.visibility = View.VISIBLE
                model.updateIpAddressEsp(newIp)
                if (resetEffect) model.updateNameOfEffect("")
            })
    }

    companion object {
        private const val MAX_SSID_BYTES = 30
        private const val MAX_PASS_BYTES = 32
    }
}