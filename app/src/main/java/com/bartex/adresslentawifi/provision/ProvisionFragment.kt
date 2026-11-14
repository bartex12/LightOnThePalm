package com.bartex.adresslentawifi.provision

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.bartex.adresslentawifi.R
import com.bartex.adresslentawifi.SharedViewModel
import com.bartex.adresslentawifi.constants.Constants
import com.bartex.adresslentawifi.databinding.FragmentProvisionBinding
import com.bartex.adresslentawifi.isVpnActive
import com.bartex.adresslentawifi.provision.ProvisionViewModel.Failure
import com.bartex.adresslentawifi.provision.ProvisionViewModel.Step
import com.bartex.adresslentawifi.showMessage

/**
 * Мастер переключения ленты между сетями (Android 10+).
 * - привязка к сети WiFi: поле имени, поле пароля и кнопка;
 * - создание локальной сети NodeMCU: только кнопка.
 * Всю работу выполняет [ProvisionViewModel]; здесь только показ.
 */
class ProvisionFragment : Fragment() {

    private var _binding: FragmentProvisionBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProvisionViewModel by viewModels()
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private lateinit var pref: SharedPreferences
    private var actionTag = ""
    private var successHandled = false

    //true - сценарий "Создание локальной сети NodeMCU", false - привязка к сети WiFi
    private val toHotpoint: Boolean
        get() = actionTag == getString(R.string.createHotpoint)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        actionTag = arguments?.getString(Constants.ACTION_NAME, "") ?: ""
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProvisionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        pref = requireActivity().getSharedPreferences("MyPref", Context.MODE_PRIVATE)

        binding.tvTitle.text = actionTag
        binding.tvDescription.setText(
            if (toHotpoint) R.string.provDescriptionHotpoint else R.string.provDescription)
        if (savedInstanceState == null) {
            //подставляем имя сети, которое вводили в прошлый раз (пароль не храним)
            binding.edName.setText(pref.getString(ssidPrefKey(), ""))
        }
        binding.btnStart.setOnClickListener { onStartClick() }
        //панель выбора сети WiFi поверх приложения, без ухода в настройки
        binding.btnWifi.setOnClickListener {
            try {
                startActivity(Intent(Settings.Panel.ACTION_WIFI))
            } catch (e: RuntimeException) {
                startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
            }
        }
        binding.btnDone.setOnClickListener {
            findNavController().popBackStack(R.id.listFragment, false)
        }
        viewModel.state.observe(viewLifecycleOwner) { render(it) }
    }

    override fun onResume() {
        super.onResume()
        //с включённым VPN запросы к ленте обычно не проходят - предупреждаем заранее
        binding.tvVpnWarning.visibility =
            if (requireContext().isVpnActive()) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    //ключ настроек, под которым запоминаем имя сети для текущего сценария
    private fun ssidPrefKey(): String =
        if (actionTag == getString(R.string.toDifferentFromLocal)) Constants.DIFFERENT_SSID
        else Constants.HOME_SSID

    private fun onStartClick() {
        if (toHotpoint) {
            val ip = sharedViewModel.getIpAddressEsp().value ?: ""
            viewModel.startToHotpoint(ip)
            return
        }
        val name = binding.edName.text?.toString()?.trim() ?: ""
        val password = binding.edPass.text?.toString() ?: ""
        when {
            name.isEmpty() || password.isEmpty() -> showMessage(R.string.emptyNameOrPass)
            //в скетче (версия 014) под имя отведено 32 байта EEPROM, под пароль - 63
            name.toByteArray().size > MAX_SSID_BYTES -> showMessage(R.string.tooLongName)
            password.toByteArray().size > MAX_PASS_BYTES -> showMessage(R.string.tooLongPass)
            else -> {
                pref.edit().putString(ssidPrefKey(), name).apply()
                hideKeyboard()
                viewModel.startToNetwork(name, password)
            }
        }
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.root.windowToken, 0)
    }

    private fun render(state: ProvisionViewModel.UiState) {
        val step = state.step
        val inProgress = step == Step.CONNECTING || step == Step.SENDING ||
                step == Step.SWITCHING || step == Step.SEARCHING
        val formVisible = step == Step.FORM || step == Step.ERROR

        //форма ввода
        val formVisibility = if (formVisible) View.VISIBLE else View.GONE
        binding.tvDescription.visibility = formVisibility
        //для локальной сети имя и пароль не нужны - они заданы в скетче
        val inputsVisibility = if (formVisible && !toHotpoint) View.VISIBLE else View.GONE
        binding.tilName.visibility = inputsVisibility
        binding.tilPass.visibility = inputsVisibility
        binding.btnStart.visibility = formVisibility
        binding.btnStart.setText(when {
            step == Step.ERROR -> R.string.provRetry
            toHotpoint -> R.string.provStartHotpoint
            else -> R.string.provStart
        })

        //ход работы
        binding.progress.visibility = if (inProgress) View.VISIBLE else View.GONE
        binding.tvSteps.visibility = if (step == Step.FORM) View.GONE else View.VISIBLE
        binding.tvSteps.text = stepsText(state)

        //подсказка: телефон надо подключить к сети ленты вручную
        val hintVisibility =
            if (step == Step.SEARCHING && state.wifiHint) View.VISIBLE else View.GONE
        binding.tvWifiHint.visibility = hintVisibility
        binding.btnWifi.visibility = hintVisibility
        binding.tvWifiHint.text = getString(R.string.provWifiHint, state.ssid)

        //результат
        binding.btnDone.visibility = if (step == Step.SUCCESS) View.VISIBLE else View.GONE
        when (step) {
            Step.SUCCESS -> {
                binding.tvResult.visibility = View.VISIBLE
                binding.tvResult.text =
                    if (toHotpoint) getString(R.string.provSuccessHotpoint)
                    else getString(R.string.provSuccess, state.espSsid, state.espIp)
                if (!successHandled) {
                    successHandled = true
                    //главный экран берёт адрес и имя сети ленты отсюда
                    sharedViewModel.updateIpAddressEsp(state.espIp)
                    sharedViewModel.updateEspNetworkSsid(state.espSsid)
                    sharedViewModel.updateNameOfEffect("")
                    if (toHotpoint) sharedViewModel.updateTlfSSID(state.espSsid)
                }
            }
            Step.ERROR -> {
                binding.tvResult.visibility = View.VISIBLE
                binding.tvResult.text = failureText(state)
            }
            else -> binding.tvResult.visibility = View.GONE
        }
    }

    //список шагов с отметками: выполнен, выполняется, ещё не начат
    private fun stepsText(state: ProvisionViewModel.UiState): String {
        //номер текущего шага: 0, 1 или 2; 3 - все выполнены
        val current = state.stepIndex
        val failed = state.step == Step.ERROR
        val titles = if (toHotpoint) listOf(
            getString(R.string.provHotStep1),
            getString(R.string.provStep1),
            getString(R.string.provHotStep3)
        ) else listOf(
            getString(R.string.provStep1),
            getString(R.string.provStep2),
            getString(R.string.provStep3, state.ssid)
        )
        return titles.mapIndexed { index, title ->
            val mark = when {
                index < current -> "✓"
                index == current && failed -> "✗"
                index == current -> "▶"
                else -> "·"
            }
            "$mark  $title"
        }.joinToString("\n\n")
    }

    private fun failureText(state: ProvisionViewModel.UiState): String {
        val text = if (toHotpoint) when (state.failure) {
            Failure.AP_NOT_FOUND -> getString(R.string.provErrorHotAp)
            Failure.NOT_DELIVERED -> getString(R.string.provErrorHotSend)
            else -> getString(R.string.provErrorHotCheck)
        } else when (state.failure) {
            Failure.AP_NOT_FOUND -> getString(R.string.provErrorAp)
            Failure.NOT_DELIVERED -> getString(R.string.provErrorSend)
            else -> getString(R.string.provErrorSearch, state.ssid)
        }
        return if (requireContext().isVpnActive()) {
            text + "\n\n" + getString(R.string.provErrorVpn)
        } else text
    }

    companion object {
        private const val MAX_SSID_BYTES = 32
        private const val MAX_PASS_BYTES = 63
    }
}
