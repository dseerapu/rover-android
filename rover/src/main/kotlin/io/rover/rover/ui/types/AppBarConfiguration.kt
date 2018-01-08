package io.rover.rover.ui.types

/**
 * App bar configuration.  Colour overrides, text, text colour, and status bar settings.
 */
data class AppBarConfiguration(
    val useGlobalTheme: Boolean,

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

// ViewAppBar
// ExperienceAppBarViewModel
// ExperienceAppBarViewModel will subscribe to ExperienceViewModel?
// ... ExperienceAppBarViewModel will want to be injected with the same instance of ExperienceViewModel as already exists.
