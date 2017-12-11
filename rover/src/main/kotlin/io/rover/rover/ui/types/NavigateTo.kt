package io.rover.rover.ui.types

import java.net.URI

/**
 * Should navigate to the given URL or Screen.
 */
sealed class NavigateTo {
    class OpenUrlAction(
        val uri: URI
    ) : NavigateTo()

    class GoToScreenAction(
        val screenId: String
    ) : NavigateTo()
}
