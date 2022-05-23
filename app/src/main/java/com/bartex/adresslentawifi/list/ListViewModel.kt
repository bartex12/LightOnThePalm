package com.bartex.adresslentawifi.list

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.bartex.adresslentawifi.App
import com.bartex.adresslentawifi.SharedViewModel
import com.bartex.adresslentawifi.entity.ListOfEffects

class ListViewModel:ViewModel() {

    private val listOfEffects = MutableLiveData<List<ListOfEffects>>()
    private val mainRepo: IEffectsList =  EffectsListImpl(App.instance)

    fun getListOfEffects(): LiveData<List<ListOfEffects>> {
        return listOfEffects
    }

    fun loadData(){
        listOfEffects.value = mainRepo.getList()
    }

}