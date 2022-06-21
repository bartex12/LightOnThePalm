package com.bartex.adresslentawifi.list

import android.content.Context
import android.content.SharedPreferences
import android.net.wifi.WifiManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bartex.adresslentawifi.R
import com.bartex.adresslentawifi.Server
import com.bartex.adresslentawifi.SharedViewModel
import com.bartex.adresslentawifi.adapter.RvAdapter
import com.bartex.adresslentawifi.constants.Constants.BASE_URL
import com.bartex.adresslentawifi.constants.Constants.IP
import com.bartex.adresslentawifi.constants.Constants.ITEM_NAME
import com.bartex.adresslentawifi.constants.Constants.KEY_WORD
import com.bartex.adresslentawifi.constants.Constants.TAG
import com.bartex.adresslentawifi.databinding.FragmentFlameBinding
import com.bartex.adresslentawifi.databinding.FragmentListBinding
import com.bartex.adresslentawifi.entity.ListOfEffects
import com.bartex.adresslentawifi.main.MainActivity
import java.util.*

class   ListFragment : Fragment(), Server.MessageListener {

    private var _binding:FragmentListBinding? = null
    private val binding get() = _binding!!

    private val listViewModel: ListViewModel by activityViewModels()
    private val model: SharedViewModel by activityViewModels()
    private lateinit var wifiManager: WifiManager
    private lateinit var navController: NavController
    private lateinit var pref: SharedPreferences
    private var rvAdapter: RvAdapter? = null

    private var nameTag:String? = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentListBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        pref = requireActivity().getSharedPreferences("MyPref", AppCompatActivity.MODE_PRIVATE)
        navController = Navigation.findNavController(view)

        getIp()
        initAdapter()  //иначе при отключенной ленте будет пустой экран
        if (model.isFirstLaunch){
            Log.d(TAG, "ListFragment onViewCreated model.isFirstLaunch")
            //посылаем 1 раз запрос на имя эффекта чтобы выставить правильный указатель
            // при включении приложения при работающей ленте
            Server(getString(R.string.baseUrl, binding.edIp.text.toString()),
                getString(R.string.postPosition, getString(R.string._position)),
                this@ListFragment).start()
        }
        updateSSID()

        //следим за изменением заголовка тулбара и показываем новый заголовок
        listViewModel.getToolbarTitle().observe(this, {
            (activity as MainActivity).supportActionBar?.title = it
        })
        //следим за тем, что пришло от сервера и передаём в адаптер
        //чтобы выставить правильный указатель при включении приложения при работающей ленте
        model.getNameOfEffect().observe(viewLifecycleOwner, { newTag ->
            nameTag = newTag
        })

        //наблюдаем за WIFI_IP адресом, который получаем от ESP через MQTT сервер broker.hivemq.com
        model.getIpAddress().observe(viewLifecycleOwner, {addressIp->
            Log.d(TAG, "***ListFragment onViewCreated addressIp = $addressIp")
            if (binding.edIp.text.toString()!=addressIp){
                binding.edIp.setText(addressIp)
                Log.d(TAG, "ListFragment onViewCreated new Ip = $addressIp")
            }
        })
        //наблюдаем за SSID и обновляем текстовое поле
        model.getSSID().observe(viewLifecycleOwner,{
            binding.tvNameList.text = it
        })
        //приводим меню тулбара в соответствии с onPrepareOptionsMenu в MainActivity
        setHasOptionsMenu(true)
}

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "ListFragment onPause")
        if(binding.edIp.text.isNotEmpty()){
            saveIp(binding.edIp.text.toString())
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

    private fun updateSSID() {
        wifiManager = requireActivity().getSystemService(Context.WIFI_SERVICE) as WifiManager
        val ssid = wifiManager.connectionInfo.ssid.removeSurrounding("\"")
        model.updateSSID(ssid)
        if (ssid == getString(R.string.localNetworkName)){
        model.updateIpAddress(getString(R.string.hotpointIp))
        }
    }

    private fun getIp(){
        val ip = pref.getString(IP, "")
        if(ip != null){
            if(ip.isNotEmpty()) binding.edIp.setText(ip)
        }
    }

    private fun saveIp(ip: String){
        val editor = pref.edit()
        editor.putString(IP, ip)
        editor.apply()
        model.updateIpAddress(ip)
    }

    private fun  initAdapter() {
        Log.d(TAG, "ListFragment initAdapter nameTag = $nameTag")
        binding.rvEffects.layoutManager = LinearLayoutManager(requireActivity())
        nameTag?. let{rvAdapter = RvAdapter(it, getOnClickListener())}
        binding. rvEffects.adapter = rvAdapter
        listViewModel.getListOfEffects().value?. let{rvAdapter?.listOfEffects = it}
    }

    private fun getOnClickListener(): RvAdapter.OnItemClickListener =
        object : RvAdapter.OnItemClickListener{
            override fun onClick(item: ListOfEffects) {
                nameTag = item.name
                listViewModel.updateTitle(item.name) //обновляем имя заголовка тулбара
                val baseUrl = String.format(getString(R.string.baseUrl), binding.edIp.text.toString())
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
                model.updateNameOfEffect(message)
                initAdapter()
                if (model.isFirstLaunch){
                    val position = getAdapterPosition()
                    Log.d(TAG, "###ListFragment onViewCreated  nameTag= $nameTag position = $position")
                    binding.rvEffects.scrollToPosition(position)
                    model.isNotFirstLaunch() //меняем флаг
                }
            }
            } catch (e:IllegalStateException){
                Log.d(TAG, "ListFragment Exception message= $e.message")
            }
        }
    }
