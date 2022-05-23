package com.bartex.adresslentawifi.dynamiccolor

import androidx.lifecycle.ViewModel
import com.bartex.adresslentawifi.App

class DynamicColorViewModel:ViewModel() {
    private val helper:IPreferenceHelper = PreferenceHelper(App.instance)


    fun saveSpeedBrightness(nameOfEffect:String, speed: String?, brightness: String?) {
        helper.saveSpeedBrightness(nameOfEffect, speed, brightness)
    }

    fun getSpeed(nameOfEffect: String): String? {
        return helper.getSpeed(nameOfEffect)
    }

    fun getBrightness(nameOfEffect: String): String?{
        return helper.getBrightness(nameOfEffect)
    }
}