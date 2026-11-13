package com.bartex.adresslentawifi

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.bartex.adresslentawifi.constants.Constants

/**
 * Локальный режим: лента сама является точкой доступа NodeMCU, роутер не нужен,
 * телефон управляет лентой, подключаясь к ней напрямую через EspLink (Android 10+).
 *
 * Признак режима хранится в настройках. Пока он включён, приложение при каждом
 * появлении на экране само подключается к NodeMCU (см. MainActivity.onStart).
 */
object LocalMode {

    /**
     * true - автоподключение уже пробовали и повторять не надо, пока не случится одно из:
     * приложение перезапущено, пользователь подключился вручную, установленная связь пропала.
     * Нужен, чтобы после отказа в системном окне оно не появлялось снова и снова.
     */
    @Volatile
    var autoConnectBlocked = false

    fun isEnabled(context: Context): Boolean =
        prefs(context).getBoolean(Constants.LOCAL_MODE, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(Constants.LOCAL_MODE, enabled).apply()
    }

    /** Подключить телефон к точке доступа ленты. Результат - в EspLink.state. */
    @RequiresApi(Build.VERSION_CODES.Q)
    fun connect(context: Context) {
        EspLink.connect(context,
            context.getString(R.string.localNetworkName),
            context.getString(R.string.localNetworkPass))
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences("MyPref", Context.MODE_PRIVATE)
}
