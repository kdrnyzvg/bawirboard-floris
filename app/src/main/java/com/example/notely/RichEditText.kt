package com.example.notely

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatEditText

/**
 * An EditText that supports rich text (via spans) and reports taps on inline
 * [AudioSpan] chips so the host can play the recording.
 */
class RichEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.editTextStyle
) : AppCompatEditText(context, attrs, defStyleAttr) {

    var onAudioTap: ((AudioSpan) -> Unit)? = null

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            val editable = text
            if (editable != null && editable.isNotEmpty()) {
                val offset = getOffsetForPosition(event.x, event.y)
                if (offset in 0 until editable.length) {
                    val spans = editable.getSpans(offset, offset + 1, AudioSpan::class.java)
                    for (span in spans) {
                        if (editable.getSpanStart(span) <= offset &&
                            offset < editable.getSpanEnd(span)
                        ) {
                            onAudioTap?.invoke(span)
                            return true
                        }
                    }
                }
            }
        }
        return super.onTouchEvent(event)
    }
}
