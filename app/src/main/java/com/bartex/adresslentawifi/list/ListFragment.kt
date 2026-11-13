package com.bartex.adresslentawifi.list

import android.content.Context
import android.content.SharedPreferences
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
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
import com.bartex.adresslentawifi.EspApi
import com.bartex.adresslentawifi.EspLink
import com.bartex.adresslentawifi.LocalMode
import com.bartex.adresslentawifi.showMessage
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


class   ListFragment : Fragment() {

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
//            EspApi.get(getString(R.string.baseUrl, binding.edIpEsp.text.toString()),
//                getString(R.string._position), onSuccess = { onPositionReceived(it) })
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

    //Лента ответила как точка доступа NodeMCU, хотя приложение к ней не подключалось.
    //Значит, телефон подключился к NodeMCU сам - эта сеть сохранена в настройках WiFi
    //телефона. Тогда "Отключиться от NodeMCU" не помогает: система тут же подключается
    //обратно. Удалить чужую сохранённую сеть приложение не может - подсказываем один раз.
    private fun hintIfSystemOnNodeMcu(ip: String, espSsid: String) {
        Log.d(TAG, "hintIfSystemOnNodeMcu ip=$ip ssid=[$espSsid] " +
                "shown=${sharedViewModel.savedNodeMcuHintShown} link=${EspLink.state.value}")
        if (!isAdded || !EspLink.isSupported || sharedViewModel.savedNodeMcuHintShown) return
        if (ip == getString(R.string.hotpointIp) && espSsid.trim() == getString(R.string.localNetworkName)) {
            sharedViewModel.savedNodeMcuHintShown = true
            showMessage(R.string.savedNodeMcuHint)
        }
    }

    //пункт меню: подключить телефон к точке доступа ленты NodeMCU или отключить от неё
    //(сообщения о результате показывает MainActivity)
    private fun toggleDirectLink() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        val context = requireContext()
        when (EspLink.state.value) {
            EspLink.State.CONNECTING, EspLink.State.CONNECTED -> {
                LocalMode.setEnabled(context, false) //больше не подключаться автоматически
                EspLink.release(context)
            }
            else -> {
                LocalMode.setEnabled(context, true)  //и при следующих запусках тоже
                LocalMode.autoConnectBlocked = true  //подключаемся сами, прямо сейчас
                LocalMode.connect(context)
            }
        }
    }

    private fun initMenu(){
        // The usage of an interface lets you inject your own implementation
            requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                // Add menu items here
                menuInflater.inflate(R.menu.main_menu, menu)
            }

            override fun onPrepareMenu(menu: Menu) {
                val item = menu.findItem(R.id.directLink) ?: return
                item.isVisible = EspLink.isSupported //только Android 10+
                val linked = EspLink.state.value == EspLink.State.CONNECTING ||
                        EspLink.state.value == EspLink.State.CONNECTED
                item.setTitle(if (linked) R.string.directLinkDisconnect else R.string.directLinkConnect)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                // Handle the menu selection
                return when (menuItem.itemId) {
                    R.id.directLink -> {
                        toggleDirectLink()
                        true
                    }
                    R.id.sync -> {
                        navController.navigate(R.id.action_listFragment_to_actionFragment)
                        true
                    }
                    R.id.info-> {
                        //Log.d(TAG, "### ListFragment onMenuItemSelected isShowInfo = $isShowInfo")
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
        isShowInfo = true //чтобы при возврате из эффекта по щелчку открыть инфо
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

    //ответ ESP на запрос позиции - для установки позиции адаптера при первом включении
    //(EspApi вызывает колбэки в главном потоке)
    private fun onPositionReceived(message: String) {
        if (!isAdded || _binding == null) return
        Log.d(TAG, "*****ListFragment onPositionReceived message= $message")
        sharedViewModel.updateNameOfEffect(message)
        initAdapter()
        if (sharedViewModel.isFirstLaunch){
            val position = getAdapterPosition()
            binding.rvEffects.scrollToPosition(position)
            sharedViewModel.isNotFirstLaunch() //меняем флаг
        }
    }

    // Instantiate a new DiscoveryListener
    private val discoveryListener = object : NsdManager.DiscoveryListener {
        // Called as soon as service discovery begins.
        override fun onDiscoveryStarted(regType: String) {
            Log.d(TAG, "Service discovery started")
            sharedViewModel.updateEspNetworkSsid("?") //внутри postValue - можно из любого потока
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

                        //колбэк NSD приходит не в главном потоке, а фрагмент к этому
                        //моменту может быть уже закрыт - тогда activity == null
                        activity?.runOnUiThread {
                            if (!isAdded || ip == null) return@runOnUiThread
                            Log.d(TAG, "runOnUiThread ip =$ip")
                            sharedViewModel.updateIpAddressEsp(ip)
                            //посылаем запрос на ESP на имя сети, к которой подключена ESP
                            val viaDirectLink = EspLink.network != null
                            EspApi.get(getString(R.string.baseUrl, ip), getString(R.string._esp),
                                onSuccess = {
                                    sharedViewModel.updateEspNetworkSsid(it)
                                    if (!viaDirectLink) hintIfSystemOnNodeMcu(ip, it)
                                })
                        }
                    }
                })

            }
        }
    }
}
