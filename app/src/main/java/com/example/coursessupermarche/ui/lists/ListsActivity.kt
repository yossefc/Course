package com.example.coursessupermarche.ui.lists

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.PopupMenu
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.coursessupermarche.MainActivity
import com.example.coursessupermarche.R
import com.example.coursessupermarche.adapters.ShoppingListAdapter
import com.example.coursessupermarche.databinding.ActivityListsBinding
import com.example.coursessupermarche.databinding.DialogCreateListBinding
import com.example.coursessupermarche.models.ShoppingList
import com.example.coursessupermarche.utils.showSnackbar
import com.example.coursessupermarche.viewmodels.SharedListViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ListsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityListsBinding
    private val viewModel: SharedListViewModel by viewModels()
    private lateinit var listAdapter: ShoppingListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityListsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        setupRecyclerView()
        setupObservers()
        setupListeners()

        // Vérifier si l'activité a été lancée avec une URL d'invitation
        intent?.data?.let { uri ->
            if (uri.toString().contains("/invite/")) {
                processInvitationLink(uri.toString())
            }
        }
    }
    private fun showListOptionsMenu(list: ShoppingList, view: View) {
        val popup = PopupMenu(this, view)
        popup.menuInflater.inflate(R.menu.list_options_menu, popup.menu)

        // Vérification directe
        val isOwner = viewModel.isListOwner(list.id)
        popup.menu.findItem(R.id.action_delete_list).isVisible = isOwner

        // Si la liste n'est pas encore partagée, changer le texte du menu
        if (!list.isShared) {
            popup.menu.findItem(R.id.action_share_list).setTitle(R.string.share_list)
        } else {
            popup.menu.findItem(R.id.action_share_list).setTitle(R.string.reshare_list)
        }

        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_share_list -> {
                    viewModel.selectList(list.id)
                    viewModel.createInvitation(list.id)
                    true
                }
                R.id.action_leave_list -> {
                    confirmLeaveList(list.id, list.name)
                    true
                }
                R.id.action_delete_list -> {
                    confirmDeleteList(list.id, list.name)
                    true
                }
                else -> false
            }
        }

        popup.show()
    }
    private fun setupRecyclerView() {
        listAdapter = ShoppingListAdapter(
            onListClicked = { list ->
                openList(list)
            },
            onMoreClicked = { list, view ->
                showListOptionsMenu(list, view)
            }
        )

        binding.recyclerViewLists.apply {
            layoutManager = LinearLayoutManager(this@ListsActivity)
            adapter = listAdapter
        }
    }

    private fun setupObservers() {
        viewModel.sharedLists.observe(this) { lists ->
            listAdapter.submitList(lists)

            // Afficher un message si la liste est vide
            if (lists.isNullOrEmpty()) {
                binding.textViewEmpty.visibility = View.VISIBLE
                binding.recyclerViewLists.visibility = View.GONE
            } else {
                binding.textViewEmpty.visibility = View.GONE
                binding.recyclerViewLists.visibility = View.VISIBLE
            }
        }

        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(this) { errorMessage ->
            errorMessage?.let {
                binding.root.showSnackbar(it)
                viewModel.clearError()
            }
        }

        viewModel.successMessage.observe(this) { message ->
            message?.let {
                binding.root.showSnackbar(it)
                viewModel.clearSuccessMessage()
            }
        }

        viewModel.invitationUrl.observe(this) { url ->
            url?.let {
                val selectedList = viewModel.selectedList.value
                if (selectedList != null) {
                    viewModel.shareInvitationViaWhatsApp(selectedList.id)
                    viewModel.clearInvitationUrl()
                }
            }
        }
    }

    private fun setupListeners() {
        binding.fabAddList.setOnClickListener {
            showCreateListDialog()
        }
    }

    private fun showCreateListDialog() {
        val dialogBinding = DialogCreateListBinding.inflate(layoutInflater)

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogBinding.root)
            .setCancelable(true)
            .create()

        dialogBinding.buttonCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.buttonCreate.setOnClickListener {
            val listName = dialogBinding.editTextListName.text.toString().trim()
            if (listName.isNotEmpty()) {
                viewModel.createShoppingList(listName)
                dialog.dismiss()
            } else {
                dialogBinding.textInputLayoutListName.error = getString(R.string.field_required)
            }
        }

        dialog.show()
    }



    private fun confirmLeaveList(listId: String, listName: String) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.leave_list_title))
            .setMessage(getString(R.string.leave_list_message, listName))
            .setPositiveButton(R.string.leave) { _, _ ->
                viewModel.leaveSharedList(listId)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun confirmDeleteList(listId: String, listName: String) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.delete_list_title))
            .setMessage(getString(R.string.delete_list_message, listName))
            .setPositiveButton(R.string.delete) { _, _ ->
                viewModel.deleteList(listId)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun openList(list: ShoppingList) {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("LIST_ID", list.id)
        }
        startActivity(intent)
    }

    private fun processInvitationLink(url: String) {
        viewModel.processInvitationFromUrl(url)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.lists_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_sort_by_name -> {
                viewModel.sortListsByName()
                true
            }
            R.id.action_sort_by_update -> {
                viewModel.sortListsByUpdated()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}