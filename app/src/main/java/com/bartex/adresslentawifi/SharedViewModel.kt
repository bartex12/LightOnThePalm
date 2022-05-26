package com.bartex.adresslentawifi

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.bartex.adresslentawifi.constants.Constants.TAG

class SharedViewModel: ViewModel() {

    private val nameOfEffect = MutableLiveData<String>()

    fun getNameOfEffect():LiveData<String>{
        Log.d(TAG, "SharedViewModel getNameOfEffect message= ${nameOfEffect.value}")
        return nameOfEffect
    }

    fun updateNameOfEffect(name:String){
        nameOfEffect.value = name
        Log.d(TAG, "SharedViewModel updateNameOfEffect message= ${nameOfEffect.value}")
    }
}