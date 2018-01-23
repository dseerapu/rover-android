package io.rover.rover.plugins.userexperience.experience.layout.screen

import io.rover.rover.plugins.data.domain.Screen
import io.rover.rover.plugins.data.domain.TitleBarButtons
import io.rover.rover.core.streams.Observable
import io.rover.rover.core.streams.asPublisher
import io.rover.rover.core.streams.flatMap
import io.rover.rover.plugins.userexperience.experience.ViewModelFactoryInterface
import io.rover.rover.plugins.userexperience.experience.blocks.concerns.background.BackgroundViewModelInterface
import io.rover.rover.plugins.userexperience.experience.blocks.concerns.layout.LayoutableViewModel
import io.rover.rover.plugins.userexperience.experience.toolbar.ToolbarConfiguration
import io.rover.rover.plugins.userexperience.experience.layout.DisplayItem
import io.rover.rover.plugins.userexperience.experience.layout.Layout
import io.rover.rover.plugins.userexperience.experience.navigation.NavigateTo
import io.rover.rover.plugins.userexperience.types.RectF
import io.rover.rover.plugins.userexperience.types.asAndroidColor
import io.rover.rover.plugins.userexperience.experience.layout.row.RowViewModelInterface

class ScreenViewModel(
    private val screen: Screen,
    private val backgroundViewModel: BackgroundViewModelInterface,
    private val viewModelFactory: ViewModelFactoryInterface
) : ScreenViewModelInterface,
    BackgroundViewModelInterface by backgroundViewModel {

    // TODO: remember (State) scroll position

    override val rowViewModels by lazy {
        screen.rows.map { row ->
            viewModelFactory.viewModelForRow(
                row.copy(blocks = row.blocks)
            )
        }
    }

    override fun gather(): List<LayoutableViewModel> {
        return rowViewModels.flatMap {
            listOf(
                it
            ) + it.blockViewModels.asReversed()
        }
    }

    override val events: Observable<NavigateTo> = rowViewModels.asPublisher().flatMap { it.eventSource }

    override val needsBrightBacklight: Boolean by lazy {
        rowViewModels.any { it.needsBrightBacklight }
    }

    override val appBarConfiguration: ToolbarConfiguration
        get() = ToolbarConfiguration(
            screen.useDefaultTitleBarStyle,
            screen.titleBarText,
            screen.titleBarBackgroundColor.asAndroidColor(),
            screen.titleBarTextColor.asAndroidColor(),
            screen.titleBarButtonColor.asAndroidColor(),
            screen.titleBarButtons == TitleBarButtons.Both || screen.titleBarButtons == TitleBarButtons.Back,
            screen.titleBarButtons == TitleBarButtons.Both || screen.titleBarButtons == TitleBarButtons.Close,
            screen.statusBarColor.asAndroidColor()
        )

    override fun render(
        widthDp: Float
    ): Layout =
        mapRowsToRectDisplayList(rowViewModels, widthDp)

    private tailrec fun mapRowsToRectDisplayList(
        remainingRowViewModels: List<RowViewModelInterface>,
        width: Float,
        results: Layout = Layout(listOf(), 0f, width)
    ): Layout {
        if (remainingRowViewModels.isEmpty()) {
            return results
        }

        val row = remainingRowViewModels.first()

        val rowBounds = RectF(
            0f,
            results.height,
            width,
            // the bottom value of the bounds is not used; the rows expand themselves as defined
            // or needed by autoheight content.
            0.0f
        )

        val rowFrame = row.frame(rowBounds)

        val tail = remainingRowViewModels.subList(1, remainingRowViewModels.size)

        val rowHead = listOf(DisplayItem(rowFrame, null, row))

        // Lay out the blocks, and then reverse the list to suit the requirement that *later* items
        // in the list must occlude prior ones.
        val blocks = row.mapBlocksToRectDisplayList(rowFrame).asReversed()

        return mapRowsToRectDisplayList(tail, width, Layout(results.coordinatesAndViewModels + rowHead + blocks, results.height + row.frame(rowBounds).height(), results.width))
    }
}
