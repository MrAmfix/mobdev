package io.github.mobdev

import android.app.Application
import io.github.mobdev.data.ChatRepository
import io.github.mobdev.data.CredentialStore

class ChatApp : Application() {

    val credentialStore: CredentialStore by lazy { CredentialStore(this) }
    val repository: ChatRepository by lazy { ChatRepository(credentialStore) }
}
