package com.bartex.adresslentawifi.list

import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
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
import com.bartex.adresslentawifi.entity.ListOfEffects
import com.bartex.adresslentawifi.main.MainActivity

class ListFragment : Fragment(), Server.MessageListener {

    private val listViewModel: ListViewModel by activityViewModels()
    private val model: SharedViewModel by activityViewModels()

    private lateinit var navController: NavController
    private var rvAdapter: RvAdapter? = null
    private  lateinit var rvEffects: RecyclerView
    private  lateinit var bSave: Button
    private  lateinit var edIp:EditText
    private lateinit var pref: SharedPreferences
    private var nameTag:String? = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        pref = requireActivity().getSharedPreferences("MyPref", AppCompatActivity.MODE_PRIVATE)
        navController = Navigation.findNavController(view)

        initViews(view)
        getIp()
        onClickSaveIp()
        initAdapter()

        listViewModel.loadData()
        listViewModel.getListOfEffects().observe(this, {
            renderData(it)
        })
        //следим за изменением заголовка тулбара и показываем новый заголовок
        listViewModel.getToolbarTitle().observe(this, {
            (activity as MainActivity).supportActionBar?.title = it
        })
        //следим за тем, что пришло от сервера и передаём в адаптер
        //это нужно для того, чтобы синхронизировать работу нескольких телефонов
        // - на экранах будет одинаковая картинка
        model.getNameOfEffect().observe(viewLifecycleOwner, {newTag->
            Log.d(TAG, "ListFragment onViewCreated newTag= $newTag")
            rvAdapter =  RvAdapter(newTag, getOnClickListener())
        })

        model.getIpAddress().observe(viewLifecycleOwner, {addressIp->
            Log.d(TAG, "ListFragment onViewCreated addressIp = $addressIp")
        })
}

    override fun onPause() {
        super.onPause()
        if(edIp.text.isNotEmpty()){
            saveIp(edIp.text.toString())
        }
    }
    private fun getIp(){
        val ip = pref.getString(IP, "")
        if(ip != null){
            if(ip.isNotEmpty()) edIp.setText(ip)
        }
    }

    private fun onClickSaveIp(){
        bSave.setOnClickListener {
            if(edIp.text.isNotEmpty()){
                saveIp(edIp.text.toString())
            }
        }
    }

    private fun saveIp(ip: String){
        val editor = pref.edit()
        editor.putString(IP, ip)
        editor.apply()
    }

    private fun renderData(data: List<ListOfEffects>?) {
        data?. let{ rvAdapter?.listOfEffects = it}
    }

    private fun initViews(view: View) {
        rvEffects = view.findViewById(R.id.rv_effects)
        bSave = view.findViewById(R.id.bSave)
        edIp = view.findViewById(R.id.edIp)
    }

    private fun initAdapter() {
        rvEffects.layoutManager = LinearLayoutManager(requireActivity())
        nameTag?. let{rvAdapter = RvAdapter(it, getOnClickListener())}
        rvEffects.adapter = rvAdapter
    }

    private fun getOnClickListener(): RvAdapter.OnItemClickListener =
        object : RvAdapter.OnItemClickListener{
            override fun onClick(item: ListOfEffects) {
                nameTag = item.name
                listViewModel.updateTitle(item.name) //обновляем имя заголовка тулбара
                val baseUrl = getString(R.string.baseUrl, edIp.text)
                when(item.name){
                    getString(R.string.staticColors) -> {
                        navigateTo(baseUrl,item, getString(R.string._static),R.id.staticColorFragment)
                    }
                    getString(R.string.playOfColors) ->{
                        navigateTo(baseUrl,item,getString(R.string._play),R.id.dynamicColorsFragment)
                    }
                    getString(R.string.playOfColorsFlash) ->{
                        navigateTo(baseUrl,item,getString(R.string._playFlash),R.id.dynamicColorsFragment)
                    }
                    getString(R.string.running_lights) ->{
                        navigateTo(baseUrl,item,getString(R.string._runningLights),R.id.runningColorsFragment)
                    }
                    getString(R.string.flame) ->{
                        navigateTo(baseUrl,item,getString(R.string._flame),R.id.dynamicColorsFragment)
                    }
                    getString(R.string.pride) ->{
                        navigateTo(baseUrl,item,getString(R.string._pride),R.id.dynamicColorsFragment)
                    }
                    getString(R.string.rainbow) ->{
                        navigateTo(baseUrl,item,getString(R.string._rainbow),R.id.dynamicColorsFragment)
                    }
                    getString(R.string.rainbow_bias) ->{
                        navigateTo(baseUrl,item,getString(R.string._rainbowBias),R.id.dynamicColorsFragment)
                    }
                    getString(R.string.rainbow_flash) ->{
                        navigateTo(baseUrl,item,getString(R.string._rainbowFlash),R.id.dynamicColorsFragment)
                    }
                    getString(R.string.random_color) ->{
                        navigateTo(baseUrl,item,getString(R.string._random),R.id.dynamicColorsFragment)
                    }
                    getString(R.string.random_color_fade) ->{
                        navigateTo(baseUrl,item,getString(R.string._randomFade),R.id.dynamicColorsFragment)
                    }
                    getString(R.string.northern_lights) ->{
                        navigateTo(baseUrl,item,getString(R.string._northernLights),R.id.dynamicColorsFragment)
                    }
                    getString(R.string.test) ->{
                        navigateTo(baseUrl,item,getString(R.string._test),R.id.flameFragment)
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
        requireActivity().runOnUiThread {
            Log.d(TAG, "*****ListFragment onMessage message= $message")
        }
    }

}