package com.bartex.adresslentawifi.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.bartex.adresslentawifi.entity.ListOfEffects

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val listOfEffects = MutableLiveData<List<ListOfEffects>>()
    private val mainRepo:IEffectsList =  EffectsListImpl(application)

    fun getListOfEffects(): LiveData<List<ListOfEffects>> {
        return listOfEffects
    }

    fun loadData(){
        listOfEffects.value = mainRepo.getList()
    }
}