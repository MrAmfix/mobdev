package io.github.mobdev.ui

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import io.github.mobdev.ChatApp
import io.github.mobdev.R
import io.github.mobdev.databinding.ActivityMainBinding
import io.github.mobdev.ui.channels.ChannelsFragment
import io.github.mobdev.ui.image.ImageFragment
import io.github.mobdev.ui.login.LoginFragment
import io.github.mobdev.ui.messages.MessagesFragment
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(),
    LoginFragment.LoginListener,
    ChannelsFragment.ChannelsListener,
    MessagesFragment.MessagesListener,
    ImageFragment.ImageListener {

    private lateinit var binding: ActivityMainBinding
    private val navigationViewModel: NavigationViewModel by viewModels()

    private val isTwoPane: Boolean
        get() = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val app = application as ChatApp
        val authed = app.credentialStore.hasCredentials()

        renderTopology(authed, navigationViewModel.state.value, force = true)

        onBackPressedDispatcher.addCallback(this, BackCallback())

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                navigationViewModel.state
                    .collect { state ->
                        renderTopology(
                            (application as ChatApp).credentialStore.hasCredentials(),
                            state,
                            force = false
                        )
                    }
            }
        }
    }

    private fun renderTopology(authed: Boolean, state: NavigationViewModel.State, force: Boolean) {
        if (!authed) {
            ensurePrimary(::LoginFragment, TAG_LOGIN, force)
            if (isTwoPane) clearSecondary()
            return
        }
        if (isTwoPane) {
            ensurePrimary(::ChannelsFragment, TAG_CHANNELS, force)
            ensureSecondary(state, force)
        } else {
            val (factory, tag) = portraitTarget(state)
            ensurePrimary(factory, tag, force)
            clearSecondary()
        }
    }

    private fun portraitTarget(state: NavigationViewModel.State): Pair<() -> Fragment, String> = when {
        state.openImage != null -> ({ ImageFragment.newInstance(state.openImage) } to tagForImage(state.openImage))
        state.selectedChannel != null -> ({ MessagesFragment.newInstance(state.selectedChannel) } to tagForMessages(state.selectedChannel))
        else -> (::ChannelsFragment to TAG_CHANNELS)
    }

    private fun ensurePrimary(factory: () -> Fragment, tag: String, force: Boolean) {
        val current = supportFragmentManager.findFragmentById(R.id.primary_container)
        if (!force && current?.tag == tag) return
        supportFragmentManager.commit {
            replace(R.id.primary_container, factory(), tag)
        }
    }

    private fun ensureSecondary(state: NavigationViewModel.State, force: Boolean) {
        val (factory, tag) = secondaryTarget(state)
        val current = supportFragmentManager.findFragmentById(R.id.secondary_container)
        if (!force && current?.tag == tag) return
        supportFragmentManager.commit {
            replace(R.id.secondary_container, factory(), tag)
        }
    }

    private fun secondaryTarget(state: NavigationViewModel.State): Pair<() -> Fragment, String> = when {
        state.openImage != null -> ({ ImageFragment.newInstance(state.openImage) } to tagForImage(state.openImage))
        state.selectedChannel != null -> ({ MessagesFragment.newInstance(state.selectedChannel) } to tagForMessages(state.selectedChannel))
        else -> (::PlaceholderFragment to TAG_PLACEHOLDER)
    }

    private fun clearSecondary() {
        val current = supportFragmentManager.findFragmentById(R.id.secondary_container) ?: return
        supportFragmentManager.commit { remove(current) }
    }

    private fun tagForMessages(channel: String) = "messages:$channel"
    private fun tagForImage(link: String) = "image:$link"

    override fun onLoggedIn() {
        navigationViewModel.reset()
        recreate()
    }

    override fun onChannelSelected(name: String) {
        navigationViewModel.selectChannel(name)
    }

    override fun onLogoutRequested() {
        (application as ChatApp).credentialStore.clear()
        viewModelStore.clear()
        recreate()
    }

    override fun onUnauthorized() {
        (application as ChatApp).credentialStore.clear()
        viewModelStore.clear()
        recreate()
    }

    override fun onBackFromMessages() {
        navigationViewModel.selectChannel(null)
    }

    override fun onImageRequested(link: String) {
        navigationViewModel.openImage(link)
    }

    override fun onImageClosed() {
        navigationViewModel.closeImage()
    }

    private inner class BackCallback : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            val authed = (application as ChatApp).credentialStore.hasCredentials()
            if (!authed) {
                finish()
                return
            }
            val state = navigationViewModel.state.value
            when {
                state.openImage != null -> navigationViewModel.closeImage()
                isTwoPane && state.selectedChannel != null -> navigationViewModel.selectChannel(null)
                isTwoPane -> finish()
                state.selectedChannel != null -> navigationViewModel.selectChannel(null)
                else -> finish()
            }
        }
    }

    companion object {
        private const val TAG_LOGIN = "login"
        private const val TAG_CHANNELS = "channels"
        private const val TAG_PLACEHOLDER = "placeholder"
    }
}
