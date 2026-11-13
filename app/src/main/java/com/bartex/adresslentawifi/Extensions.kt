package com.bartex.adresslentawifi

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.view.View
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar

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
 * Сообщение внизу экрана. В отличие от Toast (на Android 12+ он обрезается до двух строк),
 * показывает текст целиком. Безопасно, если фрагмент уже закрыт.
 */
fun Fragment.showMessage(text: CharSequence) {
    if (!isAdded) return
    val root = activity?.findViewById<View>(android.R.id.content) ?: return
    //чем длиннее текст, тем дольше показываем: от 3 до 8 секунд
    val duration = (2000 + text.length * 50).coerceIn(3000, 8000)
    val snackbar = Snackbar.make(root, text, duration)
    snackbar.view
        .findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
        ?.maxLines = 8
    snackbar.show()
}

fun Fragment.showMessage(@StringRes textId: Int) {
    if (!isAdded) return
    showMessage(getString(textId))
}

/** Сообщение "нет связи с лентой"; если включён VPN - с подсказкой про него. */
fun Fragment.toastNoConnection() {
    if (!isAdded) return
    if (requireContext().isVpnActive()) {
        showMessage(R.string.noConnectionVpn)
    } else {
        showMessage(R.string.noConnection)
    }
}
