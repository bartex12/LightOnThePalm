package com.bartex.adresslentawifi.staticcolor

import com.bartex.adresslentawifi.entity.ButtonColor

interface IColorList {
    fun getList():MutableList<ButtonColor>
    fun getShortList():MutableList<ButtonColor>
}