package io.rover.rover.ui.types

/**
 * Tool bar configuration.  Colour overrides, text, text colour, and status bar settings.
 */
data class ToolbarConfiguration(
    val useExistingStyle: Boolean,

    val appBarText: String,

    val color: Int,
    val textColor: Int,

    val backButton: Boolean,
    val closeButton: Boolean,
    // TODO: button colour

    /**
     * If null, then the default material design behaviour should be used.
     */
    val statusBarColor: Int
)
