package com.example.coursessupermarche.adapters

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.coursessupermarche.R
import com.example.coursessupermarche.models.ShoppingItem

class ShoppingItemAdapter(
    private val onItemChecked: (ShoppingItem, Boolean) -> Unit,
    private val onItemClicked: (ShoppingItem) -> Unit
) : ListAdapter<ShoppingItem, ShoppingItemAdapter.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_shopping, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    fun getItemAt(position: Int): ShoppingItem {
        return getItem(position)
    }

    inner class ViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

        private val textViewItemName: TextView = itemView.findViewById(R.id.textViewItemName)
        private val textViewCategory: TextView = itemView.findViewById(R.id.textViewCategory)
        private val textViewQuantity: TextView = itemView.findViewById(R.id.textViewQuantity)
        private val checkBoxItem: CheckBox = itemView.findViewById(R.id.checkBoxItem)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClicked(getItem(position))
                }
            }

            checkBoxItem.setOnCheckedChangeListener { _, isChecked ->
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemChecked(getItem(position), isChecked)
                }
            }
        }

        fun bind(item: ShoppingItem) {
            textViewItemName.text = item.name
            textViewCategory.text = item.category
            textViewQuantity.text = item.quantity.toString()
            checkBoxItem.isChecked = item.isChecked

            // Appliquer le style barré si l'élément est coché
            if (item.isChecked) {
                textViewItemName.paintFlags = textViewItemName.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                textViewItemName.paintFlags = textViewItemName.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<ShoppingItem>() {
            override fun areItemsTheSame(oldItem: ShoppingItem, newItem: ShoppingItem): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: ShoppingItem, newItem: ShoppingItem): Boolean {
                return oldItem == newItem
            }
        }
    }
}