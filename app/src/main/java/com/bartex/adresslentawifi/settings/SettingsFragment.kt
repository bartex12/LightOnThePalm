package com.bartex.adresslentawifi.settings

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.bartex.adresslentawifi.R
import com.bartex.adresslentawifi.SharedViewModel
import com.bartex.adresslentawifi.constants.Constants.WIFI_IP
import com.bartex.adresslentawifi.constants.Constants.WIFI_NAME
import com.bartex.adresslentawifi.constants.Constants.WIFI_PASSWORD

class SettingsFragment: Fragment() {

    private  lateinit var saveIpSet: Button
    private  lateinit var saveIpNameSet: Button
    private  lateinit var saveIpPassSet: Button
    private  lateinit var edIpSet: EditText
    private  lateinit var edIpNameSet: EditText
    private  lateinit var edIpPassSet: EditText
    private  lateinit var tvInfoSet: TextView
    private lateinit var pref: SharedPreferences
    private val model: SharedViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return LayoutInflater.from(container?.context)
            .inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        pref = requireActivity().getSharedPreferences("MyPref", AppCompatActivity.MODE_PRIVATE)
        initView(view)
        initClickListeners()

        //приводим меню тулбара в соответствии с onPrepareOptionsMenu в MainActivity
        setHasOptionsMenu(true)
    }

    private fun initClickListeners() {
        saveIpSet.setOnClickListener {
            if(edIpSet.text.isNotEmpty()){
                saveValue(WIFI_IP, edIpSet.text.toString())
            }
        }
        saveIpNameSet.setOnClickListener {
            if(edIpNameSet.text.isNotEmpty()){
                saveValue(WIFI_NAME, edIpNameSet.text.toString())
            }
        }
        saveIpPassSet.setOnClickListener {
            if(edIpPassSet.text.isNotEmpty()){
                saveValue(WIFI_PASSWORD, edIpPassSet.text.toString())
            }
        }
    }

    private fun saveValue(const:String, value: String) {
        val editor = pref.edit()
        editor.putString(const, value)
        editor.apply()
        if (const == WIFI_IP ){
            model.updateIpAddress(value)
        }
    }

    private fun initView(view: View) {
        saveIpSet = view.findViewById(R.id.saveIpSet)
        saveIpNameSet = view.findViewById(R.id.saveIpNameSet)
        saveIpPassSet = view.findViewById(R.id.savePass_home)

        edIpSet = view.findViewById(R.id.edIpSet)
        edIpNameSet = view.findViewById(R.id.edIpNameSet)
        edIpPassSet = view.findViewById(R.id.edPass_home)

        tvInfoSet = view.findViewById(R.id.tvInfoSet)
    }
}