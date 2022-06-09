package com.bartex.adresslentawifi.dynamiccolor


import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.bartex.adresslentawifi.App
import com.bartex.adresslentawifi.R
import com.bartex.adresslentawifi.constants.Constants.COLOR_RUN
import com.bartex.adresslentawifi.constants.Constants.FLAME_BRIGHTNESS
import com.bartex.adresslentawifi.constants.Constants.FLAME_SPEED
import com.bartex.adresslentawifi.constants.Constants.NORTHERN_LIGHTS_BRIGHTNESS
import com.bartex.adresslentawifi.constants.Constants.NORTHERN_LIGHTS_SPEED
import com.bartex.adresslentawifi.constants.Constants.NUMBER_LED
import com.bartex.adresslentawifi.constants.Constants.PLAY_BRIGHTNESS
import com.bartex.adresslentawifi.constants.Constants.PLAY_FLASH_BRIGHTNESS
import com.bartex.adresslentawifi.constants.Constants.PLAY_FLASH_SPEED
import com.bartex.adresslentawifi.constants.Constants.PLAY_SPEED
import com.bartex.adresslentawifi.constants.Constants.PRIDE_BRIGHTNESS
import com.bartex.adresslentawifi.constants.Constants.PRIDE_SPEED
import com.bartex.adresslentawifi.constants.Constants.RAINBOW_BIAS_BRIGHTNESS
import com.bartex.adresslentawifi.constants.Constants.RAINBOW_BIAS_SPEED
import com.bartex.adresslentawifi.constants.Constants.RAINBOW_BRIGHTNESS
import com.bartex.adresslentawifi.constants.Constants.RAINBOW_FLASH_BRIGHTNESS
import com.bartex.adresslentawifi.constants.Constants.RAINBOW_FLASH_SPEED
import com.bartex.adresslentawifi.constants.Constants.RAINBOW_SPEED
import com.bartex.adresslentawifi.constants.Constants.RANDOM_COLOR_BRIGHTNESS
import com.bartex.adresslentawifi.constants.Constants.RANDOM_COLOR_FADE_BRIGHTNESS
import com.bartex.adresslentawifi.constants.Constants.RANDOM_COLOR_FADE_SPEED
import com.bartex.adresslentawifi.constants.Constants.RANDOM_COLOR_SPEED
import com.bartex.adresslentawifi.constants.Constants.RUNNING_BRIGHTNESS
import com.bartex.adresslentawifi.constants.Constants.RUNNING_SPEED

class PreferenceHelper(val app: App) : IPreferenceHelper {
    private val pref: SharedPreferences =  PreferenceManager.getDefaultSharedPreferences(app)
    private val editor: SharedPreferences.Editor = pref.edit()

    override fun saveSpeedBrightness(nameOfEffect:String, speed: String?, brightness: String?) {
        when(nameOfEffect){
            app.getString(R.string.playOfColors)->edit(PLAY_SPEED,PLAY_BRIGHTNESS,speed,brightness)
            app.getString(R.string.playOfColorsFlash)->edit(PLAY_FLASH_SPEED,PLAY_FLASH_BRIGHTNESS,speed,brightness)
            app.getString(R.string.running_lights)->edit(RUNNING_SPEED,RUNNING_BRIGHTNESS,speed,brightness)
            app.getString(R.string.flame)->edit(FLAME_SPEED,FLAME_BRIGHTNESS,speed,brightness)
            app.getString(R.string.rainbow)->edit(RAINBOW_SPEED,RAINBOW_BRIGHTNESS,speed,brightness)
            app.getString(R.string.pride)->edit(PRIDE_SPEED,PRIDE_BRIGHTNESS,speed,brightness)
            app.getString(R.string.rainbow_bias)->edit(RAINBOW_BIAS_SPEED,RAINBOW_BIAS_BRIGHTNESS,speed,brightness)
            app.getString(R.string.rainbow_flash)->edit(RAINBOW_FLASH_SPEED,RAINBOW_FLASH_BRIGHTNESS,speed,brightness)
            app.getString(R.string.random_color)->edit(RANDOM_COLOR_SPEED,RANDOM_COLOR_BRIGHTNESS,speed,brightness)
            app.getString(R.string.random_color_fade)->edit(RANDOM_COLOR_FADE_SPEED,RANDOM_COLOR_FADE_SPEED,speed,brightness)
            app.getString(R.string.northern_lights)->edit(NORTHERN_LIGHTS_SPEED,NORTHERN_LIGHTS_BRIGHTNESS,speed,brightness)
        }
    }

    override fun saveNumbers(nameOfEffect: String, numbers: String?) {
        when(nameOfEffect){
            app.getString(R.string.running_lights)->{
                with(editor){
                    putString(NUMBER_LED, numbers)
                    apply()
                }
            }
        }
    }

    override fun saveColor(nameOfEffect: String, color: String?) {
        when(nameOfEffect){
            app.getString(R.string.running_lights)->{
                with(editor){
                    putString(COLOR_RUN, color)
                    apply()
                }
            }
        }
    }

    private fun edit(c_speed:String, c_br:String, speed:String?, br:String?){
        with(editor){
            putString(c_speed, speed)
            putString(c_br, br)
            apply()
        }
    }

    override fun getSpeed(nameOfEffect: String): String? {
        return when(nameOfEffect){
            app.getString(R.string.playOfColors) -> pref.getString(PLAY_SPEED, "200")
            app.getString(R.string.playOfColorsFlash) -> pref.getString(PLAY_FLASH_SPEED, "200")
            app.getString(R.string.running_lights) -> pref.getString(RUNNING_SPEED, "200")
            app.getString(R.string.flame) -> pref.getString(FLAME_SPEED, "200")
            app.getString(R.string.rainbow) -> pref.getString(RAINBOW_SPEED, "200")
            app.getString(R.string.pride) -> pref.getString(PRIDE_SPEED, "200")
            app.getString(R.string.rainbow_bias) -> pref.getString(RAINBOW_BIAS_SPEED, "200")
            app.getString(R.string.rainbow_flash) -> pref.getString(RAINBOW_FLASH_SPEED, "200")
            app.getString(R.string.random_color) -> pref.getString(RANDOM_COLOR_SPEED, "200")
            app.getString(R.string.random_color_fade) -> pref.getString(RANDOM_COLOR_FADE_SPEED, "200")
            app.getString(R.string.northern_lights) -> pref.getString(NORTHERN_LIGHTS_SPEED, "200")
            else ->  pref.getString(PLAY_SPEED, "200")
        }
    }

    override fun getBrightness(nameOfEffect: String): String? {
        return when(nameOfEffect){
            app.getString(R.string.playOfColors) -> pref.getString(PLAY_BRIGHTNESS, "50")
            app.getString(R.string.playOfColorsFlash) -> pref.getString(PLAY_FLASH_BRIGHTNESS, "50")
            app.getString(R.string.running_lights) -> pref.getString(RUNNING_BRIGHTNESS, "50")
            app.getString(R.string.flame) -> pref.getString(FLAME_BRIGHTNESS, "50")
            app.getString(R.string.rainbow) -> pref.getString(RAINBOW_BRIGHTNESS, "50")
            app.getString(R.string.pride) -> pref.getString(PRIDE_BRIGHTNESS, "50")
            app.getString(R.string.rainbow_bias) -> pref.getString(RAINBOW_BIAS_BRIGHTNESS, "50")
            app.getString(R.string.rainbow_flash) -> pref.getString(RAINBOW_FLASH_BRIGHTNESS, "50")
            app.getString(R.string.random_color) -> pref.getString(RANDOM_COLOR_BRIGHTNESS, "50")
            app.getString(R.string.random_color_fade) -> pref.getString(RANDOM_COLOR_FADE_BRIGHTNESS, "50")
            app.getString(R.string.northern_lights) -> pref.getString(NORTHERN_LIGHTS_BRIGHTNESS, "50")
            else ->  pref.getString(PLAY_BRIGHTNESS, "50")
        }
    }

    override fun getNumbers(nameOfEffect: String): String? {
        return when(nameOfEffect){
            app.getString(R.string.running_lights)->pref.getString(NUMBER_LED, "1")
            else->pref.getString(NUMBER_LED, "1")
        }
    }

    override fun getColor(nameOfEffect: String): String? {
        return when(nameOfEffect){
            app.getString(R.string.running_lights)->pref.getString(COLOR_RUN, "0x00FFFF")
            else->pref.getString(COLOR_RUN, "0x00FFFF")
        }
    }
}
