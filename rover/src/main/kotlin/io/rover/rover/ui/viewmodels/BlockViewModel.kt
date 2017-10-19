package io.rover.rover.ui.viewmodels

import android.graphics.Rect
import android.graphics.RectF
import io.rover.rover.core.domain.Block
import io.rover.rover.core.domain.HorizontalAlignment
import io.rover.rover.core.domain.Position
import io.rover.rover.core.domain.VerticalAlignment
import io.rover.rover.ui.measuredAgainst
import io.rover.rover.ui.types.Alignment
import io.rover.rover.ui.types.Insets

abstract class BlockViewModel(
    private val block: Block
): BlockViewModelInterface {
    override fun stackedHeight(bounds: RectF): Float = when(block.position) {
        Position.Floating -> 0.0f
        Position.Stacked -> {
            // TODO: what about if the block top and bottom values are proportional (percentage?)
            (block.offsets.top.value + block.offsets.bottom.value).toFloat()
        }
    }

    override val insets: Insets
        get() = Insets(
            block.insets.top,
            block.insets.left,
            block.insets.bottom,
            block.insets.right
        )

    override val isStacked: Boolean
        get() = block.position == Position.Stacked

    override val opacity: Float
        get() = block.opacity.toFloat()

    override val verticalAlignment: Alignment
        // maps the domain model type [VerticalAlignment] to a UI subsystem type [Alignment].  We
        // don't want model types to be exposed by the view models.
        get() = when(block.verticalAlignment) {
            VerticalAlignment.Bottom -> Alignment.Bottom
            VerticalAlignment.Fill -> Alignment.Fill
            VerticalAlignment.Middle -> Alignment.Center
            VerticalAlignment.Top -> Alignment.Top
        }

    override fun frame(bounds: RectF): RectF {
        val x = x(bounds)
        val y = y(bounds)
        val width = width(bounds)
        val height = height(bounds)

        // we floor (probably?) the float values into int values, because Android does not seem to
        // support rational sub-logical-pixel (where "logical pixel" is dp on Android and
        // points on iOS) rendering.  (interestingly, iOS' CGPoint and CGRect
        // do, suggesting that one can address a proportion smaller than a logical pixel on that
        // platform)
        return RectF(
            x,
            y,
            (width + x),
            (y + height)
        )
    }

    /**
     * Compute (or measure) the "natural" height for the content contained in this block (for
     * example, a wrapped block of text will consume up to some height depending on content and
     * other factors).  Used for our auto-height feature.
     *
     * In the base class implementation [BlockViewModel.intrinsicHeight] returns 0; the base
     * block type has no content.
     */
    open fun intrinsicHeight(bounds: RectF): Float = 0.0f

    /**
     * Computes the Block's width.
     */
    fun height(bounds: RectF): Float = when(block.verticalAlignment) {
        VerticalAlignment.Fill -> {
            val top = block.offsets.top.measuredAgainst(bounds.height().toFloat())
            val bottom = block.offsets.bottom.measuredAgainst(bounds.height().toFloat())
            bounds.height() - top - bottom
        }
        else -> {
            if(block.autoHeight) {
                intrinsicHeight(bounds)
            } else {
                block.height.measuredAgainst(bounds.height().toFloat())
            }
        }
    }

    /**
     * Computes the Block's width.
     */
    override fun width(bounds: RectF): Float {
        val width = when(block.horizontalAlignment) {
            HorizontalAlignment.Fill -> {
                val left = block.offsets.left.measuredAgainst(bounds.width().toFloat())
                val right = block.offsets.right.measuredAgainst(bounds.width().toFloat())
                bounds.width() - left - right
            }

            else -> block.width.measuredAgainst(bounds.width().toFloat())
        }

        return listOf(width, 0.0f).max()!!
    }

    /**
     * Computes the Block's absolute horizontal coordinate in the containing [RowViewModel]s relative space.
     */
    private fun x(bounds: RectF): Float {
        val width = width(bounds)

        return when(block.horizontalAlignment) {
            HorizontalAlignment.Center -> bounds.left + ((bounds.width() - width) / 2) + block.offsets.center.measuredAgainst(bounds.width().toFloat())
            HorizontalAlignment.Fill, HorizontalAlignment.Left -> bounds.left + block.offsets.left.measuredAgainst(bounds.width().toFloat())
            HorizontalAlignment.Right -> bounds.right - width - block.offsets.right.measuredAgainst(bounds.width().toFloat())
        }
    }

    /**
     * Computes the Block's absolute vertical coordinate in the containing [RowViewModel]s relative space.
     */
    private fun y(bounds: RectF): Float {
        val height = height(bounds)

        return when(block.verticalAlignment) {
            VerticalAlignment.Bottom -> bounds.bottom - height - block.offsets.bottom.measuredAgainst(bounds.height().toFloat())
            VerticalAlignment.Fill, VerticalAlignment.Top -> bounds.top + block.offsets.top.measuredAgainst(bounds.height().toFloat())
            VerticalAlignment.Middle -> bounds.top + ((bounds.height() - height) / 2) + block.offsets.middle.measuredAgainst(bounds.height().toFloat())
        }
    }
}
