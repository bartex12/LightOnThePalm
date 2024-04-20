package com.bartex.adresslentawifi

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SharedViewModel: ViewModel() {

    private val nameOfEffect = MutableLiveData<String>()
    private val ipAddressEsp = MutableLiveData<String>()
    private val ssidNetworkEsp = MutableLiveData<String>()
    private val ipAddressTlf = MutableLiveData<String>()
    private val ssidNetworkTlf = MutableLiveData<String>()

    var isFirstLaunch = true //флаг первого запуска

    fun getNameOfEffect():LiveData<String>{
        return nameOfEffect
    }

    fun updateNameOfEffect(name:String){
        nameOfEffect.value = name
    }

    fun updateIpAddressEsp(addressIp: String) {
        ipAddressEsp.value = addressIp
    }

    fun getIpAddressEsp():LiveData<String>{
        return ipAddressEsp
    }

    fun updateTlfSSID(ssid: String) {
        ssidNetworkTlf.value = ssid
    }

    fun getTlfSSID():LiveData<String>{
        return ssidNetworkTlf
    }

    fun isNotFirstLaunch() {
        isFirstLaunch = false
    }

//postValue так как запрос сервера делается в новом потоке
    fun updateEspNetworkSsid(espNetworkName: String) {
        ssidNetworkEsp.postValue(espNetworkName)
    }

    fun getEspNetworkSsid():LiveData<String>{
        return ssidNetworkEsp
    }

    fun updateTlfIp(ip: String?) {
        ipAddressTlf.value = ip
    }

    fun getTlfIp():LiveData<String>{
        return ipAddressTlf
    }

}