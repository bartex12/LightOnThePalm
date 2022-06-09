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
import com.bartex.adresslentawifi.entity.ListOfActions


class ActionAdapter(val nameTag:String, val listener: OnActionClickListener)
    :RecyclerView.Adapter<ActionAdapter.ViewHolder>() {

    var listOfActions: List<ListOfActions> = listOf()
        set(value) {
            field = value
            notifyDataSetChanged()
        }
    lateinit var context: Context

    interface OnActionClickListener {
        fun onClick(item: ListOfActions)
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        context = parent.context
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_list_actions, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(listOfActions[position])
    }

    override fun getItemCount(): Int {
        return listOfActions.size
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private var name: TextView = view.findViewById(R.id.tv_name_dis_action)
        private var triangle: ImageView = view.findViewById(R.id.iv_triangle_action)
        private val clList: ConstraintLayout = view.findViewById(R.id.cl_list_action)

        fun bind(itemView: ListOfActions) {
            name.text = itemView.nameOfAction
            triangle.tag = itemView.nameOfAction
            when (triangle.tag == nameTag) {
                true -> {
                    triangle.visibility = View.VISIBLE
                    clList.background =
                        AppCompatResources.getDrawable(context, R.drawable.item_list_effects)
                }
                false -> {
                    triangle.visibility = View.GONE
                    clList.background =
                        AppCompatResources.getDrawable(context, R.color.colorPrimaryBlack)
                }
            }
            this.itemView.setOnClickListener {
                listener.onClick(itemView)
            }
        }
    }
}