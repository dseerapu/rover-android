package io.rover.rover.ui.experience.blocks.concerns.text

import io.rover.rover.ui.experience.blocks.concerns.layout.Measurable

interface ViewTextInterface {
    var textViewModel: TextViewModelInterface?
}

/**
 * View Model for block content that contains rich text content (decorated with strong, italic, and
 * underline HTML tags).
 */
interface TextViewModelInterface : Measurable {
    val text: String

    val singleLine: Boolean

    /**
     * Should the view configure the Android text view with a vertically centering gravity?
     */
    val centerVertically: Boolean

    val fontAppearance: FontAppearance

    fun boldRelativeToBlockWeight(): Font
}