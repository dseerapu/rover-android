package io.rover.rover.ui.viewmodels

import io.rover.rover.core.domain.BarcodeBlock
import io.rover.rover.core.domain.ButtonBlock
import io.rover.rover.core.domain.Screen
import io.rover.rover.core.domain.WebViewBlock
import io.rover.rover.ui.ViewModelFactoryInterface
import io.rover.rover.ui.types.DisplayItem
import io.rover.rover.ui.types.Layout
import io.rover.rover.ui.types.RectF

class ScreenViewModel(
    private val screen: Screen,
    private val viewModelFactory: ViewModelFactoryInterface
) : ScreenViewModelInterface {

    // TODO: remember (State) scroll position

    override val rowViewModels by lazy {
        screen.rows.map { row ->
            // temporarily filter out unsupported blocks
            val filteredBlocks = row.blocks.filter {
                when (it) {
                    is WebViewBlock, is BarcodeBlock -> false
                    else -> true
                }
            }

            viewModelFactory.viewModelForRow(
                row.copy(blocks = filteredBlocks)
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
