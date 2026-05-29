package com.example.notely

import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import org.json.JSONArray
import org.json.JSONObject

/**
 * Converts a styled note body to/from JSON so formatting (and inline audio
 * chips) survive being saved to disk and reloaded.
 */
object RichTextSerializer {

    /** Object Replacement Character — the placeholder an [AudioSpan] sits on. */
    const val OBJ = "￼"

    fun toJson(title: String, body: Spanned): JSONObject {
        val text = body.toString()
        val spans = JSONArray()

        body.getSpans(0, text.length, StyleSpan::class.java).forEach { s ->
            when (s.style) {
                Typeface.BOLD -> spans.put(span(body, s, "bold", null))
                Typeface.ITALIC -> spans.put(span(body, s, "italic", null))
            }
        }
        body.getSpans(0, text.length, ForegroundColorSpan::class.java).forEach { s ->
            spans.put(span(body, s, "color", s.foregroundColor.toString()))
        }
        body.getSpans(0, text.length, AbsoluteSizeSpan::class.java).forEach { s ->
            spans.put(span(body, s, "size", s.size.toString()))
        }
        body.getSpans(0, text.length, TypefaceSpan::class.java).forEach { s ->
            s.family?.let { spans.put(span(body, s, "font", it)) }
        }
        body.getSpans(0, text.length, AudioSpan::class.java).forEach { s ->
            spans.put(span(body, s, "audio", s.fileName).put("dur", s.durationSec))
        }

        return JSONObject().apply {
            put("title", title)
            put("text", text)
            put("spans", spans)
            put("updatedAt", System.currentTimeMillis())
        }
    }

    fun toSpannable(json: JSONObject): SpannableStringBuilder {
        val builder = SpannableStringBuilder(json.optString("text"))
        val spans = json.optJSONArray("spans") ?: return builder
        val len = builder.length
        for (i in 0 until spans.length()) {
            val o = spans.getJSONObject(i)
            val start = o.getInt("start").coerceIn(0, len)
            val end = o.getInt("end").coerceIn(start, len)
            val span: Any? = when (o.getString("type")) {
                "bold" -> StyleSpan(Typeface.BOLD)
                "italic" -> StyleSpan(Typeface.ITALIC)
                "color" -> ForegroundColorSpan(o.getString("value").toInt())
                "size" -> AbsoluteSizeSpan(o.getString("value").toInt())
                "font" -> TypefaceSpan(o.getString("value"))
                "audio" -> AudioSpan(o.getString("value"), o.optInt("dur", 1))
                else -> null
            }
            if (span != null) {
                builder.setSpan(span, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
        return builder
    }

    private fun span(text: Spanned, span: Any, type: String, value: String?): JSONObject {
        return JSONObject().apply {
            put("type", type)
            put("start", text.getSpanStart(span))
            put("end", text.getSpanEnd(span))
            if (value != null) put("value", value)
        }
    }
}
