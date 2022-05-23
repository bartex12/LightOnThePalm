package com.bartex.adresslentawifi.dynamiccolor

interface IPreferenceHelper {
    fun saveSpeedBrightness(nameOfEffect:String, speed: String?, brightness: String?)
    fun getSpeed(nameOfEffect: String): String?
    fun getBrightness(nameOfEffect: String): String?
}
