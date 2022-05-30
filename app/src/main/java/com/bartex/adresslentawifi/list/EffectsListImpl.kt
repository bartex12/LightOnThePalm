package com.bartex.adresslentawifi.list

import android.app.Application
import com.bartex.adresslentawifi.entity.ListOfEffects
import com.bartex.adresslentawifi.R

class EffectsListImpl(private val application: Application) : IEffectsList {
    override fun getList(): MutableList<ListOfEffects> {
        return mutableListOf(
            ListOfEffects(icon = R.drawable.static200 , name = application.resources.getString(R.string.staticColors)),
            ListOfEffects(icon = R.drawable.play200, name = application.resources.getString(R.string.playOfColors)),
            ListOfEffects(icon = R.drawable.play_flash200, name = application.resources.getString(R.string.playOfColorsFlash)),
            ListOfEffects(icon = R.drawable.physics100, name = application.resources.getString(R.string.running_lights)),
            ListOfEffects(icon = R.drawable.flame200, name = application.resources.getString(R.string.flame)),
            ListOfEffects(icon = R.drawable.rainbow200, name = application.resources.getString(R.string.rainbow)) ,
            ListOfEffects(icon = R.drawable.rainbow_fade200, name = application.resources.getString(R.string.pride)),
            ListOfEffects(icon = R.drawable.rainbow_bias200, name = application.resources.getString(R.string.rainbow_bias)),
            ListOfEffects(icon = R.drawable.rainbow_flash100, name = application.resources.getString(R.string.rainbow_flash)),
            ListOfEffects(icon = R.drawable.konfetti200, name = application.resources.getString(R.string.random_color)),
            ListOfEffects(icon = R.drawable.konfetti_fade200, name = application.resources.getString(R.string.random_color_fade)),
            ListOfEffects(icon = R.drawable.nothflash, name = application.resources.getString(R.string.northern_lights))/*,
            ListOfEffects(icon = R.drawable.physics100, name = application.resources.getString(R.string.test))*/
        )
    }
}
