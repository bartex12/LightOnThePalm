package com.bartex.adresslentawifi.main

import com.bartex.adresslentawifi.entity.ListOfEffects

interface IEffectsList {
    fun getList():MutableList<ListOfEffects>
}