package com.example.notely

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.media.MediaPlayer
import android.os.Bundle
import android.os.SystemClock
import android.text.Editable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Chronometer
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.notely.databinding.ActivityEditorBinding

class EditorActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ID = "note_id"
    }

    private lateinit var binding: ActivityEditorBinding
    private lateinit var repo: NoteRepository
    private var noteId: String? = null
    private var player: MediaPlayer? = null
    private val recorder by lazy { AudioRecorder(this) }

    private val requestMic =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) showRecordDialog()
            else toast(getString(R.string.mic_permission_needed))
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        repo = NoteRepository(this)

        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { finish() }

        noteId = intent.getStringExtra(EXTRA_ID)
        noteId?.let { id ->
            repo.load(id)?.let { json ->
                binding.titleField.setText(json.optString("title"))
                binding.bodyField.setText(
                    RichTextSerializer.toSpannable(json),
                    TextView.BufferType.EDITABLE
                )
            }
        }

        binding.bodyField.onAudioTap = { playAudio(it) }
        buildFormatBar()
    }

    // ------------------------------------------------------------------
    // Formatting toolbar
    // ------------------------------------------------------------------

    private fun buildFormatBar() {
        val bar = binding.formatBar
        addButton(bar, "B", "Bold") { toggleStyle(Typeface.BOLD) }
        addButton(bar, "I", "Italic") { toggleStyle(Typeface.ITALIC) }
        addButton(bar, "Aa", "Font") { showFontMenu(it) }
        addButton(bar, "A±", "Size") { showSizeMenu(it) }
        addButton(bar, "🎨", "Color") { showColorDialog() }
        addButton(bar, "🎤", "Audio") { onMicClicked() }
    }

    private fun addButton(bar: LinearLayout, label: String, desc: String, onClick: (View) -> Unit) {
        val b = Button(this)
        b.text = label
        b.contentDescription = desc
        b.setTextColor(0xFFFFFFFF.toInt())
        b.setBackgroundColor(0x00000000)
        b.textSize = 17f
        b.minWidth = 0
        b.minimumWidth = 0
        b.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        b.setOnClickListener { onClick(b) }
        bar.addView(b)
    }

    // ------------------------------------------------------------------
    // Span application
    // ------------------------------------------------------------------

    /** Current selection as (start, end), or null (with a hint) if nothing is selected. */
    private fun selection(): Pair<Int, Int>? {
        val s = binding.bodyField.selectionStart
        val e = binding.bodyField.selectionEnd
        val start = minOf(s, e)
        val end = maxOf(s, e)
        if (start < 0 || start == end) {
            toast("Select some text first")
            return null
        }
        return start to end
    }

    private fun toggleStyle(style: Int) {
        val (start, end) = selection() ?: return
        val text = binding.bodyField.text ?: return
        val covered = text.getSpans(start, end, StyleSpan::class.java).any {
            it.style == style && text.getSpanStart(it) <= start && text.getSpanEnd(it) >= end
        }
        if (covered) {
            applyExclusive(
                text, StyleSpan::class.java, start, end,
                match = { it.style == style },
                clone = { StyleSpan(it.style) },
                newSpan = null
            )
        } else {
            text.setSpan(StyleSpan(style), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    private fun setColor(color: Int) {
        val (start, end) = selection() ?: return
        val text = binding.bodyField.text ?: return
        applyExclusive(
            text, ForegroundColorSpan::class.java, start, end,
            match = { true },
            clone = { ForegroundColorSpan(it.foregroundColor) },
            newSpan = ForegroundColorSpan(color)
        )
    }

    private fun setSizeSp(sp: Int) {
        val (start, end) = selection() ?: return
        val text = binding.bodyField.text ?: return
        val px = (sp * resources.displayMetrics.scaledDensity).toInt()
        applyExclusive(
            text, AbsoluteSizeSpan::class.java, start, end,
            match = { true },
            clone = { AbsoluteSizeSpan(it.size) },
            newSpan = AbsoluteSizeSpan(px)
        )
    }

    private fun setFont(family: String) {
        val (start, end) = selection() ?: return
        val text = binding.bodyField.text ?: return
        applyExclusive(
            text, TypefaceSpan::class.java, start, end,
            match = { true },
            clone = { TypefaceSpan(it.family ?: "sans-serif") },
            newSpan = TypefaceSpan(family)
        )
    }

    /**
     * Removes spans of [cls] matching [match] inside [start, end], re-adding the
     * portions that fall outside the selection (via [clone]) so neighbouring text
     * keeps its styling. Then applies [newSpan] across the selection if non-null.
     */
    private fun <T : Any> applyExclusive(
        text: Editable,
        cls: Class<T>,
        start: Int,
        end: Int,
        match: (T) -> Boolean,
        clone: (T) -> T,
        newSpan: T?
    ) {
        text.getSpans(start, end, cls).filter(match).forEach { s ->
            val ss = text.getSpanStart(s)
            val se = text.getSpanEnd(s)
            text.removeSpan(s)
            if (ss < start) text.setSpan(clone(s), ss, start, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            if (se > end) text.setSpan(clone(s), end, se, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        if (newSpan != null) text.setSpan(newSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    private fun showFontMenu(anchor: View) {
        val fonts = listOf("Sans" to "sans-serif", "Serif" to "serif", "Mono" to "monospace")
        val menu = PopupMenu(this, anchor)
        fonts.forEachIndexed { i, (name, _) -> menu.menu.add(0, i, i, name) }
        menu.setOnMenuItemClickListener { item ->
            setFont(fonts[item.itemId].second); true
        }
        menu.show()
    }

    private fun showSizeMenu(anchor: View) {
        val sizes = listOf("Small" to 14, "Normal" to 18, "Large" to 26, "Huge" to 36)
        val menu = PopupMenu(this, anchor)
        sizes.forEachIndexed { i, (name, _) -> menu.menu.add(0, i, i, name) }
        menu.setOnMenuItemClickListener { item ->
            setSizeSp(sizes[item.itemId].second); true
        }
        menu.show()
    }

    private fun showColorDialog() {
        val names = arrayOf("Default", "Red", "Orange", "Green", "Blue", "Purple", "Pink")
        val colors = intArrayOf(
            0xFF222222.toInt(), 0xFFD32F2F.toInt(), 0xFFF57C00.toInt(),
            0xFF388E3C.toInt(), 0xFF1976D2.toInt(), 0xFF7B1FA2.toInt(), 0xFFE91E63.toInt()
        )
        AlertDialog.Builder(this)
            .setTitle("Text color")
            .setItems(names) { _, which -> setColor(colors[which]) }
            .show()
    }

    // ------------------------------------------------------------------
    // Inline audio
    // ------------------------------------------------------------------

    private fun onMicClicked() {
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) showRecordDialog() else requestMic.launch(Manifest.permission.RECORD_AUDIO)
    }

    private fun showRecordDialog() {
        val fileName = repo.newAudioName()
        val file = repo.audioFile(fileName)
        try {
            recorder.start(file)
        } catch (_: Exception) {
            toast("Couldn't start recording")
            return
        }

        val chrono = Chronometer(this).apply {
            base = SystemClock.elapsedRealtime()
            start()
            textSize = 30f
            gravity = Gravity.CENTER
            setPadding(0, 48, 0, 48)
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.recording)
            .setView(chrono)
            .setCancelable(false)
            .setPositiveButton(R.string.stop_insert, null)
            .setNegativeButton(R.string.cancel, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                chrono.stop()
                insertAudio(fileName, recorder.stop())
                dialog.dismiss()
            }
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener {
                chrono.stop()
                recorder.cancel()
                file.delete()
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun insertAudio(fileName: String, durationSec: Int) {
        val body = binding.bodyField
        val text = body.text ?: return
        val pos = body.selectionStart.coerceIn(0, text.length)
        text.insert(pos, RichTextSerializer.OBJ)
        text.setSpan(
            AudioSpan(fileName, durationSec),
            pos, pos + 1,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        body.setSelection(pos + 1)
    }

    private fun playAudio(span: AudioSpan) {
        val file = repo.audioFile(span.fileName)
        if (!file.exists()) {
            toast("Audio clip is missing")
            return
        }
        player?.release()
        player = MediaPlayer().apply {
            setDataSource(file.absolutePath)
            setOnCompletionListener { mp ->
                mp.release()
                player = null
            }
            prepare()
            start()
        }
    }

    // ------------------------------------------------------------------
    // Persistence / lifecycle
    // ------------------------------------------------------------------

    override fun onPause() {
        super.onPause()
        saveNote()
    }

    override fun onDestroy() {
        super.onDestroy()
        player?.release()
        player = null
    }

    private fun saveNote() {
        val title = binding.titleField.text?.toString()?.trim().orEmpty()
        val body: Editable = binding.bodyField.text ?: SpannableStringBuilder()
        val plain = body.toString().replace(RichTextSerializer.OBJ, " ").trim()

        if (title.isBlank() && plain.isBlank()) {
            // nothing worth keeping
            noteId?.let { repo.delete(it) }
            return
        }
        val id = noteId ?: repo.newId().also { noteId = it }
        val effectiveTitle = title.ifBlank { plain.take(40).ifBlank { "Untitled" } }
        repo.save(id, RichTextSerializer.toJson(effectiveTitle, body))
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
