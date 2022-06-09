package com.bartex.adresslentawifi.settings

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

class SyncroFragment:Fragment(), Server.MessageListener {

    var wifiList: MutableList<String> = mutableListOf()
    lateinit var rvWiFi: RecyclerView
    lateinit var tvInfo: TextView
    lateinit var btnPrev: Button
    lateinit var btnNext: Button
    lateinit var bottomSheetHome: ConstraintLayout
    lateinit var  headerHome:TextView
    lateinit var  descriptionHome:TextView
    lateinit var nameHome:EditText
    lateinit var passHome:EditText
    lateinit var profileHome: Group
    lateinit var sendNamePassToServerHome:Button
    lateinit var buttonToSettingHome: Button

    lateinit var wiFiListAdapter: WiFiListAdapter
    lateinit var wifiManager: WifiManager
    lateinit var wifiScanReceiver: BroadcastReceiver
    val intentFilter = IntentFilter()
     var isNodeMcu = false
     var actionTag:String? = ""
     val model: SharedViewModel by activityViewModels()
     lateinit var bottomSheetBehavior: BottomSheetBehavior<ConstraintLayout>

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "SyncroFragment onStart ")
        actionTag = arguments?.getString(ACTION_NAME, "")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return LayoutInflater.from(container?.context)
            .inflate(R.layout.fragment_syncro, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        //Log.d(TAG, "SyncroFragment onViewCreated ")
        initView(view)
        wifiManager = requireActivity().getSystemService(Context.WIFI_SERVICE) as WifiManager
        //Проверяем включен ли WiFi, если нет то включаем
        enableWifi()
        //проверяем текущую сеть WiFi
        val ssid = wifiManager.connectionInfo.ssid.removeSurrounding("\"")
        isNodeMcu = ssid == Constants.NODE_MCU
        model.updateSSID(ssid)
        Log.d(TAG, "SyncroFragment onViewCreated isNodeMcu =$isNodeMcu")
        //находим корневой лейаут и подключаем BottomSheet
        initBottomSheet(view, isNodeMcu)

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
        requireActivity().registerReceiver(wifiScanReceiver,intentFilter)
    }

    override fun onPause() {
        super.onPause()
        requireActivity(). unregisterReceiver(wifiScanReceiver)
    }

    //abstract fun initBottomSheet(view: View, isNodeMCU:Boolean)

    //находим корневой лейаут и подключаем BottomSheet
    private fun initBottomSheet(view: View, isNodeMCU:Boolean) {
        bottomSheetHome = view.findViewById(R.id.bottom_sheet_container_home)
        headerHome =view.findViewById(R.id.bottom_sheet_header_home)
        descriptionHome =view.findViewById(R.id.bottom_sheet_description_home)
        nameHome =view.findViewById(R.id.edName_home)
        passHome =view.findViewById(R.id.edPass_home)
        profileHome =view.findViewById(R.id.profile_home)
        sendNamePassToServerHome =view.findViewById(R.id.savePass_home)
        buttonToSettingHome = view.findViewById(R.id.button_to_setting_home)
        //buttonToSettingHome.visibility = View.GONE

        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetHome)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HALF_EXPANDED

        buttonToSettingHome.setOnClickListener {
            startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
        }
        sendNamePassToServerHome.setOnClickListener {
            val name = nameHome.text.toString()
            val password = passHome.text.toString()
            Log.d(TAG, "SyncroFragment sendNamePassToServerHome" +
                    "  name = $name password = $password")
            Server(
                getString(R.string.urlHotpoint),
                getString(R.string.postHome, getString(R.string._home),name, password),
                this@SyncroFragment
            ).start()

            profileHome.visibility = View.GONE  //убираем с экрана чтобы не путаться
            buttonToSettingHome.visibility = View.VISIBLE
            model.updateIpAddress(getString(R.string.hotpointZero))
        }
//
//        when(actionTag){
//            getString(R.string.createHotpoint) ->{}
//            getString(R.string.toHome) ->{}
//            getString(R.string.toDifferent) ->{}
//            getString(R.string.firstInclusion) ->{}
//        }
        //изменяем параметры в зависимости от того, есть ли подключение к сети NodeMCU
        if (isNodeMCU){
            model.updateIpAddress(getString(R.string.hotpointIp))
            profileHome.visibility = View.VISIBLE
            headerHome.text = requireActivity().getString(R.string.bottom_shit_title22)
            descriptionHome.text = requireActivity().getString(R.string.bottom_shit_descr2)
        }else{
            profileHome.visibility = View.GONE
            headerHome.text = requireActivity().getString(R.string.bottom_shit_title12)
            descriptionHome.text = requireActivity().getString(R.string.bottom_shit_descr1)
        }
    }

    private fun initView(view: View) {
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