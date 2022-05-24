package com.bartex.adresslentawifi.dynamiccolor

interface IPreferenceHelper {
    fun saveSpeedBrightness(nameOfEffect:String, speed: String?, brightness: String?)
    fun saveNumbers(nameOfEffect: String, numbers: String?)
    fun saveColor(nameOfEffect: String, color: String?)

    fun getSpeed(nameOfEffect: String): String?
    fun getBrightness(nameOfEffect: String): String?
    fun getNumbers(nameOfEffect: String): String?
    fun getColor(nameOfEffect: String): String?
}
