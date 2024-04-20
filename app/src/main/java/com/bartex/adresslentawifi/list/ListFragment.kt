package com.bartex.adresslentawifi.list

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
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


class   ListFragment : Fragment(), Server.MessageListener {

    private var _binding:FragmentListBinding? = null
    private val binding get() = _binding!!

    private val listViewModel: ListViewModel by activityViewModels()
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private lateinit var navController: NavController
    private lateinit var pref: SharedPreferences
    private var rvAdapter: RvListAdapter? = null

    private var nameTag:String? = ""

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

        getIp()  //получаем ip адрес из преференсис - если лента выключена, будет старый сохранённый ip

        initAdapter()  //иначе при отключенной ленте будет пустой экран
        if (sharedViewModel.isFirstLaunch){
            Log.d(TAG, "ListFragment onViewCreated sharedViewModel.isFirstLaunch")
            //посылаем 1 раз запрос на имя эффекта чтобы выставить правильный указатель
            // при включении приложения при работающей ленте
            Server(getString(R.string.baseUrl, binding.edIpEsp.text.toString()),
                getString(R.string.postPosition, getString(R.string._position)),
                this@ListFragment).start()
        }

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
        //наблюдаем за SSID сети ESP, который получаем от ESP через запрос к ESP после получения его Id
        sharedViewModel.getEspNetworkSsid().observe(viewLifecycleOwner) {espNetworkSsid->
            binding.nameEsp.text = espNetworkSsid
            Log.d(TAG, "### ListFragment onViewCreated SSID сети ESP = $espNetworkSsid")
        }
        sharedViewModel.getTlfIp().observe(viewLifecycleOwner) {tlfIp->
            binding.edIpTlf.setText(tlfIp)
           // Log.d(TAG, "### ListFragment onViewCreated Ip сети ТЛФ = $tlfIp")
        }
        //наблюдаем за SSID сети телефона, который получаем от wifiManager и обновляем текстовое поле
        sharedViewModel.getTlfSSID().observe(viewLifecycleOwner) {tlfNetworkSsid->
            binding.nameTlf.text = tlfNetworkSsid
            //Log.d(TAG, "### ListFragment onViewCreated SSID сети ТЛФ = $tlfNetworkSsid")
        }

        //приводим меню тулбара в соответствии с onPrepareOptionsMenu в MainActivity
        setHasOptionsMenu(true)
}

    override fun onResume() {
        super.onResume()
        updateSSIDandIP() //получаем название wifi сети от wifiManager
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "ListFragment onPause")
        if(binding.edIpEsp.text.isNotEmpty()){
            saveIp(binding.edIpEsp.text.toString())
        }
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
                sharedViewModel.updateTlfSSID(getString(R.string.wf_network))
            }
            else -> {
                sharedViewModel.updateTlfSSID(ssid)
            }
        }
    }

    private fun getIp(){
        val ip = pref.getString(IP, "")
        if(ip != null){
            if(ip.isNotEmpty()) binding.edIpEsp.setText(ip)
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
        } catch (e:IllegalStateException){
            Log.d(TAG, "ListFragment Exception message= ${e.message}")
        }
    }
}
