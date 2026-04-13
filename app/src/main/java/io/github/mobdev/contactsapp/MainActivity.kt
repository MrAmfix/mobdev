package io.github.mobdev.contactsapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.mobdev.contactsapp.ui.navigation.AppNavigation
import io.github.mobdev.contactsapp.ui.screens.PermissionScreen
import io.github.mobdev.contactsapp.ui.theme.ContactsAppTheme
import io.github.mobdev.contactsapp.viewmodel.ContactsViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ContactsAppTheme {
                ContactsApp()
            }
        }
    }
}

@Composable
private fun ContactsApp() {
    val context = LocalContext.current
    val viewModel: ContactsViewModel = viewModel()

    var permissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var permanentlyDenied by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            permissionGranted = true
            viewModel.resetAndReload()
        } else {
            val activity = context as? android.app.Activity
            permanentlyDenied = activity != null &&
                !activity.shouldShowRequestPermissionRationale(Manifest.permission.READ_CONTACTS)
        }
    }

    val openAppSettings: () -> Unit = {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }

    AnimatedContent(
        targetState = permissionGranted,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "permission_transition"
    ) { granted ->
        if (granted) {
            AppNavigation(viewModel = viewModel)
        } else {
            PermissionScreen(
                isPermanentlyDenied = permanentlyDenied,
                onRequestPermission = {
                    permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                },
                onOpenSettings = openAppSettings
            )
        }
    }
}
