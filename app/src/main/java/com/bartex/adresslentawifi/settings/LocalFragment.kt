package com.bartex.adresslentawifi.settings

import android.content.Intent
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.Group
import androidx.fragment.app.activityViewModels
import com.bartex.adresslentawifi.R
import com.bartex.adresslentawifi.Server
import com.bartex.adresslentawifi.SharedViewModel
import com.bartex.adresslentawifi.constants.Constants
import com.bartex.adresslentawifi.constants.Constants.TAG
import com.google.android.material.bottomsheet.BottomSheetBehavior

//class LocalFragment: SyncroFragment() {
//
//    //находим корневой лейаут и подключаем BottomSheet
//    override fun initBottomSheet(view: View, isNodeMCU:Boolean) {
//        val bottomSheetLocal: ConstraintLayout = view.findViewById(R.id.bottom_sheet_container_local)
//        val profileLocal: Group =view.findViewById(R.id.profile_local)
//        profileLocal.visibility = View.GONE
//        val buttonToSettingLocal: Button = view.findViewById(R.id.button_to_setting_local)
//        val  headerLocal: TextView =view.findViewById(R.id.bottom_sheet_header_local)
//        headerLocal.text =getString(R.string. bottom_shit_title_home)
//        val  descriptionLocal: TextView =view.findViewById(R.id.bottom_sheet_description_local)
//        descriptionLocal.text =getString(R.string. bottom_shit_descr3)
//        //val nameLocal: EditText =view.findViewById(R.id.edName_local)
//        // val passLocal: EditText =view.findViewById(R.id.edPass_local)
//        //val sendNamePassToServerLocal: Button =view.findViewById(R.id.savePass_local)
//
//        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetLocal)
//        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HALF_EXPANDED
//
//        buttonToSettingLocal.setOnClickListener {
//            val ipAddress =  model.getIpAddress().value
//            Log.d(TAG, "initBottomSheet: ipAddress = $ipAddress ")
//            Server(
//                getString(R.string.baseUrl, ipAddress),
//                getString(R.string.postHome, getString(R.string._hotpoint),
//                    getString(R.string.localNetworkName), getString(R.string.localNetworkPass)),
//                this@LocalFragment
//            ).start()
//            model.updateIpAddress(getString(R.string.hotpointIp))
//            startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
//        }
//    }
//
//}