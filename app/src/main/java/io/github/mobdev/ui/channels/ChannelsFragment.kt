package io.github.mobdev.ui.channels

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import io.github.mobdev.ChatApp
import io.github.mobdev.R
import io.github.mobdev.databinding.DialogCreateChannelBinding
import io.github.mobdev.databinding.FragmentChannelsBinding
import io.github.mobdev.ui.NavigationViewModel
import kotlinx.coroutines.launch

class ChannelsFragment : Fragment(R.layout.fragment_channels) {

    private var binding: FragmentChannelsBinding? = null
    private lateinit var adapter: ChannelsAdapter

    private val viewModel: ChannelsViewModel by activityViewModels {
        val app = requireActivity().application as ChatApp
        ChannelsViewModel.Factory(app.repository)
    }
    private val navigationViewModel: NavigationViewModel by activityViewModels()

    interface ChannelsListener {
        fun onChannelSelected(name: String)
        fun onLogoutRequested()
        fun onUnauthorized()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val b = FragmentChannelsBinding.bind(view)
        binding = b

        adapter = ChannelsAdapter { name ->
            navigationViewModel.selectChannel(name)
            (activity as? ChannelsListener)?.onChannelSelected(name)
        }
        b.channelsList.layoutManager = LinearLayoutManager(requireContext())
        b.channelsList.adapter = adapter

        b.swipeRefresh.setOnRefreshListener { viewModel.load() }

        b.toolbar.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.action_logout) {
                (activity as? ChannelsListener)?.onLogoutRequested()
                true
            } else false
        }

        b.newChannelFab.setOnClickListener { showCreateChannelDialog() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.state.collect { applyState(it) } }
                launch { navigationViewModel.state.collect { syncSelection(it.selectedChannel) } }
                launch { viewModel.events.collect { handleEvent(it) } }
                launch {
                    viewModel.refreshComplete.collect {
                        binding?.swipeRefresh?.isRefreshing = false
                    }
                }
            }
        }
    }

    private fun applyState(state: ChannelsViewModel.State) {
        val b = binding ?: return
        if (state is ChannelsViewModel.State.Loading) b.swipeRefresh.isRefreshing = true
        when (state) {
            is ChannelsViewModel.State.Loaded -> {
                val selected = navigationViewModel.state.value.selectedChannel
                adapter.submitList(state.channels.map { ChannelRow(it, it == selected) })
                b.emptyView.visibility = if (state.channels.isEmpty()) View.VISIBLE else View.GONE
            }
            is ChannelsViewModel.State.Error -> {
                Snackbar.make(b.root, state.text, Snackbar.LENGTH_LONG)
                    .setAction(R.string.action_retry) { viewModel.load() }
                    .show()
            }
            is ChannelsViewModel.State.Unauthorized -> {
                (activity as? ChannelsListener)?.onUnauthorized()
            }
            is ChannelsViewModel.State.Loading -> Unit
        }
    }

    private fun handleEvent(event: ChannelsViewModel.Event) {
        val b = binding ?: return
        when (event) {
            is ChannelsViewModel.Event.ChannelCreated -> {
                navigationViewModel.selectChannel(event.name)
                (activity as? ChannelsListener)?.onChannelSelected(event.name)
            }
            is ChannelsViewModel.Event.Message -> {
                Snackbar.make(b.root, event.text, Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun showCreateChannelDialog() {
        val dialogBinding = DialogCreateChannelBinding.inflate(LayoutInflater.from(requireContext()))
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(R.string.dialog_new_channel_title)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.dialog_create, null)
            .setNegativeButton(R.string.dialog_cancel, null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val raw = dialogBinding.channelNameInput.text?.toString().orEmpty().trim()
                if (raw.isEmpty()) {
                    dialogBinding.channelNameField.error = getString(R.string.error_empty_channel_name)
                    return@setOnClickListener
                }
                val channelName = if (raw.contains('@')) raw else "$raw@channel"
                val app = requireActivity().application as ChatApp
                val user = app.credentialStore.login.orEmpty()
                val firstMessage = getString(R.string.created_by_template, user)
                viewModel.createChannel(channelName, firstMessage)
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun syncSelection(selected: String?) {
        val current = adapter.currentList
        if (current.isEmpty()) return
        val updated = current.map { ChannelRow(it.name, it.name == selected) }
        if (updated != current) adapter.submitList(updated)
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }
}
