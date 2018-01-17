package io.rover.rover.ui.viewmodels

import io.rover.rover.core.domain.Row
import io.rover.rover.core.logging.log
import io.rover.rover.streams.Observable
import io.rover.rover.streams.asPublisher
import io.rover.rover.streams.filterNulls
import io.rover.rover.streams.flatMap
import io.rover.rover.streams.map
import io.rover.rover.streams.share
import io.rover.rover.ui.ViewModelFactoryInterface
import io.rover.rover.ui.experience.blocks.barcode.BarcodeBlockViewModel
import io.rover.rover.ui.experience.blocks.concerns.background.BackgroundViewModelInterface
import io.rover.rover.ui.types.measuredAgainst
import io.rover.rover.ui.types.DisplayItem
import io.rover.rover.ui.types.NavigateTo
import io.rover.rover.ui.types.RectF
import io.rover.rover.ui.types.ViewType

class RowViewModel(
    private val row: Row,
    private val viewModelFactory: ViewModelFactoryInterface,
    private val backgroundViewModel: BackgroundViewModelInterface
) : RowViewModelInterface, BackgroundViewModelInterface by backgroundViewModel {
    override val viewType: ViewType = ViewType.Row

    override val blockViewModels: List<BlockViewModelInterface> by lazy {
        row.blocks.map { viewModelFactory.viewModelForBlock(it) }
    }

    override val eventSource : Observable<NavigateTo> = blockViewModels.map { blockViewModel ->
        blockViewModel.events.map {
            when(it) {
                is BlockViewModelInterface.Event.Clicked -> it.navigateTo
                is BlockViewModelInterface.Event.Touched, is BlockViewModelInterface.Event.Released -> null
            }
        }.filterNulls()
    }.asPublisher().flatMap { it }.share().map {
        log.v("Row event: $it")
        it
    }

    /**
     * Returns the position (with origin being the bounds) that this view model should
     * be laid out.  Note that the returned rect is relative to the same space as the given [bounds]
     * rect, but not relative to the [bounds] rect itself.
     *
     * Also note that the [RectF.bottom] value of the [bounds] will be ignored; rows are entirely
     * responsible for defining their own heights, and are not height-constrained by the containing
     * [ScreenViewModel].
     */
    override fun frame(bounds: RectF): RectF {
        val x = bounds.left
        val y = bounds.top
        val width = bounds.width()

        val height = height(bounds)

        return RectF(
            x,
            y,
            x + width,
            y + height
        )
    }

    private fun height(bounds: RectF): Float {
        return if (row.autoHeight) {
            blockViewModels.map { it.stackedHeight(bounds) }.sum()
        } else {
            row.height.measuredAgainst(bounds.height())
        }
    }

    override fun mapBlocksToRectDisplayList(rowFrame: RectF): List<DisplayItem> {
        // kick off the iteration to map (for stacking, as needed) the blocks into
        // a flat layout of coordinates while accumulating the stack height.
        return mapBlocksToRectDisplayList(
            this.blockViewModels,
            rowFrame,
            0.0f,
            listOf()
        )
    }

    override val needsBrightBacklight: Boolean by lazy {
        blockViewModels.any { blockViewModel -> blockViewModel is BarcodeBlockViewModel }
    }

    private tailrec fun mapBlocksToRectDisplayList(
        remainingBlockViewModels: List<BlockViewModelInterface>,
        rowFrame: RectF,
        accumulatedStackHeight: Float,
        results: List<DisplayItem>
    ): List<DisplayItem> {
        if (remainingBlockViewModels.isEmpty()) {
            return results
        }
        val block = remainingBlockViewModels.first()

        // if we're stacked, we need to stack on top of any prior stacked elements.
        val stackDeflection = if (block.isStacked) accumulatedStackHeight else 0.0f

        val blockBounds = RectF(
            rowFrame.left,
            rowFrame.top + stackDeflection,
            rowFrame.right,
            rowFrame.bottom + stackDeflection
        )
        val blockFrame = block.frame(blockBounds)

        val tail = remainingBlockViewModels.subList(1, remainingBlockViewModels.size)

        // if blockFrame exceeds the blockBounds, we need clip, and in terms relative to blockBounds
        val clip = if (!rowFrame.contains(blockFrame)) {
            // and find the intersection with blockFrame to find out what should be exposed and then
            // transform into coord space with origin of blockframe' top left corner:

            val intersection = rowFrame.intersection(blockFrame)

            if(intersection == null) {
                // there is no intersection. This means the block is *entirely* outside of the bounds.  An unlikely but not impossible situation.  Clip it entirely.
                RectF(blockFrame.left, blockFrame.top, blockFrame.left, blockFrame.top)
            } else {
                intersection.offset(0 - blockFrame.left, 0 - blockFrame.top)
            }
        } else {
            // no clip is necessary because the blockFrame is contained entirely within the
            // surrounding block.
            null
        }

        return mapBlocksToRectDisplayList(
            tail,
            rowFrame,
            accumulatedStackHeight + block.stackedHeight(blockBounds),
            results + listOf(
                DisplayItem(blockFrame, clip, block)
            )
        )
    }
}
