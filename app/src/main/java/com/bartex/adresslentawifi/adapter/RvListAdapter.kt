package com.bartex.adresslentawifi.adapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.bartex.adresslentawifi.R
import com.bartex.adresslentawifi.constants.Constants.TAG
import com.bartex.adresslentawifi.entity.ListOfEffects
import com.squareup.picasso.Picasso

class RvListAdapter(val nameTag:String, val listener: OnItemClickListener):RecyclerView.Adapter<RvListAdapter.ViewHolder>() {

    init { Log.d(TAG, "RvListAdapter init nameTag = $nameTag")}

    var listOfEffects:List<ListOfEffects> = listOf()
    set(value) {
        field = value
        notifyDataSetChanged()
    }
    lateinit var context:Context

    interface OnItemClickListener{
        fun onClick(item: ListOfEffects)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        //Log.d(TAG, "RvListAdapter onCreateViewHolder: nameTag = $nameTag")
        context =parent.context
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_list_effects,parent, false )
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(listOfEffects[position])
    }

    override fun getItemCount(): Int {
        return listOfEffects.size
    }

   inner class ViewHolder(view: View):RecyclerView.ViewHolder(view) {
        private var name: TextView = view.findViewById(R.id.tv_name_dis)
         private var triangle: ImageView = view.findViewById(R.id.iv_triangle)
        private val imageRound: ImageView = view.findViewById(R.id.iv_round_dis)
        private val clList: ConstraintLayout = view.findViewById(R.id.cl_list)

        fun bind(item: ListOfEffects){
            //Log.d(TAG, "RvListAdapter ViewHolder bind: nameTag = $nameTag")
            name.text = item.name
            triangle.tag = item.name

            when(triangle.tag == nameTag){
                true -> {
                    triangle.visibility = View.VISIBLE
                    clList.background = AppCompatResources.getDrawable(context, R.drawable.item_list_effects)
                }
                false-> {
                    triangle.visibility = View.GONE
                    clList.background = AppCompatResources.getDrawable(context, R.color.colorPrimaryBlack)
                }
            }

            Picasso.get()
                .load(item.icon)
                .placeholder(R.drawable.trio_200)
                .error(R.drawable.mistake)
                .into(imageRound)

            itemView.setOnClickListener {
                listener.onClick(item)
            }
        }
    }
}