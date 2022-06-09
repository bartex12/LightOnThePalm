package com.bartex.adresslentawifi.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.bartex.adresslentawifi.R
import com.bartex.adresslentawifi.entity.ListOfEffects
import com.squareup.picasso.Picasso

class RvAdapter(val nameTag:String, val listener: OnItemClickListener):RecyclerView.Adapter<RvAdapter.ViewHolder>() {

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

        fun bind(itemView: ListOfEffects){
            name.text = itemView.name
            triangle.tag = itemView.name
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
                .load(itemView.icon)
                .placeholder(R.drawable.trio_200)
                .error(R.drawable.mistake)
                .into(imageRound)

            this.itemView.setOnClickListener {
                listener.onClick(itemView)
            }
        }
    }
}