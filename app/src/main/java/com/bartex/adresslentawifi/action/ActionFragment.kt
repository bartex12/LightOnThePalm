package com.bartex.adresslentawifi.action

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import com.bartex.adresslentawifi.R
import com.bartex.adresslentawifi.adapter.ActionAdapter
import com.bartex.adresslentawifi.constants.Constants
import com.bartex.adresslentawifi.databinding.FragmentActionBinding
import com.bartex.adresslentawifi.entity.ListOfActions

class ActionFragment : Fragment(){
    private var _binding: FragmentActionBinding? = null
    private val binding get() = _binding!!

    private val listViewModel: ActionViewModel by activityViewModels()
    private lateinit var navController: NavController
    private var rvAdapter: ActionAdapter? = null
    private var nameTag:String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        //Log.d(Constants.TAG, "ActionFragment onCreateView")
        _binding = FragmentActionBinding.inflate(inflater, container, false)
        return binding.root //fragment_action
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(Constants.TAG, "ActionFragment onViewCreated")

        navController = Navigation.findNavController(view)
        initAdapter()
        listViewModel.loadData()
        listViewModel.getListOfActions().observe(viewLifecycleOwner) {
            renderData(it)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun renderData(data: List<ListOfActions>?) {
        data?. let{ rvAdapter?.listOfActions = it}
    }

    private fun  initAdapter() {
        binding.rvAction.layoutManager = LinearLayoutManager(requireActivity())
        rvAdapter = ActionAdapter(nameTag, getOnClickListener())
        binding.rvAction.adapter= rvAdapter
    }

    private fun getOnClickListener(): ActionAdapter.OnActionClickListener =
        object : ActionAdapter.OnActionClickListener {
            override fun onClick(item: ListOfActions) {
                nameTag = item.nameOfAction
                //Перевод ленты из одной сети WiFi в другую (лента и телефон сейчас в одной
                //сети) идёт по инструкции. Всё, где телефон подключается к точке доступа
                //ленты NodeMCU, выполняет мастер.
                val betweenNetworks = nameTag == getString(R.string.toHome) ||
                        nameTag == getString(R.string.toDifferent)
                if (betweenNetworks) {
                    navigateTo(nameTag, R.id.action_actionFragment_to_syncroFragment)
                } else {
                    navigateTo(nameTag, R.id.action_actionFragment_to_provisionFragment)
                }
            }
        }

    private  fun navigateTo(nameTag:String,  frag:Int ){
        val bundle = bundleOf(Constants.ACTION_NAME to nameTag)
        navController.navigate(frag, bundle)
    }
}