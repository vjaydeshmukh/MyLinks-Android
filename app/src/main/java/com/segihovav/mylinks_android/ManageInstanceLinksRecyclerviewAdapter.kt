package com.segihovav.mylinks_android

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ManageInstanceLinksRecyclerviewAdapter internal constructor(private val mContext: Context, private val myLinks: MutableList<String>) : RecyclerView.Adapter<ManageInstanceLinksRecyclerviewAdapter.MyViewHolder>() {
     var darkMode: Boolean = false
     var rowFG: LinearLayout? = null

     override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
          val view: View = LayoutInflater.from(mContext).inflate(R.layout.manage_instance_link_item, parent, false)
          rowFG=view.findViewById(R.id.rowFG)
          return MyViewHolder(view)
     }

     override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
          if (position >= DataService.instanceURLType.size)
               return

          holder.linkName.text = DataService.instanceURLType[position].Name
          holder.linkInfo.text = DataService.instanceURLType[position].URL
     }

     override fun getItemCount(): Int {
          return myLinks.size
     }

     inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
          val linkName: TextView = itemView.findViewById(R.id.link_name)
          val linkInfo: TextView = itemView.findViewById(R.id.link_info)
     }
}
