package com.bartex.adresslentawifi.running

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.bartex.adresslentawifi.App
import com.bartex.adresslentawifi.dynamiccolor.IPreferenceHelper
import com.bartex.adresslentawifi.dynamiccolor.PreferenceHelper
import com.bartex.adresslentawifi.entity.ButtonColor
import com.bartex.adresslentawifi.staticcolor.ColorListImpl
import com.bartex.adresslentawifi.staticcolor.IColorList

class RunningViewModel: ViewModel() {
    private val helper: IPreferenceHelper = PreferenceHelper(App.instance)

    private val colorList = MutableLiveData<List<ButtonColor>>()
    private val staticColorRepo: IColorList =  ColorListImpl(App.instance)

    fun getStaticColors(): LiveData<List<ButtonColor>> {
        return colorList
    }

    fun loadStaticColors(){
        colorList.value = staticColorRepo.getShortList()
    }

    fun saveSpeedBrightness(nameOfEffect: String, speed: String?, brightness: String?) {
        helper.saveSpeedBrightness(nameOfEffect, speed, brightness)
    }

    fun saveNumbers(nameOfEffect: String, numbers: String?) {
        helper.saveNumbers(nameOfEffect, numbers)
    }

    fun saveColor(nameOfEffect: String, color: String?) {
        helper.saveColor(nameOfEffect, color)
    }

    fun getSpeed(nameOfEffect: String): String? {
        return helper.getSpeed(nameOfEffect)
    }

    fun getBrightness(nameOfEffect: String): String? {
        return helper.getBrightness(nameOfEffect)
    }

    fun getNumbers(nameOfEffect: String): String? {
        return helper.getNumbers(nameOfEffect)
    }

    fun getColor(nameOfEffect: String): String? {
        return helper.getColor(nameOfEffect)
    }
}