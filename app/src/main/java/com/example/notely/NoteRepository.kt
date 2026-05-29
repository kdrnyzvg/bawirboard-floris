package com.example.notely

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.util.UUID

/** Stores notes as JSON files and audio clips as m4a files in app-private storage. */
class NoteRepository(context: Context) {

    private val notesDir = File(context.filesDir, "notes").apply { mkdirs() }
    private val audioDir = File(context.filesDir, "audio").apply { mkdirs() }

    fun newId(): String = UUID.randomUUID().toString()

    fun newAudioName(): String = "audio_${System.currentTimeMillis()}.m4a"

    fun audioFile(name: String): File = File(audioDir, name)

    private fun fileFor(id: String) = File(notesDir, "$id.json")

    fun load(id: String): JSONObject? {
        val f = fileFor(id)
        if (!f.exists()) return null
        return try {
            JSONObject(f.readText())
        } catch (_: Exception) {
            null
        }
    }

    fun save(id: String, json: JSONObject) {
        json.put("id", id)
        fileFor(id).writeText(json.toString())
    }

    fun delete(id: String) {
        fileFor(id).delete()
    }

    fun list(): List<NoteMeta> {
        val files = notesDir.listFiles { f -> f.extension == "json" } ?: return emptyList()
        return files.mapNotNull { f ->
            try {
                val o = JSONObject(f.readText())
                val id = o.optString("id", f.nameWithoutExtension)
                val title = o.optString("title").ifBlank { "Untitled" }
                val text = o.optString("text").replace(RichTextSerializer.OBJ, "🔊")
                NoteMeta(id, title, text.trim().take(80), o.optLong("updatedAt"))
            } catch (_: Exception) {
                null
            }
        }.sortedByDescending { it.updatedAt }
    }
}
