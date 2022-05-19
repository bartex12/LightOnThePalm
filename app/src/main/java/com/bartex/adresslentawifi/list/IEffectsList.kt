package com.bartex.adresslentawifi.list

import com.bartex.adresslentawifi.entity.ListOfEffects

interface IEffectsList {
    fun getList():MutableList<ListOfEffects>
}