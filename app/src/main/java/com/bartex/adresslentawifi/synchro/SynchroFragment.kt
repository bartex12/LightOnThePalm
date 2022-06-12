package com.bartex.adresslentawifi.synchro

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.ScanResult
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
import com.bartex.adresslentawifi.Server
import com.bartex.adresslentawifi.SharedViewModel
import com.bartex.adresslentawifi.adapter.WiFiListAdapter
import com.bartex.adresslentawifi.constants.Constants
import com.bartex.adresslentawifi.constants.Constants.ACTION_NAME
import com.bartex.adresslentawifi.constants.Constants.TAG
import com.google.android.material.bottomsheet.BottomSheetBehavior

class SynchroFragment:Fragment(), Server.MessageListener {

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
        initView(view)
        wifiManager = requireActivity().getSystemService(Context.WIFI_SERVICE) as WifiManager
        //Проверяем включен ли WiFi, если нет то включаем
        enableWifi()
        //находим корневой лейаут и подключаем BottomSheet
        initBottomSheet(isNodeMcu)

        wifiScanReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)
                if (success) {
                    // обрабатываем успех
                    scanSuccess()
                } else {
                    // обрабатываем неудачу
                    scanFailure()
                }
            }
        }
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        requireActivity().registerReceiver(wifiScanReceiver,intentFilter)
        val success = wifiManager.startScan()
        if (!success) {
            // обрабатываем неудачу
            scanFailure()
        }
        tvInfo.text = getString(R.string.StartScan)

        //приводим меню тулбара в соответствии с onPrepareOptionsMenu в MainActivity
        setHasOptionsMenu(true)
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "SynchroFragment onResume ")
        requireActivity().registerReceiver(wifiScanReceiver,intentFilter)
        //проверяем текущую сеть WiFi
        val ssid = wifiManager.connectionInfo.ssid.removeSurrounding("\"")
        isNodeMcu = ssid == Constants.NODE_MCU
        model.updateSSID(ssid)
        Log.d(TAG, "SynchroFragment onViewCreated ssid =$ssid isNodeMcu =$isNodeMcu")
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
            model.updateIpAddress(getString(R.string.hotpointIp))
            profileHome.visibility = View.VISIBLE
            headerHome.text = requireActivity().getString(R.string.bottom_shit_title22)
            descriptionHome.text = requireActivity().getString(R.string.bottom_shit_descr2)
        }
    }

    //находим корневой лейаут и подключаем BottomSheet
    private fun initBottomSheet(isNodeMCU:Boolean) {

        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetHome)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HALF_EXPANDED

        when(actionTag){
            //Привязка к сети WiFi при первом включении
            getString(R.string.firstInclusion) ->{
                profileHome.visibility = View.GONE
                headerHome.text = requireActivity().getString(R.string.bottom_shit_title12)
                descriptionHome.text = requireActivity().getString(R.string.bottom_shit_descr1)
            }

            //Переключиться на другую сеть WiFi
            getString(R.string.toDifferent) ->{
                //todo брать из сохр в преференсис а лучше из списка
                btnPrev.visibility = View.GONE
                btnNext.visibility = View.GONE
                nameWiFi.setText(getString(R.string.Redmi))
                passWiFi.setText(getString(R.string.passRedmi))
                profileHome.visibility = View.VISIBLE
                headerHome.text = requireActivity().getString(R.string.bottom_shit_title_different)
                descriptionHome.text = requireActivity().getString(R.string.bottom_shit_descr_different)
            }

            //Переключиться на домашнюю сеть WiFi из другой сети
            getString(R.string.toHome) ->{
                //todo брать из сохр в преференсис так как дом сети - разные могут быть, а лучше из списка
                btnPrev.visibility = View.GONE
                btnNext.visibility = View.GONE
                nameWiFi.setText(R.string.homeNetworkName)
                passWiFi.setText((R.string.homeNetworkPass))
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
                //todo брать из сохр в преференсис а лучше из списка
                btnPrev.visibility = View.GONE
                btnNext.visibility = View.GONE
                nameWiFi.setText(getString(R.string.Redmi))
                passWiFi.setText(getString(R.string.passRedmi))
                profileHome.visibility = View.VISIBLE
                headerHome.text = requireActivity().getString(R.string.bottom_shit_title_different_from_local)
                descriptionHome.text = requireActivity().getString(R.string.bottom_shit_descr_different)
            }
        }

        sendNamePassToServerHome.setOnClickListener {
            val name = nameWiFi.text.toString()
            val password = passWiFi.text.toString()
            Log.d(TAG, "SynchroFragment sendNamePassToServerHome" +
                    "  name = $name password = $password")
            if (name.isNotEmpty()&& password.isNotEmpty()){
               val ip = model.getIpAddress().value

                when(actionTag){
                    getString(R.string.firstInclusion),
                    getString(R.string.toHomeFromLocal),
                    getString(R.string.toDifferentFromLocal)->{
                        Server(
                            getString(R.string.urlHotpoint ),
                            getString(R.string.postHome, getString(R.string._home),name, password),
                            this@SynchroFragment
                        ).start()

                        profileHome.visibility = View.GONE  //убираем с экрана чтобы не путаться
                        buttonToSettingHome.visibility = View.VISIBLE
                        model.updateIpAddress(getString(R.string.hotpointZero))
                        model.updateNameOfEffect("")
                    }

                    getString(R.string.toHome),
                    getString(R.string.toDifferent) ->{
                        Server(
                            getString(R.string.baseUrl,ip ),
                            getString(R.string.postHome, getString(R.string._home),name, password),
                            this@SynchroFragment
                        ).start()
                        profileHome.visibility = View.GONE  //убираем с экрана чтобы не путаться
                        buttonToSettingHome.visibility = View.VISIBLE
                        model.updateIpAddress(getString(R.string.hotpointZero))
                    }

                    getString(R.string.createHotpoint) ->{
                        Server(
                            getString(R.string.baseUrl,ip ),
                            getString(R.string.postLocal, getString(R.string._hotpoint),name, password),
                            this@SynchroFragment
                        ).start()
                        profileHome.visibility = View.GONE  //убираем с экрана чтобы не путаться
                        buttonToSettingHome.visibility = View.VISIBLE
                        model.updateIpAddress(getString(R.string.hotpointIp))
                        model.updateNameOfEffect("")
                    }
                }
            }else{
                Toast.makeText(requireActivity(), "имя и пароль не должны быть пустыми", Toast.LENGTH_LONG).show()
            }
        }

        buttonToSettingHome.setOnClickListener {
            startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
        }
    }

    private fun scanSuccess() {
        val results:MutableList<ScanResult> = wifiManager.scanResults
        "Доступно WiFi сетей:  ${results.size}".also { tvInfo.text = it }
        Log.d(TAG, "scanSuccess: results size = ${results.size}")

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

    private fun scanFailure() {
        // handle failure: new scan did NOT succeed
        // consider using old scan results: these are the OLD results!
        val results = wifiManager.scanResults
        //... potentially use older scan results ...
        Log.d(TAG, "scanFailure ")
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

    override fun onMessage(message: String) {
        Log.d(TAG, "onMessage message = $message")
    }

}