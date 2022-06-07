package com.bartex.adresslentawifi

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SharedViewModel: ViewModel() {

    private val nameOfEffect = MutableLiveData<String>()
    private val ipAddress = MutableLiveData<String>()
    private val ssidNetwork = MutableLiveData<String>()

    fun getNameOfEffect():LiveData<String>{
        //Log.d(TAG, "SharedViewModel getNameOfEffect message= ${nameOfEffect.value}")
        return nameOfEffect
    }

    fun updateNameOfEffect(name:String){
        nameOfEffect.value = name
        //Log.d(TAG, "SharedViewModel updateNameOfEffect message= ${nameOfEffect.value}")
    }

    fun updateIpAddress(addressIp: String) {
        ipAddress.value = addressIp
    }

    fun getIpAddress():LiveData<String>{
        return ipAddress
    }

    fun updateSSID(ssid: String) {
        ssidNetwork.value = ssid
    }

    fun getSSID():LiveData<String>{
        return ssidNetwork
    }
}