package com.bartex.adresslentawifi.list

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.bartex.adresslentawifi.App
import com.bartex.adresslentawifi.constants.Constants
import com.bartex.adresslentawifi.entity.ListOfEffects

class ListViewModel:ViewModel() {

    private val listOfEffects = MutableLiveData<List<ListOfEffects>>()
    private val mainRepo: IEffectsList =  EffectsListImpl(App.instance)
    private val toolbarTitle = MutableLiveData<String>("Цвет на ладони")

    fun getListOfEffects(): LiveData<List<ListOfEffects>> {
        return listOfEffects
    }

    fun loadData(){
        listOfEffects.value = mainRepo.getList()
    }

    fun updateTitle(newTitle:String){
        toolbarTitle.value = newTitle
        Log.d(Constants.TAG, "SharedViewModel updateTitle: toolbarTitle.value = ${toolbarTitle.value}")
    }

    fun getToolbarTitle():LiveData<String>{
        return toolbarTitle
    }

}