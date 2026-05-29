package com.example.notely

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File

/** Thin wrapper around [MediaRecorder] that records AAC audio to an m4a file. */
class AudioRecorder(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var startMs: Long = 0L

    fun start(output: File) {
        val r = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
        r.setAudioSource(MediaRecorder.AudioSource.MIC)
        r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        r.setOutputFile(output.absolutePath)
        r.prepare()
        r.start()
        recorder = r
        startMs = System.currentTimeMillis()
    }

    /** Stops recording and returns the duration in whole seconds (minimum 1). */
    fun stop(): Int {
        val elapsed = ((System.currentTimeMillis() - startMs) / 1000).toInt()
        release()
        return maxOf(1, elapsed)
    }

    fun cancel() = release()

    private fun release() {
        try {
            recorder?.stop()
        } catch (_: Exception) {
            // recorder may not have captured anything yet; ignore
        } finally {
            recorder?.release()
            recorder = null
        }
    }
}
