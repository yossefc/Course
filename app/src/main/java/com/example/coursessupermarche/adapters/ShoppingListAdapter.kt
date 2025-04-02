package com.example.coursessupermarche.adapters

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.coursessupermarche.R
import com.example.coursessupermarche.models.ShoppingList
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.firebase.auth.FirebaseAuth

class ShoppingListAdapter(
    private val onListClicked: (ShoppingList) -> Unit,
    private val onMoreClicked: (ShoppingList, View) -> Unit
) : ListAdapter<ShoppingList, ShoppingListAdapter.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_shopping_list, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textViewListName: TextView = itemView.findViewById(R.id.textViewListName)
        private val textViewItemCount: TextView = itemView.findViewById(R.id.textViewItemCount)
        private val textViewLastUpdate: TextView = itemView.findViewById(R.id.textViewLastUpdate)
        private val imageViewShared: ImageView = itemView.findViewById(R.id.imageViewShared)
        private val buttonMore: ImageButton = itemView.findViewById(R.id.buttonMore)
        private val progressIndicator: LinearProgressIndicator = itemView.findViewById(R.id.progressIndicator)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onListClicked(getItem(position))
                }
            }

            buttonMore.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onMoreClicked(getItem(position), buttonMore)
                }
            }
        }

        fun bind(list: ShoppingList) {
            textViewListName.text = list.name

            // Afficher le nombre d'articles
            val itemCountText = itemView.context.resources.getQuantityString(
                R.plurals.item_count,
                list.totalItems,
                list.totalItems
            )
            textViewItemCount.text = itemCountText

            // Afficher la date de dernière mise à jour
            val relativeTimeSpan = DateUtils.getRelativeTimeSpanString(
                list.updatedAt.time,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS
            )
            textViewLastUpdate.text = itemView.context.getString(
                R.string.last_updated,
                relativeTimeSpan
            )

            // Afficher l'indicateur de progression
            val progress = (list.progress * 100).toInt()
            progressIndicator.progress = progress

            // Afficher l'icône de partage si la liste est partagée
            if (list.isShared) {
                imageViewShared.visibility = View.VISIBLE
            } else {
                imageViewShared.visibility = View.GONE
            }

            // Mettre en évidence si l'utilisateur est le propriétaire
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
            if (list.ownerId == currentUserId) {
                // Propriétaire
                itemView.alpha = 1.0f
            } else {
                // Membre invité
                itemView.alpha = 0.9f
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<ShoppingList>() {
            override fun areItemsTheSame(oldItem: ShoppingList, newItem: ShoppingList): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: ShoppingList, newItem: ShoppingList): Boolean {
                return oldItem.name == newItem.name &&
                        oldItem.isShared == newItem.isShared &&
                        oldItem.updatedAt == newItem.updatedAt &&
                        oldItem.totalItems == newItem.totalItems &&
                        oldItem.completedItems == newItem.completedItems
            }
        }
    }
}