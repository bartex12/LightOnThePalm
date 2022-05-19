package com.bartex.adresslentawifi.list

import android.app.Application
import com.bartex.adresslentawifi.entity.ListOfEffects
import com.bartex.adresslentawifi.R

class EffectsListImpl(private val application: Application) : IEffectsList {
    override fun getList(): MutableList<ListOfEffects> {
        return mutableListOf(
            ListOfEffects(application.resources.getString(R.string.staticColors)),
            ListOfEffects(application.resources.getString(R.string.playOfColors)),
            ListOfEffects(application.resources.getString(R.string.flame)),
            ListOfEffects(application.resources.getString(R.string.pride)),
            ListOfEffects(application.resources.getString(R.string.rainbow))
        )
    }
}
