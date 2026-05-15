package io.github.mobdev.data

import android.content.Context
import androidx.core.content.edit

class CredentialStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    var login: String?
        get() = prefs.getString(KEY_LOGIN, null)
        set(value) = prefs.edit { putString(KEY_LOGIN, value) }

    var password: String?
        get() = prefs.getString(KEY_PASSWORD, null)
        set(value) = prefs.edit { putString(KEY_PASSWORD, value) }

    var token: String?
        get() = prefs.getString(KEY_TOKEN, null)
        set(value) = prefs.edit { putString(KEY_TOKEN, value) }

    fun hasCredentials(): Boolean = !login.isNullOrEmpty() && !password.isNullOrEmpty()

    fun clear() {
        prefs.edit {
            remove(KEY_LOGIN)
            remove(KEY_PASSWORD)
            remove(KEY_TOKEN)
        }
    }

    fun clearToken() {
        prefs.edit { remove(KEY_TOKEN) }
    }

    companion object {
        private const val PREFS = "credentials"
        private const val KEY_LOGIN = "login"
        private const val KEY_PASSWORD = "password"
        private const val KEY_TOKEN = "token"
    }
}
