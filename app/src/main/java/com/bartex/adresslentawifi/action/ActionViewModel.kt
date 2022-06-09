package com.bartex.adresslentawifi.action

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.bartex.adresslentawifi.App
import com.bartex.adresslentawifi.entity.ListOfActions

class ActionViewModel: ViewModel() {
    private val listOfActions = MutableLiveData<List<ListOfActions>>()
    private val actionRepo: IActionsList = ActionsListImpl(App.instance)


    fun getListOfActions(): LiveData<List<ListOfActions>> {
        return listOfActions
    }

    fun loadData(){
        listOfActions.value = actionRepo.getActionsList()
    }
}