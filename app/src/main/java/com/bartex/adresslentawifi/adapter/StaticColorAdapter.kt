package com.bartex.adresslentawifi.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatButton
import androidx.recyclerview.widget.RecyclerView
import com.bartex.adresslentawifi.entity.ButtonColor
import com.bartex.adresslentawifi.R

class StaticColorAdapter(val listener: OnItemClickListener):RecyclerView.Adapter<StaticColorAdapter.ViewHolder>() {
    var listOfButtonColor:List<ButtonColor> = listOf()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    interface OnItemClickListener{
        fun onClick(item: ButtonColor)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_button_color,parent, false )
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(listOfButtonColor[position])
    }

    override fun getItemCount(): Int {
       return listOfButtonColor.size
    }

    inner class ViewHolder(itemView: View):RecyclerView.ViewHolder(itemView) {
        private val buttonColor: AppCompatButton = itemView.findViewById(R.id.but1)

        fun bind(itemBut: ButtonColor){
            buttonColor.setBackgroundColor(Color.parseColor(itemBut.color))
            buttonColor.text = itemBut.text
            if (itemBut.text == "18") buttonColor.setTextColor(Color.WHITE)

            buttonColor.setOnClickListener {
                listener.onClick(itemBut)
            }
        }
    }
}