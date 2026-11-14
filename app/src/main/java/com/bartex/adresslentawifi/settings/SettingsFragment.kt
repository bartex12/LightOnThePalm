package com.bartex.adresslentawifi.settings

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.bartex.adresslentawifi.EspApi
import com.bartex.adresslentawifi.R
import com.bartex.adresslentawifi.SharedViewModel
import com.bartex.adresslentawifi.databinding.FragmentSettingsBinding
import com.bartex.adresslentawifi.showMessage
import com.bartex.adresslentawifi.toastNoConnection

/**
 * Экран настроек (пункт "Настройки" в шторке).
 * Пока один пункт - количество точек на ленте. Значение хранит сама лента:
 * запрос /leds возвращает "текущее/максимум", /leds?number=N задаёт новое количество,
 * после чего лента перезагружается (нужна прошивка 016 или новее).
 */
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val sharedViewModel: SharedViewModel by activityViewModels()

    private var currentLeds: Int? = null      //сколько точек задано на ленте сейчас
    private var maxLeds = DEFAULT_MAX_LEDS    //наибольшее количество - сообщает лента

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.cardLedCount.setOnClickListener { showLedCountDialog() }
        loadLedCount()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun espBaseUrl(): String? {
        val ip = sharedViewModel.getIpAddressEsp().value
        if (ip.isNullOrEmpty() || ip == getString(R.string.hotpointZero)) return null
        return getString(R.string.baseUrl, ip)
    }

    //спрашиваем у ленты текущее количество точек
    private fun loadLedCount() {
        val baseUrl = espBaseUrl()
        if (baseUrl == null) {
            binding.tvLedCountValue.setText(R.string.settingsNoEsp)
            return
        }
        binding.tvLedCountValue.setText(R.string.settingsLoading)
        EspApi.get(baseUrl, getString(R.string._leds),
            onError = { message ->
                if (_binding != null) {
                    //404 - лента не знает такой команды, то есть прошивка старая
                    binding.tvLedCountValue.setText(
                        if (message.contains("404")) R.string.settingsOldFirmware
                        else R.string.settingsNoEsp)
                }
            },
            onSuccess = { body ->
                if (_binding != null) {
                    //ответ вида "32/1000": текущее количество / максимум
                    val parts = body.trim().split("/")
                    currentLeds = parts.getOrNull(0)?.trim()?.toIntOrNull()
                    parts.getOrNull(1)?.trim()?.toIntOrNull()?.let { maxLeds = it }
                    showLedCount()
                }
            })
    }

    private fun showLedCount() {
        val count = currentLeds
        binding.tvLedCountValue.text =
            if (count != null) getString(R.string.settingsLedCountValue, count, maxLeds)
            else getString(R.string.settingsNoEsp)
    }

    //окно ввода количества точек
    private fun showLedCountDialog() {
        val baseUrl = espBaseUrl()
        if (baseUrl == null) {
            toastNoConnection()
            return
        }
        val input = EditText(requireContext())
        input.inputType = InputType.TYPE_CLASS_NUMBER
        input.setText(currentLeds?.toString() ?: "")
        input.setSelection(input.text.length)
        //В окне Android "отрицательная" кнопка стоит слева, "положительная" - справа.
        //Нужно "Хорошо" слева и "Отмена" справа, поэтому действие висит на левой кнопке.
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.settingsLedCount)
            .setMessage(getString(R.string.settingsLedCountMessage, maxLeds))
            .setView(input)
            .setNegativeButton(R.string.dialogOk) { _, _ ->
                val number = input.text.toString().trim().toIntOrNull()
                if (number == null || number < 1 || number > maxLeds) {
                    showMessage(getString(R.string.settingsLedCountWrong, maxLeds))
                } else {
                    sendLedCount(baseUrl, number)
                }
            }
            .setPositiveButton(R.string.dialogCancel, null)
            .show()
    }

    //передаём ленте новое количество точек; она сохраняет его и перезагружается
    private fun sendLedCount(baseUrl: String, number: Int) {
        EspApi.get(baseUrl, getString(R.string._leds),
            listOf("number" to number.toString()),
            onError = { message ->
                if (message.contains("404")) showMessage(R.string.settingsOldFirmware)
                else toastNoConnection()
            },
            onSuccess = {
                currentLeds = number
                if (_binding != null) showLedCount()
                showMessage(R.string.settingsLedCountSaved)
            })
    }

    companion object {
        private const val DEFAULT_MAX_LEDS = 1000
    }
}
