package com.bartex.adresslentawifi.action

import com.bartex.adresslentawifi.entity.ListOfActions

interface IActionsList {
    fun getActionsList():MutableList<ListOfActions>
}
