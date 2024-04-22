package com.bartex.adresslentawifi.list

import android.content.Context
import android.content.SharedPreferences
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.Group
import androidx.core.os.bundleOf
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import com.bartex.adresslentawifi.R
import com.bartex.adresslentawifi.Server
import com.bartex.adresslentawifi.SharedViewModel
import com.bartex.adresslentawifi.adapter.RvListAdapter
import com.bartex.adresslentawifi.constants.Constants.BASE_URL
import com.bartex.adresslentawifi.constants.Constants.IP
import com.bartex.adresslentawifi.constants.Constants.ITEM_NAME
import com.bartex.adresslentawifi.constants.Constants.KEY_WORD
import com.bartex.adresslentawifi.constants.Constants.TAG
import com.bartex.adresslentawifi.databinding.FragmentListBinding
import com.bartex.adresslentawifi.entity.ListOfEffects
import com.bartex.adresslentawifi.main.MainActivity


class   ListFragment : Fragment(), Server.MessageListener{

    companion object {
        const val SERVICE_TYPE = "_http._tcp"
    }

    private var _binding:FragmentListBinding? = null
    private val binding get() = _binding!!

    private val listViewModel: ListViewModel by activityViewModels()
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private lateinit var navController: NavController
    private lateinit var pref: SharedPreferences
    private var rvAdapter: RvListAdapter? = null
    private var nameTag:String? = ""

    val espName:String = "esp8266"
    lateinit var nsdManager: NsdManager

    private var isShowInfo: Boolean = true
    lateinit var info: Group

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        Log.d(TAG, "@@@ListFragment onCreateView")
        _binding = FragmentListBinding.inflate(inflater, container, false)
        return binding.root  //fragment_list
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "@@@ListFragment onViewCreated")

        pref = requireActivity().getSharedPreferences("MyPref", AppCompatActivity.MODE_PRIVATE)
        navController = Navigation.findNavController(view)
        //определяем NsdManager менеджер
        nsdManager = requireActivity().getSystemService(Context.NSD_SERVICE) as NsdManager
        info = binding.infoGroup

        getIp()  //получаем ip адрес из преференсис - если лента выключена, будет старый сохранённый ip

        initAdapter()  //иначе при отключенной ленте будет пустой экран
        //todo восстановить запрос по указателю
//        if (sharedViewModel.isFirstLaunch){
//            Log.d(TAG, "ListFragment onViewCreated sharedViewModel.isFirstLaunch")
//            //посылаем 1 раз запрос на имя эффекта чтобы выставить правильный указатель
//            // при включении приложения при работающей ленте
//            Server(getString(R.string.baseUrl, binding.edIpEsp.text.toString()),
//                getString(R.string.postPosition, getString(R.string._position)),
//                this@ListFragment).start()
//        }

        //следим за изменением заголовка тулбара и показываем новый заголовок
        listViewModel.getToolbarTitle().observe(viewLifecycleOwner) {
            (activity as MainActivity).supportActionBar?.title = it
        }
        //следим за тем, что пришло от сервера и передаём в адаптер
        //чтобы выставить правильный указатель при включении приложения при работающей ленте
        sharedViewModel.getNameOfEffect().observe(viewLifecycleOwner) { newTag ->
            nameTag = newTag
        }
        //наблюдаем за IP адресом ESP, который получаем от ESP на телефон через DNS-SD
        sharedViewModel.getIpAddressEsp().observe(viewLifecycleOwner) { addressIp ->
            Log.d(TAG, "***ListFragment onViewCreated addressIp = $addressIp")
            if (binding.edIpEsp.text.toString() != addressIp) {
                binding.edIpEsp.setText(addressIp)
                Log.d(TAG, "ListFragment onViewCreated new ESP Ip = $addressIp")
            }
        }
        //наблюдаем за именем (SSID) сети ESP, который получаем от ESP через запрос к ESP после получения его Id
        sharedViewModel.getEspNetworkSsid().observe(viewLifecycleOwner) {espNetworkSsid->
            binding.nameEsp.text = espNetworkSsid
            Log.d(TAG, "### ListFragment onViewCreated SSID сети ESP = $espNetworkSsid")
        }
        //наблюдаем за IP адресом телефона - пока нет, так как пока неправильно определяется Ip телефона
//        sharedViewModel.getTlfIp().observe(viewLifecycleOwner) {tlfIp->
//            binding.edIpTlf.setText(tlfIp)
//           // Log.d(TAG, "### ListFragment onViewCreated Ip сети ТЛФ = $tlfIp")
//        }
        //наблюдаем за именем (SSID) сети телефона, который получаем от wifiManager и обновляем текстовое поле
        sharedViewModel.getTlfSSID().observe(viewLifecycleOwner) {tlfNetworkSsid->
            binding.nameTlf.text = tlfNetworkSsid
            //Log.d(TAG, "### ListFragment onViewCreated SSID сети ТЛФ = $tlfNetworkSsid")
        }

        initMenu()
    }

    private fun initMenu(){
        // The usage of an interface lets you inject your own implementation
            requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                // Add menu items here
                menuInflater.inflate(R.menu.main_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                // Handle the menu selection
                return when (menuItem.itemId) {
                    R.id.sync -> {
                        navController.navigate(R.id.action_listFragment_to_actionFragment)
                        true
                    }
                    R.id.info-> {
                        if(isShowInfo){
                            info.visibility = View.VISIBLE
                        }else{
                            info.visibility = View.GONE
                        }
                        isShowInfo = !isShowInfo
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "@@@ListFragment onResume")
        nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        updateSSIDandIP() //получаем название wifi и Ip сети телефона  от wifiManager
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "ListFragment onPause")
        if(binding.edIpEsp.text.isNotEmpty()){
            saveIp(binding.edIpEsp.text.toString())
        }
        nsdManager.stopServiceDiscovery(discoveryListener)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun getAdapterPosition():Int {
        val list:List<ListOfEffects>? = rvAdapter?.listOfEffects
        list?. let{
            for (item in it.indices) {
                if (it[item].name == nameTag) {
                    return item
                }
            }
        }
        return -1
    }

    private fun updateSSIDandIP() {
       val  wifiManager = requireActivity().getSystemService(Context.WIFI_SERVICE) as WifiManager
        val ssid = wifiManager.connectionInfo.ssid.removeSurrounding("\"")
        //не всегда ip идёт первым, поэтому это вызывает крэш и надо искать другой метод
//        //https://stackoverflow.com/questions/71003749/android-wifiinfo-getipaddress-deprecated
//        var ip:String? = ""
//        fun Context.getConnectivityManager() = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
//        fun getIpAddress(context: Context) = with(context.getConnectivityManager()) {
//            getLinkProperties(activeNetwork)!!.linkAddresses[1].address.hostAddress!!
//        }
//        ip = context?.let { getIpAddress(it) }
//        Log.d(TAG, "*#*# ListFragment updateSSIDandIP ТЛФ:    ssid= $ssid  networkId = $ip")
//        sharedViewModel.updateTlfIp(ip)

        when (ssid) {
            getString(R.string.localNetworkName) -> {
                sharedViewModel.updateTlfSSID(ssid)
                sharedViewModel.updateIpAddressEsp(getString(R.string.hotpointIp))
            }
            getString(R.string.unknownSsid) -> {
                sharedViewModel.updateTlfSSID(getString(R.string.network_name))
            }
            else -> {
                sharedViewModel.updateTlfSSID(ssid)
            }
        }
    }

    private fun getIp(){
        val ip = pref.getString(IP, "")
        if(ip != null){
            if(ip.isNotEmpty()) binding.edIpEsp.text = ip
        }
    }

    private fun saveIp(ip: String){
        val editor = pref.edit()
        editor.putString(IP, ip)
        editor.apply()
        sharedViewModel.updateIpAddressEsp(ip)
    }

    private fun  initAdapter() {
        Log.d(TAG, "ListFragment initAdapter nameTag = $nameTag")
        binding.rvEffects.layoutManager = LinearLayoutManager(requireActivity())
        nameTag?. let{rvAdapter = RvListAdapter(it, getOnClickListener())}
        binding. rvEffects.adapter = rvAdapter
        listViewModel.getListOfEffects().value?. let{rvAdapter?.listOfEffects = it}
    }

    private fun getOnClickListener(): RvListAdapter.OnItemClickListener =
        object : RvListAdapter.OnItemClickListener{
            override fun onClick(item: ListOfEffects) {
                nameTag = item.name
                listViewModel.updateTitle(item.name) //обновляем имя заголовка тулбара
                val baseUrl = String.format(getString(R.string.baseUrl), binding.edIpEsp.text.toString())
                when(item.name){
                    getString(R.string.staticColors) -> {
                        navigateTo(baseUrl,item, getString(R.string._static),R.id.action_listFragment_to_staticColorFragment)
                    }
                    getString(R.string.playOfColors) ->{
                        navigateTo(baseUrl,item,getString(R.string._play),R.id.action_listFragment_to_dynamicColorsFragment)
                    }
                    getString(R.string.playOfColorsFlash) ->{
                        navigateTo(baseUrl,item,getString(R.string._playFlash),R.id.action_listFragment_to_dynamicColorsFragment)
                    }
                    getString(R.string.running_lights) ->{
                        navigateTo(baseUrl,item,getString(R.string._runningLights),R.id.action_listFragment_to_runningColorsFragment)
                    }
                    getString(R.string.flame) ->{
                        navigateTo(baseUrl,item,getString(R.string._flame),R.id.action_listFragment_to_dynamicColorsFragment)
                    }
                    getString(R.string.pride) ->{
                        navigateTo(baseUrl,item,getString(R.string._pride),R.id.action_listFragment_to_dynamicColorsFragment)
                    }
                    getString(R.string.rainbow) ->{
                        navigateTo(baseUrl,item,getString(R.string._rainbow),R.id.action_listFragment_to_dynamicColorsFragment)
                    }
                    getString(R.string.rainbow_bias) ->{
                        navigateTo(baseUrl,item,getString(R.string._rainbowBias),R.id.action_listFragment_to_dynamicColorsFragment)
                    }
                    getString(R.string.rainbow_flash) ->{
                        navigateTo(baseUrl,item,getString(R.string._rainbowFlash),R.id.action_listFragment_to_dynamicColorsFragment)
                    }
                    getString(R.string.random_color) ->{
                        navigateTo(baseUrl,item,getString(R.string._random),R.id.action_listFragment_to_dynamicColorsFragment)
                    }
                    getString(R.string.random_color_fade) ->{
                        navigateTo(baseUrl,item,getString(R.string._randomFade),R.id.action_listFragment_to_dynamicColorsFragment)
                    }
                    getString(R.string.northern_lights) ->{
                        navigateTo(baseUrl,item,getString(R.string._northernLights),R.id.action_listFragment_to_dynamicColorsFragment)
                    }
                    getString(R.string.test) ->{
                        navigateTo(baseUrl,item,getString(R.string._test),R.id.action_listFragment_to_flameFragment)
                    }
                }
            }
        }

    private  fun navigateTo(baseUrl:String, item: ListOfEffects,keyword:String,  frag:Int ){
        val bundle = bundleOf(BASE_URL to baseUrl, ITEM_NAME to item.name,
            KEY_WORD to keyword )
        navController.navigate(frag, bundle)
    }

    override fun onMessage(message: String) {
        //сюда попадаем 1 раз - при первом включении - для установки позиции адаптера
        try {
            requireActivity().runOnUiThread {
                Log.d(TAG, "*****ListFragment onMessage message= $message")
                sharedViewModel.updateNameOfEffect(message)
                initAdapter()
                if (sharedViewModel.isFirstLaunch){
                    val position = getAdapterPosition()
                    Log.d(TAG, "###ListFragment onViewCreated  nameTag= $nameTag position = $position")
                    binding.rvEffects.scrollToPosition(position)
                    sharedViewModel.isNotFirstLaunch() //меняем флаг
                }
            }
        } catch (e:Exception){
            Log.d(TAG, "***##***ListFragment Exception message= ${e.message}")
        }
    }

    // Instantiate a new DiscoveryListener
    private val discoveryListener = object : NsdManager.DiscoveryListener {
        // Called as soon as service discovery begins.
        override fun onDiscoveryStarted(regType: String) {
            Log.d(TAG, "Service discovery started")
            requireActivity().runOnUiThread {
                sharedViewModel.updateEspNetworkSsid("?")
            }
        }
        override fun onServiceLost(service: NsdServiceInfo) {
            // When the network service is no longer available.
            // Internal bookkeeping code goes here.
            Log.d(TAG, "service lost: $service")
            Log.d(TAG, "-----------------------")
            //при потере сервиса выставляем звёздочки
            //runOnUiThread {model.updateIpAddressEsp("*.*.*.*") }
        }
        override fun onDiscoveryStopped(serviceType: String) {
            Log.d(TAG, "Discovery stopped: $serviceType")
            Log.d(TAG, "-----------------------")
            //при остановке сервиса выставляем звёздочки
            //runOnUiThread {model.updateIpAddressEsp("*.*.*.*") }
        }
        override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
            Log.d(TAG, "Discovery failed: Error code:$errorCode")
            nsdManager.stopServiceDiscovery(this)
        }
        override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
            Log.d(TAG, "Discovery failed: Error code:$errorCode")
            Log.d(TAG, "-----------------------")
            nsdManager.stopServiceDiscovery(this)
        }

        override fun onServiceFound(service: NsdServiceInfo) {
            // A service was found! Do something with it.
            Log.d(TAG, "Service discovery success   $service")
            if (service.serviceName.contains(espName)){
                Log.d(TAG, "serviceName.contains(\"esp8266\")")
                nsdManager.resolveService(service, object : NsdManager.ResolveListener {
                    override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                        // Called when the resolve fails. Use the error code to debug.
                        Log.d(TAG, "Resolve failed: $errorCode")
                    }
                    override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                        Log.d(TAG, "Resolve Succeeded. $serviceInfo")
                        val port: Int = serviceInfo.port
                        val ip: String? = serviceInfo.host.hostAddress
                        val serviceName = serviceInfo.serviceName
                        val attr = serviceInfo.attributes
                        Log.d(TAG, "port = $port ip = $ip serviceName = $serviceName   attr = $attr")
                        Log.d(TAG, "-----------------------")

                        requireActivity().runOnUiThread {
                            try {
                                ip?.let {
                                    Log.d(TAG, "runOnUiThread ip =$ip")
                                    sharedViewModel.updateIpAddressEsp(it)
                                    val zapros =getString(R.string.baseUrl, it)+ getString(R.string.postPosition, getString(R.string._esp))
                                    Log.d(TAG, "runOnUiThread zapros =$zapros")
                                    //посылаем запрос на ESP на имя сети, к которой подключена ESP
                                    Server(getString(R.string.baseUrl, it),
                                        getString(R.string.postPosition, getString(R.string._esp)),
                                        object : Server.MessageListener{
                                            override fun onMessage(message: String) {
                                                sharedViewModel.updateEspNetworkSsid(message)
                                            }
                                        }).start()
                                }
                            }catch (e:Exception){
                                Log.d(TAG, "@@@ListFragment onServiceResolved error = ${e.message} ")
                            }
                        }
                    }
                })

            }
        }
    }
}
