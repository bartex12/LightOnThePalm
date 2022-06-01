package com.bartex.adresslentawifi.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.bartex.adresslentawifi.R

class WiFiListAdapter(val nameTag:String, val listener:OnClickWiFiListener): RecyclerView.Adapter<WiFiListAdapter.ViewHolder>() {
    var list:List<String> = listOf()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    lateinit var context: Context

    interface OnClickWiFiListener{
     fun  onWiFiClick(name:String)
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        context =parent.context
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_view,parent, false )
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(list[position])
    }

    override fun getItemCount(): Int {
        return list.size
    }

    inner class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        private val item: TextView = itemView.findViewById(R.id.tvWiFi)
        private val cardView: CardView = itemView.findViewById(R.id.card)

        fun bind(itemWiFi: String){
            item.text = itemWiFi
            item.tag =itemWiFi

            when(item.tag == "NodeMCU"){
                true -> cardView.background =
                    AppCompatResources.getDrawable(context, R.drawable.item_list_effects)
                false-> {
                    cardView.background =
                        AppCompatResources.getDrawable(context, R.color.colorSecondaryBlack)
                }
            }


            itemView.setOnClickListener {
                listener.onWiFiClick(itemWiFi)
            }
        }
    }
}