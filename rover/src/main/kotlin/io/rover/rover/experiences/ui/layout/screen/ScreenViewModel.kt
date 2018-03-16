package io.rover.rover.experiences.ui.layout.screen

import io.rover.rover.core.data.domain.Screen
import io.rover.rover.core.data.domain.TitleBarButtons
import io.rover.rover.core.streams.Observable
import io.rover.rover.core.streams.asPublisher
import io.rover.rover.core.streams.flatMap
import io.rover.rover.core.streams.map
import io.rover.rover.core.data.domain.Row
import io.rover.rover.core.logging.log
import io.rover.rover.experiences.ui.blocks.concerns.background.BackgroundViewModelInterface
import io.rover.rover.experiences.ui.blocks.concerns.layout.LayoutableViewModel
import io.rover.rover.experiences.ui.toolbar.ToolbarConfiguration
import io.rover.rover.experiences.ui.layout.DisplayItem
import io.rover.rover.experiences.ui.layout.Layout
import io.rover.rover.experiences.types.RectF
import io.rover.rover.experiences.types.asAndroidColor
import io.rover.rover.experiences.ui.layout.row.RowViewModelInterface

class ScreenViewModel(
    private val screen: Screen,
    private val backgroundViewModel: BackgroundViewModelInterface,
    private val resolveNavigationViewModel: (row: Row) -> RowViewModelInterface
) : ScreenViewModelInterface,
    BackgroundViewModelInterface by backgroundViewModel {

    // TODO: remember (State) scroll position

    private val rowsById : Map<String, Row> = screen.rows.associateBy { it.id.rawValue }.apply {
        if(this.size != screen.rows.size) {
            throw RuntimeException("Duplicate screen IDs appeared in screen $screenId.")
        }
    }

    /**
     * Map of Row View models and the Row ids they own.  Map entry order is their order in the
     * Experience.
     */
    private val rowViewModelsById : Map<String, RowViewModelInterface> by lazy {
        rowsById.mapValues { (_, row) ->
            // TODO: why on earth is this copy() here?
            resolveNavigationViewModel(row.copy(blocks = row.blocks))
        }
    }

    override val rowViewModels by lazy {
        rowViewModelsById.values.toList()
    }

    override fun gather(): List<LayoutableViewModel> {
        return rowViewModels.flatMap { rowViewModel ->
            listOf(
                rowViewModel
            ) + rowViewModel.blockViewModels.asReversed()
        }
    }

    override val events: Observable<ScreenViewModelInterface.Event> by lazy {
        rowViewModelsById.entries.asPublisher().flatMap { (rowId, rowViewModel) ->
            rowViewModel.eventSource.map { rowEvent ->
                ScreenViewModelInterface.Event(
                    rowId,
                    rowEvent.blockId,
                    rowEvent.navigateTo
                )
            }
        }
    }

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

        val rowViewModel = remainingRowViewModels.first()

        val rowBounds = RectF(
            0f,
            results.height,
            width,
            // the bottom value of the bounds is not used; the rows expand themselves as defined
            // or needed by autoheight content.
            0.0f
        )

        val rowFrame = rowViewModel.frame(rowBounds)

        val tail = remainingRowViewModels.subList(1, remainingRowViewModels.size)

        val rowHead = listOf(DisplayItem(rowFrame, null, rowViewModel))

        // Lay out the blocks, and then reverse the list to suit the requirement that *later* items
        // in the list must occlude prior ones.
        val blocks = rowViewModel.mapBlocksToRectDisplayList(rowFrame).asReversed()

        return mapRowsToRectDisplayList(tail, width, Layout(results.coordinatesAndViewModels + rowHead + blocks, results.height + rowViewModel.frame(rowBounds).height(), results.width))
    }

    override val screenId: String
        get() = screen.id.rawValue
}
