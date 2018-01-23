package io.rover.rover.plugins.userexperience

import io.rover.rover.plugins.userexperience.experience.blocks.concerns.text.Font
import io.rover.rover.plugins.userexperience.experience.blocks.concerns.text.FontAppearance
import io.rover.rover.plugins.userexperience.experience.blocks.barcode.BarcodeViewModelInterface

interface MeasurementService {

    /**
     * Measure how much height a given bit of Unicode [richText] (with optional HTML tags such as
     * strong, italic, and underline) will require if soft wrapped to the given [width] and
     * [fontAppearance] ultimately meant to be displayed using an Android View.
     *
     * [boldFontAppearance] provides any font size or font-family modifications that should be
     * applied to bold text ( with no changes to colour or alignment).  This allows for nuanced
     * control of bold span styling.
     *
     * Returns the height needed to accommodate the text at the given width, in dps.
     */
    fun measureHeightNeededForRichText(
        richText: String,
        fontAppearance: FontAppearance,
        boldFontAppearance: Font,
        width: Float
    ): Float

    /**
     * Measure how much height a given bit of Unicode [richText] (with optional HTML tags such as
     * strong, italic, and underline) will require if soft wrapped to the given [width] and
     * [fontAppearance] ultimately meant to be displayed using an Android View.
     *
     * [boldFontAppearance] provides any font size or font-family modifications that should be
     * applied to bold text ( with no changes to colour or alignment).  This allows for nuanced
     * control of bold span styling.
     *
     * Returns the height needed to accommodate the barcode, at the correct aspect, at the given
     * width, in dps.
     */
    fun measureHeightNeededForBarcode(
        text: String,
        type: BarcodeViewModelInterface.BarcodeType,
        width: Float
    ): Float
}
