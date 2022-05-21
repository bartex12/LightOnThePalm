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
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bartex.adresslentawifi.R
import com.bartex.adresslentawifi.Server
import com.bartex.adresslentawifi.adapter.RvAdapter
import com.bartex.adresslentawifi.constants.Constants.BASE_URL
import com.bartex.adresslentawifi.constants.Constants.IP
import com.bartex.adresslentawifi.constants.Constants.ITEM_NAME
import com.bartex.adresslentawifi.constants.Constants.KEY_WORD
import com.bartex.adresslentawifi.constants.Constants.TAG
import com.bartex.adresslentawifi.entity.ListOfEffects

class ListFragment : Fragment(), Server.MessageListener {

    lateinit var navController: NavController
    private val listViewModel: ListViewModel by lazy {
        ViewModelProvider(this).get(ListViewModel::class.java)
    }
    private var rvAdapter: RvAdapter? = null
    private  lateinit var rvEffects: RecyclerView
    private  lateinit var bSave: Button
    private  lateinit var edIp:EditText
    private lateinit var pref: SharedPreferences


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "ListFragment onViewCreated ")
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
        rvAdapter =  RvAdapter(getOnClickListener())
        rvEffects.adapter = rvAdapter
    }

    private fun getOnClickListener(): RvAdapter.OnItemClickListener =
        object : RvAdapter.OnItemClickListener{
            override fun onClick(item: ListOfEffects) {
                val baseUrl = getString(R.string.baseUrl, edIp.text)
                when(item.name){
                    getString(R.string.staticColors) -> {
                        val bundle = bundleOf(BASE_URL to baseUrl, ITEM_NAME to item.name,
                            KEY_WORD to getString(R.string._static)) //так проще
                        navController.navigate(R.id.staticColorFragment, bundle)
                    }
                    getString(R.string.playOfColors) ->{
                        val bundle = bundleOf(BASE_URL to baseUrl, ITEM_NAME to item.name,
                             KEY_WORD to getString(R.string._play))
                        navController.navigate(R.id.dynamicColorsFragment, bundle)
                    }

                    getString(R.string.flame) ->
                        Server(baseUrl, getString(R.string._flame),this@ListFragment).start()

                    getString(R.string.pride) ->{
                        val bundle = bundleOf(BASE_URL to baseUrl, ITEM_NAME to item.name,
                            KEY_WORD to getString(R.string._pride))
                        navController.navigate(R.id.dynamicColorsFragment, bundle)
                    }
                    getString(R.string.rainbow) ->{
                        val bundle = bundleOf(BASE_URL to baseUrl, ITEM_NAME to item.name,
                            KEY_WORD to getString(R.string._rainbow))
                        navController.navigate(R.id.dynamicColorsFragment, bundle)
                    }
                    getString(R.string.rainbow_bias) ->{
                        val bundle = bundleOf(BASE_URL to baseUrl, ITEM_NAME to item.name,
                            KEY_WORD to getString(R.string._rainbowBias))
                        navController.navigate(R.id.dynamicColorsFragment, bundle)
                    }
                    getString(R.string.random_color) ->{
                        val bundle = bundleOf(BASE_URL to baseUrl, ITEM_NAME to item.name,
                            KEY_WORD to getString(R.string._random))
                        navController.navigate(R.id.dynamicColorsFragment, bundle)
                    }
                    getString(R.string.test) ->{
                        val bundle = bundleOf(BASE_URL to baseUrl)
                        navController.navigate(R.id.testColorFragment, bundle)
                    }
                }
            }
        }

    override fun onMessage(message: String) {
        requireActivity().runOnUiThread {
            Log.d(TAG, "ListFragment onMessage message= $message")
        }
    }

}