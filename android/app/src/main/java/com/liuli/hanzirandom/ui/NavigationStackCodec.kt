package com.liuli.hanzirandom.ui

sealed interface AppScreen {
    data object Main : AppScreen
    data class Detail(val character: String) : AppScreen
    data class Review(val characters: List<String>, val id: Long) : AppScreen
}

internal object NavigationStackCodec {
    private const val SEPARATOR = "\u001F"

    fun encode(screen: AppScreen): String = when (screen) {
        AppScreen.Main -> "main"
        is AppScreen.Detail -> "detail" + SEPARATOR + screen.character
        is AppScreen.Review -> "review" + SEPARATOR + screen.id + SEPARATOR +
            screen.characters.joinToString(SEPARATOR)
    }

    fun decode(encoded: String): AppScreen? {
        val parts = encoded.split(SEPARATOR)
        return when (parts.firstOrNull()) {
            "main" -> if (parts.size == 1) AppScreen.Main else null
            "detail" -> {
                val character = parts.getOrNull(1)
                if (parts.size == 2 && !character.isNullOrEmpty()) {
                    AppScreen.Detail(character)
                } else {
                    null
                }
            }
            "review" -> {
                val id = parts.getOrNull(1)?.toLongOrNull()
                val characters = parts.drop(2).filter { it.isNotEmpty() }
                if (parts.size >= 3 && id != null && characters.isNotEmpty()) {
                    AppScreen.Review(characters, id)
                } else {
                    null
                }
            }
            else -> null
        }
    }
}
