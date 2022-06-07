package com.bartex.adresslentawifi.action

import com.bartex.adresslentawifi.entity.ListOfActions
import com.bartex.adresslentawifi.entity.ListOfEffects

interface IActionsList {
    fun getActionsList():MutableList<ListOfActions>
}
