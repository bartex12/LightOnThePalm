package com.bartex.adresslentawifi.action

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bartex.adresslentawifi.R
import com.bartex.adresslentawifi.SharedViewModel
import com.bartex.adresslentawifi.adapter.ActionAdapter
import com.bartex.adresslentawifi.constants.Constants
import com.bartex.adresslentawifi.entity.ListOfActions

class ActionFragment : Fragment(){
    private val listViewModel: ActionViewModel by activityViewModels()
    private val model: SharedViewModel by activityViewModels()
    private lateinit var navController: NavController
    private  lateinit var rvActions: RecyclerView
    private  lateinit var tvName: TextView
    private var rvAdapter: ActionAdapter? = null
    private var nameTag:String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d(Constants.TAG, "ActionFragment onCreateView")
        return inflater.inflate(R.layout.fragment_action, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(Constants.TAG, "ActionFragment onViewCreated")

        navController = Navigation.findNavController(view)
        initViews(view)
        initAdapter()
        listViewModel.loadData()
        listViewModel.getListOfActions().observe(this, {
            renderData(it)
        })
    }
    private fun renderData(data: List<ListOfActions>?) {
        data?. let{ rvAdapter?.listOfActions = it}
    }

    private fun initViews(view: View) {
        rvActions = view.findViewById(R.id.rv_action)
        tvName = view.findViewById(R.id.tvNameAction)
        tvName.text = getString(R.string.chooseAction)
    }

    private fun  initAdapter() {
        rvActions.layoutManager = LinearLayoutManager(requireActivity())
        rvAdapter = ActionAdapter(nameTag, getOnClickListener())
        rvActions.adapter = rvAdapter
    }

    private fun getOnClickListener(): ActionAdapter.OnActionClickListener =
        object : ActionAdapter.OnActionClickListener {
            override fun onClick(item: ListOfActions) {
                nameTag = item.nameOfAction
                navigateTo(nameTag, R.id.action_actionFragment_to_localFragment)
               // listViewModel.updateTitle(item.nameOfAction) //обновляем имя заголовка тулбара
                //val baseUrl = String.format(getString(R.string.baseUrl), edIp.text.toString())
            }
        }

    private  fun navigateTo(nameTag:String,  frag:Int ){
        val bundle = bundleOf(Constants.ACTION_NAME to nameTag)
        navController.navigate(frag, bundle)
    }
}