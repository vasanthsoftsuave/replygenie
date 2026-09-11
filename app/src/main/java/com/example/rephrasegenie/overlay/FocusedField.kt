package com.example.rephrasegenie.overlay

import android.graphics.Rect

/**
 * The text field the user is currently typing in, as seen by the accessibility service.
 *
 * [bounds] is in screen pixels and decides where the bubble sits.
 */
data class FocusedField(
    val packageName: String,
    val bounds: Rect,
)

/** What the bubble is doing right now. */
sealed interface BubbleState {
    data object Idle : BubbleState
    data object Working : BubbleState
    data class Message(val text: String, val isError: Boolean) : BubbleState
}
