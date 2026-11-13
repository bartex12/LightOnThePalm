package com.bartex.adresslentawifi.synchro

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.Group
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.bartex.adresslentawifi.EspApi
import com.bartex.adresslentawifi.R
import com.bartex.adresslentawifi.SharedViewModel
import com.bartex.adresslentawifi.constants.Constants
import com.bartex.adresslentawifi.constants.Constants.ACTION_NAME
import com.bartex.adresslentawifi.constants.Constants.TAG
import com.bartex.adresslentawifi.isVpnActive
import com.bartex.adresslentawifi.showMessage
import com.google.android.material.bottomsheet.BottomSheetBehavior

/**
 * Перевод ленты из одной сети WiFi в другую, когда лента и телефон сейчас в одной сети:
 * - "Привязка к другой сети WiFi из домашней сети"
 * - "Привязка к домашней сети WiFi из другой сети"
 *
 * Лента получает имя и пароль новой сети и уходит в неё, а телефон в новую сеть
 * переводит пользователь через настройки WiFi (кнопка "Перейти в настройки").
 *
 * Остальные сценарии (через точку доступа NodeMCU) выполняет мастер - ProvisionFragment.
 */
class SynchroFragment : Fragment() {

    private lateinit var bottomSheetHome: ConstraintLayout
    private lateinit var headerHome: TextView
    private lateinit var descriptionHome: TextView
    private lateinit var nameWiFi: EditText
    private lateinit var passWiFi: EditText
    private lateinit var profileHome: Group
    private lateinit var sendNamePassToServerHome: Button
    private lateinit var buttonToSettingHome: Button

    private var actionTag: String? = ""
    private val model: SharedViewModel by activityViewModels()
    private lateinit var pref: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        actionTag = arguments?.getString(ACTION_NAME, "")
        Log.d(TAG, "SynchroFragment onCreate actionTag = $actionTag")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_syncro, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        pref = requireActivity().getSharedPreferences("MyPref", Context.MODE_PRIVATE)
        initView(view)
        initBottomSheet()
    }

    private fun initView(view: View) {
        bottomSheetHome = view.findViewById(R.id.bottom_sheet_container_home)
        headerHome = view.findViewById(R.id.bottom_sheet_header_home)
        descriptionHome = view.findViewById(R.id.bottom_sheet_description_home)
        nameWiFi = view.findViewById(R.id.edName_home)
        passWiFi = view.findViewById(R.id.edPass_home)
        profileHome = view.findViewById(R.id.profile_home)
        sendNamePassToServerHome = view.findViewById(R.id.savePass_home)
        buttonToSettingHome = view.findViewById(R.id.button_to_setting_home)
        //кнопки "Назад" и "Вперёд" были нужны для двухшаговой инструкции - теперь её нет
        view.findViewById<View>(R.id.btnPrev).visibility = View.GONE
        view.findViewById<View>(R.id.btnNext).visibility = View.GONE
    }

    private fun initBottomSheet() {
        BottomSheetBehavior.from(bottomSheetHome).state = BottomSheetBehavior.STATE_EXPANDED

        //подставляем имя сети, которое вводили в прошлый раз (пароль не храним)
        nameWiFi.setText(pref.getString(ssidPrefKey(), ""))
        passWiFi.setText("")
        profileHome.visibility = View.VISIBLE

        if (actionTag == getString(R.string.toDifferent)) {
            headerHome.text = getString(R.string.bottom_shit_title_different)
            descriptionHome.text = getString(R.string.bottom_shit_descr_different)
        } else {
            headerHome.text = getString(R.string.bottom_shit_title_home)
            descriptionHome.text = getString(R.string.bottom_shit_descr_home)
        }

        sendNamePassToServerHome.setOnClickListener {
            val name = nameWiFi.text.toString()
            val password = passWiFi.text.toString()
            Log.d(TAG, "SynchroFragment sendNamePassToServerHome name = $name") //пароль в лог не пишем
            when {
                name.isEmpty() || password.isEmpty() -> showMessage(R.string.emptyNameOrPass)
                //в скетче под имя отведено 30 байт EEPROM, под пароль - 32
                name.toByteArray().size > MAX_SSID_BYTES -> showMessage(R.string.tooLongName)
                password.toByteArray().size > MAX_PASS_BYTES -> showMessage(R.string.tooLongPass)
                else -> sendNameAndPassword(name, password)
            }
        }

        buttonToSettingHome.setOnClickListener {
            startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
        }
    }

    //ключ настроек, под которым запоминаем имя сети для текущего сценария
    private fun ssidPrefKey(): String =
        if (actionTag == getString(R.string.toDifferent)) Constants.DIFFERENT_SSID
        else Constants.HOME_SSID

    //передаём в блок управления имя и пароль новой сети
    private fun sendNameAndPassword(name: String, password: String) {
        val ip = model.getIpAddressEsp().value
        pref.edit().putString(ssidPrefKey(), name).apply()
        sendNamePassToServerHome.isEnabled = false //от повторных нажатий, пока ждём ответ

        //порядок параметров важен: скетч читает их по номеру
        EspApi.get(getString(R.string.baseUrl, ip), getString(R.string._home),
            listOf("name" to name, "password" to password),
            onError = {
                if (view == null) return@get
                sendNamePassToServerHome.isEnabled = true
                //форму не убираем - можно исправить и повторить
                val text = if (requireContext().isVpnActive()) R.string.noConnectionVpn
                           else R.string.sendFailed
                showMessage(text)
            },
            onSuccess = {
                if (view == null) return@get
                sendNamePassToServerHome.isEnabled = true
                profileHome.visibility = View.GONE  //убираем с экрана чтобы не путаться
                buttonToSettingHome.visibility = View.VISIBLE
                //адрес ленты в новой сети пока неизвестен - его найдёт главный экран
                model.updateIpAddressEsp(getString(R.string.hotpointZero))
            })
    }

    companion object {
        private const val MAX_SSID_BYTES = 30
        private const val MAX_PASS_BYTES = 32
    }
}
