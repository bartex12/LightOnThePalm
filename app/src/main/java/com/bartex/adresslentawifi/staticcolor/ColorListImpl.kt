package com.bartex.adresslentawifi.staticcolor

import com.bartex.adresslentawifi.App
import com.bartex.adresslentawifi.R
import com.bartex.adresslentawifi.entity.ButtonColor

class ColorListImpl(private val instance: App): IColorList {

    override fun getList(): MutableList<ButtonColor> {
        return mutableListOf(
            ButtonColor(instance.resources.getString(R.string._1),"1"),
            ButtonColor(instance.resources.getString(R.string._2),"2"),
            ButtonColor(instance.resources.getString(R.string._3),"3"),
            ButtonColor(instance.resources.getString(R.string._4),"4"),
            ButtonColor(instance.resources.getString(R.string._5),"5"),
            ButtonColor(instance.resources.getString(R.string._6),"6"),
            ButtonColor(instance.resources.getString(R.string._7),"7"),
            ButtonColor(instance.resources.getString(R.string._8),"8"),
            ButtonColor(instance.resources.getString(R.string._9),"9"),
            ButtonColor(instance.resources.getString(R.string._10),"10"),
            ButtonColor(instance.resources.getString(R.string._11),"11"),
            ButtonColor(instance.resources.getString(R.string._12),"12"),
            ButtonColor(instance.resources.getString(R.string._13),"13"),
            ButtonColor(instance.resources.getString(R.string._14),"14"),
            ButtonColor(instance.resources.getString(R.string._15),"15"),
            ButtonColor(instance.resources.getString(R.string._16),"16"),
            ButtonColor(instance.resources.getString(R.string._17),"17"),
            ButtonColor(instance.resources.getString(R.string._18),"18")
        )
    }

    override fun getShortList(): MutableList<ButtonColor> {
        return mutableListOf(
            ButtonColor(instance.resources.getString(R.string._1),"1"),
            ButtonColor(instance.resources.getString(R.string._5),"2"),
            ButtonColor(instance.resources.getString(R.string._7),"3"),
            ButtonColor(instance.resources.getString(R.string._13),"4"),
            ButtonColor(instance.resources.getString(R.string._15),"5"),
            ButtonColor(instance.resources.getString(R.string._17),"6"),
        )
    }
}