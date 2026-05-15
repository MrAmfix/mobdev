package io.github.mobdev.data.api

import io.github.mobdev.data.CredentialStore
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val store: CredentialStore) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val token = store.token
        val request = if (token != null) {
            original.newBuilder()
                .header("X-Auth-Token", token)
                .build()
        } else {
            original
        }
        return chain.proceed(request)
    }
}
