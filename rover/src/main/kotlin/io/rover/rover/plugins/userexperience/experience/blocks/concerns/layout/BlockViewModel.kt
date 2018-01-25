package io.rover.rover.plugins.userexperience.experience.blocks.concerns.layout

import io.rover.rover.plugins.data.domain.Block
import io.rover.rover.plugins.data.domain.BlockAction
import io.rover.rover.plugins.data.domain.HorizontalAlignment
import io.rover.rover.plugins.data.domain.Position
import io.rover.rover.plugins.data.domain.VerticalAlignment
import io.rover.rover.core.logging.log
import io.rover.rover.platform.whenNotNull
import io.rover.rover.core.streams.PublishSubject
import io.rover.rover.core.streams.share
import io.rover.rover.plugins.userexperience.experience.layout.measuredAgainst
import io.rover.rover.plugins.userexperience.experience.navigation.NavigateTo
import io.rover.rover.plugins.userexperience.types.RectF
import io.rover.rover.plugins.userexperience.experience.layout.ViewType
import io.rover.rover.plugins.userexperience.experience.layout.screen.ScreenViewModel

/**
 * A mixin used by all blocks that contains the block layout and positioning concerns.
 *
 * - LayoutableViewModel probably needs to split, because we want to be able to delegate the frame()
 *   method to the new mixin version of BlockViewModel but obviously it should not specify view type
 */
class BlockViewModel(
    private val block: Block,
    private val paddingDeflections: Set<LayoutPaddingDeflection> = emptySet(),
    private val measurable: Measurable? = null
) : BlockViewModelInterface {

    override val viewType: ViewType
        // TODO: remove this entirely
        get() = TODO("You must implement this in your block ViewModel type.")

    override fun stackedHeight(bounds: RectF): Float = when (block.position) {
        Position.Floating -> 0.0f
        Position.Stacked -> {
            // TODO: what about if the block top and bottom values are proportional (percentage?)
            this.height(bounds) + block.offsets.top.value.toFloat() + block.offsets.bottom.value.toFloat()
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
        get() = when (block.verticalAlignment) {
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

        return RectF(
            x,
            y,
            (width + x),
            (y + height)
        )
    }

    /**
     * Computes the Block's width.
     */
    fun height(bounds: RectF): Float = when (block.verticalAlignment) {
        VerticalAlignment.Fill -> {
            val top = block.offsets.top.measuredAgainst(bounds.height())
            val bottom = block.offsets.bottom.measuredAgainst(bounds.height())
            bounds.height() - top - bottom
        }
        else -> {
            if (block.autoHeight) {
                val boundsConsideringInsets = RectF(
                    bounds.left + insets.left + paddingDeflections.map { it.paddingDeflection.left }.sum(),
                    bounds.top,
                    bounds.left + width(bounds) - insets.right - paddingDeflections.map { it.paddingDeflection.right }.sum(),
                    bounds.bottom
                )

                // TODO: boundsConsideringInsets could go negative if the insets are bigger than the
                // bounds, causing illegal/undefined behaviour further down the chain.

                if (measurable == null) {
                    log.w("Block is set to auto-height but no measurable is given.")
                    0f
                } else {
                    measurable.intrinsicHeight(boundsConsideringInsets) +
                        insets.bottom +
                        insets.top +
                        paddingDeflections.map {
                            it.paddingDeflection.top + it.paddingDeflection.bottom
                        }.sum()
                }
            } else {
                block.height.measuredAgainst(bounds.height())
            }
        }
    }

    /**
     * Computes the Block's width.
     */
    override fun width(bounds: RectF): Float {
        val width = when (block.horizontalAlignment) {
            HorizontalAlignment.Fill -> {
                val left = block.offsets.left.measuredAgainst(bounds.width())
                val right = block.offsets.right.measuredAgainst(bounds.width())
                bounds.width() - left - right
            }

            else -> block.width.measuredAgainst(bounds.width())
        }

        return listOf(width, 0.0f).max()!!
    }

    private val eventSource = PublishSubject<BlockViewModelInterface.Event>()
    override val events = eventSource.share()

    override val isClickable: Boolean
        get() = block.action != null

    override fun click() {
        // I don't have an epic (any other asynchronous behaviour to compose) here, just a single
        // event emitter, so I'll just publish an event directly.
        val action = block.action

        val navigateTo = when (action) {
            null -> null
            is BlockAction.GoToScreenAction -> { NavigateTo.GoToScreenAction(action.screenId.rawValue) }
            is BlockAction.OpenUrlAction -> { NavigateTo.OpenUrlAction(action.url) }
        }

        navigateTo.whenNotNull {
            eventSource.onNext(
                BlockViewModelInterface.Event.Clicked(it)
            )
        }
    }

    override fun touched() {
        eventSource.onNext(
            BlockViewModelInterface.Event.Touched()
        )
    }

    override fun released() {
        eventSource.onNext(
            BlockViewModelInterface.Event.Released()
        )
    }

    /**
     * Computes the Block's absolute horizontal coordinate in the [ScreenViewModel]'s coordinate
     * space.
     */
    private fun x(bounds: RectF): Float {
        val width = width(bounds)

        return when (block.horizontalAlignment) {
            HorizontalAlignment.Center -> bounds.left + ((bounds.width() - width) / 2) + block.offsets.center.measuredAgainst(bounds.width())
            HorizontalAlignment.Fill, HorizontalAlignment.Left -> bounds.left + block.offsets.left.measuredAgainst(bounds.width())
            HorizontalAlignment.Right -> bounds.right - width - block.offsets.right.measuredAgainst(bounds.width())
        }
    }

    /**
     * Computes the Block's absolute vertical coordinate in the [ScreenViewModel]'s coordinate
     * space.
     */
    private fun y(bounds: RectF): Float {
        val height = height(bounds)

        val alignment = if (isStacked) {
            VerticalAlignment.Top
        } else {
            block.verticalAlignment
        }

        return when (alignment) {
            VerticalAlignment.Bottom -> bounds.bottom - height - block.offsets.bottom.measuredAgainst(bounds.height())
            VerticalAlignment.Fill, VerticalAlignment.Top -> bounds.top + block.offsets.top.measuredAgainst(bounds.height())
            VerticalAlignment.Middle -> bounds.top + ((bounds.height() - height) / 2) + block.offsets.middle.measuredAgainst(bounds.height())
        }
    }
}
