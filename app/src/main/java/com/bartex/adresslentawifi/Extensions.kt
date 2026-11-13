package com.bartex.adresslentawifi

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.widget.Toast
import androidx.fragment.app.Fragment

/**
 * Включён ли на телефоне VPN.
 * При включённом VPN запросы к ленте обычно не проходят: Android отправляет их в туннель VPN,
 * а привязать соединение к сети WiFi приложению не разрешает (ошибка EPERM).
 */
fun Context.isVpnActive(): Boolean {
    val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    return try {
        @Suppress("DEPRECATION")
        cm.allNetworks.any {
            cm.getNetworkCapabilities(it)?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true
        }
    } catch (e: RuntimeException) {
        false
    }
}

/**
 * Сообщение "нет связи с лентой"; если включён VPN - с подсказкой про него.
 * Безопасно, если фрагмент уже закрыт.
 */
fun Fragment.toastNoConnection() {
    if (!isAdded) return
    val ctx = requireContext()
    if (ctx.isVpnActive()) {
        Toast.makeText(ctx, R.string.noConnectionVpn, Toast.LENGTH_LONG).show()
    } else {
        Toast.makeText(ctx, R.string.noConnection, Toast.LENGTH_SHORT).show()
    }
}
