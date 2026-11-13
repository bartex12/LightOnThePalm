package com.bartex.adresslentawifi.provision

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.annotation.RequiresApi
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
 * Мастер привязки ленты к сети WiFi (Android 10+): одно поле имени, одно поле пароля и кнопка.
 * Подключение к точке доступа ленты, передачу данных и проверку результата выполняет
 * [ProvisionViewModel]; здесь только показ.
 */
@RequiresApi(Build.VERSION_CODES.Q)
class ProvisionFragment : Fragment() {

    private var _binding: FragmentProvisionBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProvisionViewModel by viewModels()
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private lateinit var pref: SharedPreferences
    private var actionTag = ""
    private var successHandled = false

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
        if (savedInstanceState == null) {
            //подставляем имя сети, которое вводили в прошлый раз (пароль не храним)
            binding.edName.setText(pref.getString(ssidPrefKey(), ""))
        }
        binding.btnStart.setOnClickListener { onStartClick() }
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
        val name = binding.edName.text?.toString()?.trim() ?: ""
        val password = binding.edPass.text?.toString() ?: ""
        when {
            name.isEmpty() || password.isEmpty() -> showMessage(R.string.emptyNameOrPass)
            //в скетче под имя отведено 30 байт EEPROM, под пароль - 32
            name.toByteArray().size > MAX_SSID_BYTES -> showMessage(R.string.tooLongName)
            password.toByteArray().size > MAX_PASS_BYTES -> showMessage(R.string.tooLongPass)
            else -> {
                pref.edit().putString(ssidPrefKey(), name).apply()
                hideKeyboard()
                viewModel.start(name, password)
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
        binding.tilName.visibility = formVisibility
        binding.tilPass.visibility = formVisibility
        binding.btnStart.visibility = formVisibility
        binding.btnStart.setText(if (step == Step.ERROR) R.string.provRetry else R.string.provStart)

        //ход работы
        binding.progress.visibility = if (inProgress) View.VISIBLE else View.GONE
        binding.tvSteps.visibility = if (step == Step.FORM) View.GONE else View.VISIBLE
        binding.tvSteps.text = stepsText(state)

        //результат
        binding.btnDone.visibility = if (step == Step.SUCCESS) View.VISIBLE else View.GONE
        when (step) {
            Step.SUCCESS -> {
                binding.tvResult.visibility = View.VISIBLE
                binding.tvResult.text = getString(R.string.provSuccess, state.espSsid, state.espIp)
                if (!successHandled) {
                    successHandled = true
                    //главный экран берёт адрес и имя сети ленты отсюда
                    sharedViewModel.updateIpAddressEsp(state.espIp)
                    sharedViewModel.updateEspNetworkSsid(state.espSsid)
                    sharedViewModel.updateNameOfEffect("")
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
        val current = when (state.step) {
            Step.FORM, Step.CONNECTING -> 0
            Step.SENDING -> 1
            Step.SWITCHING, Step.SEARCHING -> 2
            Step.SUCCESS -> 3
            Step.ERROR -> when (state.failure) {
                Failure.AP_NOT_FOUND -> 0
                Failure.NOT_DELIVERED -> 1
                else -> 2
            }
        }
        val failed = state.step == Step.ERROR
        val titles = listOf(
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
        val text = when (state.failure) {
            Failure.AP_NOT_FOUND -> getString(R.string.provErrorAp)
            Failure.NOT_DELIVERED -> getString(R.string.provErrorSend)
            else -> getString(R.string.provErrorSearch, state.ssid)
        }
        return if (requireContext().isVpnActive()) {
            text + "\n\n" + getString(R.string.provErrorVpn)
        } else text
    }

    companion object {
        private const val MAX_SSID_BYTES = 30
        private const val MAX_PASS_BYTES = 32
    }
}
