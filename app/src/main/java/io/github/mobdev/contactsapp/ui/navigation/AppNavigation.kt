package io.github.mobdev.contactsapp.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.github.mobdev.contactsapp.ui.screens.ContactDetailScreen
import io.github.mobdev.contactsapp.ui.screens.ContactsListScreen
import io.github.mobdev.contactsapp.viewmodel.ContactsViewModel

sealed class Screen(val route: String) {
    data object ContactsList : Screen("contacts_list")
    data object ContactDetail : Screen("contact_detail/{contactId}") {
        fun createRoute(contactId: Long) = "contact_detail/$contactId"
    }
}

private const val SLIDE_DURATION_MS = 300

@Composable
fun AppNavigation(viewModel: ContactsViewModel) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.ContactsList.route
    ) {
        composable(
            route = Screen.ContactsList.route,
            exitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Start,
                    tween(SLIDE_DURATION_MS)
                )
            },
            popEnterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.End,
                    tween(SLIDE_DURATION_MS)
                )
            }
        ) {
            ContactsListScreen(
                viewModel = viewModel,
                onContactClick = { contactId ->
                    navController.navigate(Screen.ContactDetail.createRoute(contactId))
                }
            )
        }

        composable(
            route = Screen.ContactDetail.route,
            arguments = listOf(
                navArgument("contactId") { type = NavType.LongType }
            ),
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Start,
                    tween(SLIDE_DURATION_MS)
                )
            },
            popExitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.End,
                    tween(SLIDE_DURATION_MS)
                )
            }
        ) { backStackEntry ->
            val contactId = backStackEntry.arguments?.getLong("contactId") ?: return@composable
            val contact = viewModel.getContactById(contactId)
            ContactDetailScreen(
                contact = contact,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
