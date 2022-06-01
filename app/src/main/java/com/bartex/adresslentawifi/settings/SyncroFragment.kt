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
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bartex.adresslentawifi.R
import com.bartex.adresslentawifi.Server
import com.bartex.adresslentawifi.adapter.WiFiListAdapter
import com.bartex.adresslentawifi.constants.Constants
import com.bartex.adresslentawifi.constants.Constants.TAG
import com.google.android.material.bottomsheet.BottomSheetBehavior

class SyncroFragment:Fragment(), Server.MessageListener {

    var wifiList: MutableList<String> = mutableListOf()
    private lateinit var rvWiFi: RecyclerView
    private lateinit var tvInfo: TextView
    private var nameTag:String? = ""

    private lateinit var wiFiListAdapter: WiFiListAdapter
    private lateinit var wifiManager: WifiManager
    private lateinit var wifiScanReceiver: BroadcastReceiver
    private val intentFilter = IntentFilter()
//    var networkSSID = "WL500GPv2_GbcK"
//    var networkPass = "aassddff"

    private var networkSSID = "NodeMCU"
    private var networkPass = "12345678"
    private var isNodeMcu = false

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<ConstraintLayout>

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "SyncroFragment onStart ")
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
        Log.d(TAG, "SyncroFragment onViewCreated ")
        initView(view)
        wifiManager = requireActivity().getSystemService(Context.WIFI_SERVICE) as WifiManager
        //Проверяем включен ли WiFi, если нет то включаем
        enableWifi()
        //проверяем текущую сеть WiFi
        isNodeMcu = (wifiManager.connectionInfo.ssid.removeSurrounding("\"")
                == Constants.NODE_MCU)
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

    //находим корневой лейаут и подключаем BottomSheet
    private fun initBottomSheet(view: View, isNode:Boolean) {

        val bottomSheet: ConstraintLayout = view.findViewById(R.id.bottom_sheet_container)
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HALF_EXPANDED

        val  header:TextView =view.findViewById(R.id.bottom_sheet_header)
        header.text = requireActivity().getString(R.string.bottom_shit_title12)

        val  description:TextView =view.findViewById(R.id.bottom_sheet_description)
        description.text = requireActivity().getString(R.string.bottom_shit_text)

        val buttonSheet: Button = view.findViewById(R.id.buttonBottomSheet)
        buttonSheet.setOnClickListener {
            startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
        }
        val bottomSheetHome: ConstraintLayout = view.findViewById(R.id.bottom_sheet_container_home)
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetHome)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HALF_EXPANDED

        val  headerHome:TextView =view.findViewById(R.id.bottom_sheet_header_home)
        //headerHome.text = requireActivity().getString(R.string.bottom_shit_title_home)

        val  descriptionHome:TextView =view.findViewById(R.id.bottom_sheet_description_home)
        descriptionHome.text = requireActivity().getString(R.string.bottom_shit_text_home)

        val buttonSheetHome: Button = view.findViewById(R.id.buttonBottomSheetHome)
        buttonSheetHome.setOnClickListener {
            startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
        }

        val nameHome:EditText =view.findViewById(R.id.edName_home)
        val passHome:EditText =view.findViewById(R.id.edPass_home)
        val sendNamePassToServerHome:Button =view.findViewById(R.id.savePass_home)
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
        }

        if (isNode){
            bottomSheet.visibility = View.GONE
            bottomSheetHome.visibility = View.VISIBLE
            headerHome.text = requireActivity().getString(R.string.bottom_shit_title22)
        }else{
            bottomSheet.visibility = View.VISIBLE
            bottomSheetHome.visibility = View.GONE
            header.text = requireActivity().getString(R.string.bottom_shit_title12)
        }
    }

    private fun initView(view: View) {
        rvWiFi = view.findViewById(R.id.rvWiFi)
        tvInfo = view.findViewById(R.id.tvInfo)
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
        nameTag?. let{wiFiListAdapter = WiFiListAdapter(it, getOnWiFiListener())}
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
                nameTag = "NodeMCU"
                startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
            }
        }
    }

    override fun onMessage(message: String) {
        Log.d(TAG, "onMessage message = $message")
    }

}