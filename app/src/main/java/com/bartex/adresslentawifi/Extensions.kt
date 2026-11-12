package com.bartex.adresslentawifi

import android.widget.Toast
import androidx.fragment.app.Fragment

/** Короткое сообщение "нет связи с лентой". Безопасно, если фрагмент уже закрыт. */
fun Fragment.toastNoConnection() {
    if (!isAdded) return
    Toast.makeText(requireContext(), R.string.noConnection, Toast.LENGTH_SHORT).show()
}
