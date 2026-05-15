package io.github.mobdev.ui.login

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import io.github.mobdev.ChatApp
import io.github.mobdev.R
import io.github.mobdev.databinding.FragmentLoginBinding
import androidx.appcompat.app.AlertDialog
import kotlinx.coroutines.launch

class LoginFragment : Fragment(R.layout.fragment_login) {

    private var binding: FragmentLoginBinding? = null

    private val viewModel: LoginViewModel by viewModels {
        val app = requireActivity().application as ChatApp
        LoginViewModel.Factory(app.repository)
    }

    interface LoginListener {
        fun onLoggedIn()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val b = FragmentLoginBinding.bind(view)
        binding = b

        b.signInButton.setOnClickListener {
            val login = b.loginInput.text?.toString().orEmpty()
            val password = b.passwordInput.text?.toString().orEmpty()
            if (login.isBlank() || password.isBlank()) {
                showDialog(getString(R.string.error_empty_fields))
                return@setOnClickListener
            }
            viewModel.login(
                login,
                password,
                invalidCredsMessage = R.string.error_invalid_credentials,
                networkErrorMessage = R.string.error_network
            )
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state -> applyState(state) }
            }
        }
    }

    private fun applyState(state: LoginViewModel.State) {
        val b = binding ?: return
        when (state) {
            is LoginViewModel.State.Idle -> {
                b.progress.visibility = View.GONE
                b.signInButton.isEnabled = true
            }
            is LoginViewModel.State.Loading -> {
                b.progress.visibility = View.VISIBLE
                b.signInButton.isEnabled = false
            }
            is LoginViewModel.State.Error -> {
                b.progress.visibility = View.GONE
                b.signInButton.isEnabled = true
                showDialog(getString(state.messageRes))
                viewModel.consumeError()
            }
            is LoginViewModel.State.Success -> {
                b.progress.visibility = View.GONE
                (activity as? LoginListener)?.onLoggedIn()
            }
        }
    }

    private fun showDialog(message: String) {
        AlertDialog.Builder(requireContext())
            .setMessage(message)
            .setPositiveButton(R.string.dialog_ok, null)
            .show()
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }
}
