package com.klortek.velora.screens

private val spanishGenreNames = mapOf(
    "Action" to "Acción",
    "Adventure" to "Aventura",
    "Animation" to "Animación",
    "Comedy" to "Comedia",
    "Crime" to "Crimen",
    "Documentary" to "Documental",
    "Drama" to "Drama",
    "Family" to "Familia",
    "Fantasy" to "Fantasía",
    "History" to "Historia",
    "Horror" to "Terror",
    "Music" to "Música",
    "Mystery" to "Misterio",
    "Romance" to "Romance",
    "Science Fiction" to "Ciencia ficción",
    "Sci-Fi" to "Ciencia ficción",
    "Thriller" to "Suspense",
    "War" to "Bélica",
    "Western" to "Western"
)

fun localizedGenreName(name: String): String =
    spanishGenreNames.entries.firstOrNull { it.key.equals(name.trim(), ignoreCase = true) }?.value
        ?: name
