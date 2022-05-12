package com.bartex.adresslentawifi.staticcolor

import com.bartex.adresslentawifi.entity.ButtonColor
import com.bartex.adresslentawifi.entity.ListOfEffects

interface IColorList {
    fun getList():MutableList<ButtonColor>
}