package com.example.rephrasegenie.domain.model

/**
 * The swatches offered in the Tone Builder.
 *
 * The Windows app has a drag canvas with a brightness bar and three number boxes, which needs a
 * mouse — and has a real bug, where the blue value is worked out from green. A grid of fixed
 * swatches works with a thumb and cannot produce a wrong colour.
 *
 * The eight built-in tone colours are in the list on purpose: they show as taken, so the user can
 * see why they cannot be picked.
 */
object ToneColors {

    val PALETTE: List<String> = listOf(
        "#E0245E", "#DC2626", "#F97316", "#F59E0B",
        "#EAB308", "#84CC16", "#10B981", "#059669",
        "#14B8A6", "#06B6D4", "#0EA5E9", "#3B82F6",
        "#6366F1", "#8B5CF6", "#A855F7", "#D946EF",
        "#EC4899", "#F472B6", "#64748B", "#6B7280",
        "#78716C", "#0F766E", "#7C3AED", "#B91C1C",
    )

    /** "#0ea5e9", "0EA5E9" and "#0EA5E9" are the same colour. Compared in this form. */
    fun normalize(hex: String?): String? {
        val cleaned = hex?.trim()?.removePrefix("#")?.uppercase() ?: return null
        return if (cleaned.length == 6 && cleaned.all { it in "0123456789ABCDEF" }) {
            "#$cleaned"
        } else {
            null
        }
    }
}
