package com.bartex.adresslentawifi.list

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.bartex.adresslentawifi.App
import com.bartex.adresslentawifi.R
import com.bartex.adresslentawifi.entity.ListOfEffects

class ListViewModel:ViewModel() {

    private val listOfEffects = MutableLiveData<List<ListOfEffects>>()
    private val mainRepo: IEffectsList =  EffectsListImpl(App.instance)
    private val toolbarTitle = MutableLiveData(App.instance.getString(R.string.ligthOnHand))
    private val listPosition = MutableLiveData<Int>()


    fun getListOfEffects(): LiveData<List<ListOfEffects>> {
        loadData()
        return listOfEffects
    }

    private fun loadData(){
        listOfEffects.value = mainRepo.getList()
    }

    fun updateTitle(newTitle:String){
        toolbarTitle.value = newTitle
    }

    fun getToolbarTitle():LiveData<String>{
        return toolbarTitle
    }

    fun updateListPosition(position:Int){
        listPosition.value = position
    }

    fun getListPosition():LiveData<Int>{
        return listPosition
    }
}