package io.rover.rover.plugins.userexperience.experience.layout.row

import io.rover.rover.core.streams.Observable
import io.rover.rover.plugins.userexperience.experience.blocks.concerns.background.BackgroundViewModelInterface
import io.rover.rover.plugins.userexperience.experience.blocks.concerns.layout.BlockViewModelInterface
import io.rover.rover.plugins.userexperience.experience.blocks.concerns.layout.LayoutableViewModel
import io.rover.rover.plugins.userexperience.experience.layout.DisplayItem
import io.rover.rover.plugins.userexperience.experience.navigation.NavigateTo
import io.rover.rover.experiences.types.RectF

/**
 * View model for Rover UI blocks.
 */
interface RowViewModelInterface : LayoutableViewModel, BackgroundViewModelInterface {
    val blockViewModels: List<BlockViewModelInterface>

    /**
     * Render all the blocks to a list of coordinates (and the [BlockViewModelInterface]s
     * themselves).
     */
    fun mapBlocksToRectDisplayList(
        rowFrame: RectF
    ): List<DisplayItem>

    /**
     * Rows may emit navigation events.
     */
    val eventSource: Observable<Event>

    /**
     * Does this row contain anything that calls for the backlight to be set temporarily extra
     * bright?
     */
    val needsBrightBacklight: Boolean

    data class Event(
        val blockId: String,
        val navigateTo: NavigateTo
    )
}
