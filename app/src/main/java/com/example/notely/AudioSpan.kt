package com.example.notely

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.text.style.ReplacementSpan

/**
 * An inline, tappable audio chip rendered directly in the text flow.
 * It is backed by a single placeholder character in the Editable, so it can
 * live anywhere — including mid-sentence — and moves naturally as text is edited.
 */
class AudioSpan(
    val fileName: String,
    val durationSec: Int
) : ReplacementSpan() {

    private val label: String
        get() = "▶  " + formatDuration(durationSec)

    override fun getSize(
        paint: Paint,
        text: CharSequence?,
        start: Int,
        end: Int,
        fm: Paint.FontMetricsInt?
    ): Int {
        val padH = paint.textSize * 0.6f
        if (fm != null) {
            val top = (paint.ascent() * 1.15f).toInt()
            val bottom = (paint.descent() * 1.15f).toInt()
            fm.ascent = top
            fm.top = top
            fm.descent = bottom
            fm.bottom = bottom
        }
        return (paint.measureText(label) + padH * 2).toInt()
    }

    override fun draw(
        canvas: Canvas,
        text: CharSequence?,
        start: Int,
        end: Int,
        x: Float,
        top: Int,
        y: Int,
        bottom: Int,
        paint: Paint
    ) {
        val padH = paint.textSize * 0.6f
        val width = paint.measureText(label) + padH * 2
        val rect = RectF(x, y + paint.ascent() * 1.1f, x + width, y + paint.descent() * 1.1f)
        val radius = rect.height() / 2f

        val savedColor = paint.color
        paint.color = Color.parseColor("#0A7EA4")
        canvas.drawRoundRect(rect, radius, radius, paint)
        paint.color = Color.WHITE
        canvas.drawText(label, x + padH, y.toFloat(), paint)
        paint.color = savedColor
    }

    private fun formatDuration(sec: Int): String {
        val m = sec / 60
        val s = sec % 60
        return "%d:%02d".format(m, s)
    }
}
