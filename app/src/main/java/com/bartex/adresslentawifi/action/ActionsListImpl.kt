package com.bartex.adresslentawifi.action

import com.bartex.adresslentawifi.App
import com.bartex.adresslentawifi.R
import com.bartex.adresslentawifi.entity.ListOfActions


class ActionsListImpl(private val application: App) : IActionsList {

    override fun getActionsList(): MutableList<ListOfActions> {
        return mutableListOf(
            ListOfActions(nameOfAction = application.resources.getString(R.string.firstInclusion)),
            ListOfActions(nameOfAction = application.resources.getString(R.string.toDifferent)),
            ListOfActions(nameOfAction = application.resources.getString(R.string.toHome)),
            ListOfActions(nameOfAction = application.resources.getString(R.string.createHotpoint)),
            ListOfActions(nameOfAction = application.resources.getString(R.string.toHomeFromLocal)),
            ListOfActions(nameOfAction = application.resources.getString(R.string.toDifferentFromLocal))
        )
    }
}