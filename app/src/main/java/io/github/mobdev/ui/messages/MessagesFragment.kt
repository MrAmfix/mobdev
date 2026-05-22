package io.github.mobdev.ui.messages

import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import io.github.mobdev.ChatApp
import io.github.mobdev.R
import io.github.mobdev.databinding.FragmentMessagesBinding
import io.github.mobdev.ui.NavigationViewModel
import io.github.mobdev.util.ImageCompressor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

class MessagesFragment : Fragment(R.layout.fragment_messages) {

    private var binding: FragmentMessagesBinding? = null
    private lateinit var adapter: MessagesAdapter
    private lateinit var channel: String

    private val viewModel: MessagesViewModel by lazy {
        val app = requireActivity().application as ChatApp
        ViewModelProvider(
            requireActivity(),
            MessagesViewModel.Factory(app.repository, channel)
        )["messages:$channel", MessagesViewModel::class.java]
    }
    private val navigationViewModel: NavigationViewModel by activityViewModels()

    private val pickImage = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) handlePickedImage(uri)
    }

    interface MessagesListener {
        fun onBackFromMessages()
        fun onImageRequested(link: String)
        fun onUnauthorized()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        channel = requireArguments().getString(ARG_CHANNEL)
            ?: error("Channel argument missing")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val b = FragmentMessagesBinding.bind(view)
        binding = b

        val app = requireActivity().application as ChatApp
        val currentUser = app.credentialStore.login.orEmpty()

        adapter = MessagesAdapter(currentUser) { link ->
            navigationViewModel.openImage(link)
            (activity as? MessagesListener)?.onImageRequested(link)
        }
        val lm = LinearLayoutManager(requireContext()).apply {
            reverseLayout = true
            stackFromEnd = false
        }
        b.messagesList.layoutManager = lm
        b.messagesList.adapter = adapter

        b.toolbar.title = getString(R.string.channel_prefix) + channel
        b.toolbar.setNavigationOnClickListener {
            (activity as? MessagesListener)?.onBackFromMessages()
        }

        b.swipeRefresh.setOnRefreshListener { viewModel.refresh() }

        b.sendButton.setOnClickListener {
            val text = b.messageInput.text?.toString().orEmpty().trim()
            if (text.isEmpty()) return@setOnClickListener
            viewModel.send(text)
            b.messageInput.text = null
        }

        b.attachButton.setOnClickListener {
            pickImage.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }

        b.messagesList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                val layoutManager = rv.layoutManager as LinearLayoutManager
                val lastVisible = layoutManager.findLastVisibleItemPosition()
                val total = layoutManager.itemCount
                if (lastVisible >= total - 3) {
                    viewModel.loadMore()
                }
            }
        })

        val currentGen = navigationViewModel.state.value.selectGeneration
        if (viewModel.lastConsumedSelectGen < currentGen) {
            viewModel.lastConsumedSelectGen = currentGen
            viewModel.loadInitial()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.state.collect { applyState(it) } }
                launch {
                    while (coroutineContext.isActive) {
                        delay(POLL_INTERVAL_MS)
                        viewModel.pollNewer()
                    }
                }
            }
        }
    }

    private fun handlePickedImage(uri: Uri) {
        val context = context ?: return
        val resolver = context.contentResolver

        viewLifecycleOwner.lifecycleScope.launch {
            val bytes = runCatching {
                withContext(Dispatchers.IO) {
                    ImageCompressor.compressToJpeg(resolver, uri)
                }
            }.getOrNull()
            val b = binding
            if (bytes == null || bytes.isEmpty()) {
                if (b != null) {
                    Snackbar.make(b.root, R.string.error_pick_image, Snackbar.LENGTH_LONG).show()
                }
                return@launch
            }
            val filename = "image_${System.currentTimeMillis()}.jpg"
            viewModel.sendImage(bytes, "image/jpeg", filename)
        }
    }

    private fun applyState(state: MessagesViewModel.UiState) {
        val b = binding ?: return
        b.swipeRefresh.isRefreshing = state.isLoading && state.allMessages.isEmpty()

        val lm = b.messagesList.layoutManager as LinearLayoutManager
        // position 0 is the visual bottom due to reverseLayout=true
        val isAtBottom = lm.findFirstVisibleItemPosition() <= 1
        val scrollDown = state.scrollToBottom || isAtBottom

        adapter.submitList(state.allMessages) {
            if (scrollDown) {
                b.messagesList.scrollToPosition(0)
                if (state.scrollToBottom) viewModel.consumeScrollToBottom()
            }
        }

        if (state.errorText != null) {
            Snackbar.make(b.root, state.errorText, Snackbar.LENGTH_LONG).show()
            viewModel.consumeError()
        }
        if (state.unauthorized) {
            (activity as? MessagesListener)?.onUnauthorized()
        }
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    companion object {
        private const val ARG_CHANNEL = "channel"
        private const val POLL_INTERVAL_MS = 3000L
        fun newInstance(channel: String): MessagesFragment = MessagesFragment().apply {
            arguments = Bundle().apply { putString(ARG_CHANNEL, channel) }
        }
    }
}
