package io.github.mobdev.util

object Urls {
    private const val BASE = "https://faerytea.name"

    fun thumb(path: String): String = "$BASE/thumb/${path.trimStart('/')}"
    fun image(path: String): String = "$BASE/img/${path.trimStart('/')}"
}
