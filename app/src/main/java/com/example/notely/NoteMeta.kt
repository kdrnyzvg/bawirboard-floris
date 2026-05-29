package com.example.notely

/** Lightweight metadata shown in the notes list. */
data class NoteMeta(
    val id: String,
    val title: String,
    val snippet: String,
    val updatedAt: Long
)
