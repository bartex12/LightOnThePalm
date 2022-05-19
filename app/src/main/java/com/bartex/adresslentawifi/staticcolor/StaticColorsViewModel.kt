package com.bartex.adresslentawifi.staticcolor

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.bartex.adresslentawifi.App
import com.bartex.adresslentawifi.entity.ButtonColor

class StaticColorsViewModel:ViewModel() {

    private val colorList = MutableLiveData<List<ButtonColor>>()
    private val staticColorRepo: IColorList =  ColorListImpl(App.instance)

    fun getStaticColors():LiveData<List<ButtonColor>>{
    return colorList
    }

    fun loadStaticColors(){
        colorList.value = staticColorRepo.getList()
    }
}