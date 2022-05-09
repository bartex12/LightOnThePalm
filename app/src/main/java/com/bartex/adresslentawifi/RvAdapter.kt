package com.bartex.adresslentawifi

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class RvAdapter(val listener:OnItemClickListener):RecyclerView.Adapter<RvAdapter.ViewHolder>() {

    var listOfEffects:List<ListOfEffects> = listOf()
    set(value) {
        field = value
        notifyDataSetChanged()
    }

    interface OnItemClickListener{
        fun onClick(item:ListOfEffects)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_view,parent, false )
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(listOfEffects[position])
    }

    override fun getItemCount(): Int {
        return listOfEffects.size
    }

   inner class ViewHolder(view: View):RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.name)

        fun bind(itemView:ListOfEffects){
            name.text = itemView.name

            this.itemView.setOnClickListener {
                listener.onClick(itemView)
            }
        }
    }
}