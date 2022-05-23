package com.bartex.adresslentawifi

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.bartex.adresslentawifi.constants.Constants.TAG

class SharedViewModel: ViewModel() {

    private val toolbarTitle = MutableLiveData<String>("Цвет на ладони")

    fun updateTitle(newTitle:String){
        toolbarTitle.value = newTitle
        Log.d(TAG, "SharedViewModel updateTitle: toolbarTitle.value = ${toolbarTitle.value}")
    }

    fun getToolbarTitle():LiveData<String>{
        return toolbarTitle
    }
}