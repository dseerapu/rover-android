package io.rover.rover.ui.viewmodels

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Shader
import android.os.Parcelable
import android.util.DisplayMetrics
import android.view.WindowManager
import io.rover.rover.core.domain.Background
import io.rover.rover.core.domain.Border
import io.rover.rover.core.domain.Experience
import io.rover.rover.core.domain.Screen
import io.rover.rover.services.network.NetworkTask
import io.rover.rover.streams.Observable
import io.rover.rover.ui.BlockAndRowLayoutManager
import io.rover.rover.ui.types.Alignment
import io.rover.rover.ui.types.DisplayItem
import io.rover.rover.ui.types.Font
import io.rover.rover.ui.types.FontAppearance
import io.rover.rover.ui.types.Insets
import io.rover.rover.ui.types.Layout
import io.rover.rover.ui.types.NavigateTo
import io.rover.rover.ui.types.PixelSize
import io.rover.rover.ui.types.Rect
import io.rover.rover.ui.types.RectF
import io.rover.rover.ui.views.PaddingContributor
import io.rover.rover.ui.views.ViewBlock
import java.net.URI
import java.net.URL

/**
 * Exposed by a view model that may need to contribute to the padding around the content.  For
 * instance, the [BorderViewModel] exposes this so that content-bearing view models can ensure their
 * content is not occluded by the border.
 *
 * Note that the View mixins will likely need to implement the [PaddingContributor] interface and
 * ensure that they are passed to the [ViewBlock].  Please see the documentation there for more
 * details and the rationale.
 */
interface LayoutPaddingDeflection {
    // TODO: consider changing to not use Rect to better indicate that it is not a rectangle but an
    // inset for each edge
    val paddingDeflection: Rect
}

/**
 * Specifies how the view should properly display the given background image.
 *
 * The method these are specified with is a bit idiosyncratic on account of Android implementation
 * details and the combination of Drawables the view uses to achieve the effect.
 */
class BackgroundImageConfiguration(
    /**
     * Bounds in pixels, in *relative insets from their respective edges*.
     *
     * TODO: consider changing to not use Rect to better indicate that it is not a rectangle but an inset for each edge
     *
     * Our drawable is always set to FILL_XY, which means by specifying these insets you get
     * complete control over the aspect ratio, sizing, and positioning.  Note that this parameter
     * cannot be used to specify any sort of scaling for tiling, since the bottom/right bounds are
     * effectively undefined as the pattern repeats forever.  In that case, consider using using
     * [imageNativeDensity] to achieve a scale effect (although note that it is in terms of the
     * display DPI).
     *
     * (Note: we use this approach rather than just having a configurable gravity on the drawable
     * because that would not allow for aspect correct fit scaling.)
     */
    val insets: Rect,

    /**
     * An Android tiling mode.  For no tiling, set as null.
     */
    val tileMode: Shader.TileMode?,

    /**
     * This density value should be set on the bitmap with [Bitmap.setDensity] before drawing it
     * on an Android canvas.
     */
    val imageNativeDensity: Int
)

/**
 * This interface is exposed by View Models that have support for a background.  Equivalent to
 * the [Background] domain model interface.
 */
interface BackgroundViewModelInterface {
    val backgroundColor: Int

    fun requestBackgroundImage(
        targetViewPixelSize: PixelSize,
        displayMetrics: DisplayMetrics,
        callback: (
            /**
             * The bitmap to be drawn.  It is recommended that the consumer arrange to have it
             * scaled to a roughly appropriate amount (need not be exact; that is the purpose of the
             * view size and the [insets] given above) and also to be uploaded to GPU texture memory
             * off thread ([Bitmap.prepareToDraw]) before setting it.
             *
             * Note: one can set the source density of the bitmap to control its scaling (which is
             * particularly relevant for tile modes where
             */
            Bitmap,

            BackgroundImageConfiguration
        ) -> Unit
    ): NetworkTask?
}

/**
 * This interface is exposed by View Models that have support for a border (of arbitrary width and
 * possibly rounded with a radius).  Equivalent to the [Border] domain model interface.
 */
interface BorderViewModelInterface : LayoutPaddingDeflection {
    val borderColor: Int

    // TODO: this should start returning Px instead of Dp
    val borderRadius: Int

    // TODO: this should start returning Px instead of Dp
    val borderWidth: Int

    companion object
}

/**
 * View Model for block content that contains rich text content (decorated with strong, italic, and
 * underline HTML tags).
 */
interface TextViewModelInterface : Measurable {
    val text: String

    val singleLine: Boolean

    /**
     * Should the view configure the Android text view with a vertically centering gravity?
     */
    val centerVertically: Boolean

    val fontAppearance: FontAppearance

    fun boldRelativeToBlockWeight(): Font
}

/**
 * View Model for block content that contains a clickable button with several different
 * states.
 */
interface ButtonViewModelInterface {
//    val text: String

    val buttonEvents: Observable<Event>

    sealed class Event {
        /**
         * Reveal the text for the given.
         */
        class DisplayState(
            val viewModel: ButtonStateViewModelInterface,
            val animate: Boolean,

            /**
             * The owning view will maintain a set of background views itself for allowing for
             * partially occlusive transitions between button states.  This means it has need to
             * know which of the backgrounds it should display on a given [Event.DisplayState]
             * event.
             */
            val stateOfButton: StateOfButton,

            /**
             * The given animation should undo itself afterwards.
             */
            val selfRevert: Boolean
        ): Event()
    }


    /**
     * The owning view will maintain a set of background views itself for allowing for partially
     * occlusive transitions between button states.  This means it has need to interrogate to
     * bind all of the needed view models to each of the background layers.
     */
    fun viewModelForState(state: StateOfButton): ButtonStateViewModelInterface
}

interface ImageViewModelInterface : Measurable {
    // TODO: I may elect to demote the Bitmap concern from the ViewModel into just the View (or a
    // helper of some kind) in order to avoid a thick Android object (Bitmap) being touched here

    /**
     * Get the needed image for display, hitting caches if possible and the network if necessary.
     * You'll need to give a [PixelSize] of the target view the image will be landing in.  This will
     * allow for optimizations to select, download, and cache the appropriate size of content.
     *
     * Remember to call [NetworkTask.resume] to start the retrieval, or your callback will never
     * be hit.
     */
    fun requestImage(
        targetViewPixelSize: PixelSize,
        displayMetrics: DisplayMetrics,
        callback: (Bitmap) -> Unit
    ): NetworkTask?
}

interface WebViewModelInterface {
    val url: URL
    val scrollingEnabled: Boolean
}

interface BarcodeViewModelInterface: Measurable, LayoutPaddingDeflection {
    val barcodeType: BarcodeType

    val barcodeValue: String

    enum class BarcodeType {
        PDF417, Code128, Aztec, QrCode
    }
}

/**
 * Can vertically measure its content for stacked/autoheight purposes.
 */
interface Measurable {
    /**
     * Measure the "natural" height for the content contained in this block (for
     * example, a wrapped block of text will consume up to some height depending on content and
     * other factors), given the width of the bounds.  Used for our auto-height feature.
     */
    fun intrinsicHeight(bounds: RectF): Float
}

/**
 * A view model for Blocks (particularly, the dynamic layout thereof).
 */
interface BlockViewModelInterface : LayoutableViewModel {

    /**
     * The full amount contributed by this block (including its own height and offsets) to the
     * height of all the stacked blocks within the row.  So, the subsequent stacked block must be
     * laid out at a y position that is the sum of all the [stackedHeight]s of all the prior stacked
     * blocks.
     */
    fun stackedHeight(bounds: RectF): Float

    val insets: Insets

    val isStacked: Boolean

    /**
     * Alpha applied to the entire view.
     *
     * Between 0 (transparent) and 1 (fully opaque).
     */
    val opacity: Float

    val verticalAlignment: Alignment

    fun width(bounds: RectF): Float

    // TODO: the following may be moved into a mixin view model, even though our domain model atm
    // has block actions being included for all blocks.

    /**
     * The view is clickable.
     */
    val isClickable: Boolean

    /**
     * User has clicked the view.
     */
    fun click()

    /**
     * User has touched the view, but not necessarily clicked it.
     */
    fun touched()

    /**
     * User has released the view, but not necessarily clicked it.
     */
    fun released()

    sealed class Event {
        /**
         * Block has been clicked, requesting that we [navigateTo] something.
         */
        class Clicked(
            val navigateTo: NavigateTo
        ): Event()

        /**
         * Block has been touched, but not clicked.
         *
         * TODO: may not be appropriate to use MotionEvent.
         */
        class Touched(): Event()

        /**
         * Block has been released, but not necessarily clicked.
         */
        class Released(): Event()
    }

    val events: Observable<Event>
}

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
    val eventSource : Observable<NavigateTo>

    /**
     * Does this row contain anything that calls for the backlight to be set temporarily extra
     * bright?
     */
    val needsBrightBacklight: Boolean
}

/**
 * View Model for a Screen.  Used in [Experience]s.
 *
 * Rover View Models are a little atypical compared to what you may have seen elsewhere in industry:
 * unusually, layouts are data, so much layout structure and parameters are data passed through and
 * transformed by the view models.
 *
 * Implementers can take a comprehensive UI layout contained within a Rover [Screen], such as that
 * within an Experience, and lay all of the contained views out into two-dimensional space.  It does
 * so by mapping a given [Screen] to an internal graph of [RowViewModelInterface]s and
 * [BlockViewModelInterface]s, ultimately yielding the [RowViewModelInterface]s and
 * [BlockViewModelInterface]s as a sequence of [LayoutableViewModel] flat blocks in two-dimensional
 * space.
 *
 * Primarily used by [BlockAndRowLayoutManager].
 */
interface ScreenViewModelInterface: BindableViewModel, BackgroundViewModelInterface {
    /**
     * Do the computationally expensive operation of laying out the entire graph of UI view models.
     */
    fun render(widthDp: Float): Layout

    /**
     * Retrieve a list of the view models in the order they'd be laid out (guaranteed to be in
     * the same order as returned by [render]), but without the layout itself being performed.
     */
    fun gather(): List<LayoutableViewModel>

    val rowViewModels: List<RowViewModelInterface>

    /**
     * Screens may emit navigation events.
     */
    val events: Observable<NavigateTo>

    val needsBrightBacklight: Boolean
}

/**
 * Responsible for fetching and displaying an Experience, with the appropriate AppBar along the top.
 *
 * Emits an event with a new ExperienceNavigationViewModel (and
 *
 * TODO: consider renaming to experiencefetchviewmodel
 */
interface ExperienceViewModelInterface: BindableViewModel {
    val events: Observable<Event>

    fun pressBack()

    sealed class Event {
        class ExperienceReady(
            val experienceNavigationViewModel: ExperienceNavigationViewModelInterface
        ): Event()
        class DisplayError(
            val message: String
        ): Event()

        // TODO: rename to navigationEvent
        class ViewEvent(
            val event: ExperienceViewEvent
        ): Event()


        // TODO: activity indication event
    }

    /**
     * Obtain a state object for this Experience View Model.
     *
     * This view model is the start of the chain of responsibility for any nested view models.
     */
    val state: Parcelable
}

interface ExperienceNavigationViewModelInterface : BindableViewModel {
    val events : Observable<Event>

    fun pressBack()

    /**
     * Ask the view model if there are any entries on its internal back stack to revert to.
     *
     * Check this before calling [pressBack].  However, it is optional: if you call pressBack()
     * without checking [canGoBack], and there are no remaining back stack entries remaining, you'll
     * receive an [ExperienceNavigationViewModelInterface.Event.Exit] event.
     */
    fun canGoBack(): Boolean

    /**
     * Obtain a state object for this Experience Navigation View Model.
     *
     * TODO: determine if in fact it is worth exposing my own State interface type here (but not the
     * full type to avoid exposing internals).  A bit more boilerplate but it allows consuming view
     * models (that contain this one) to have stronger type guarantees in their own state bundles.
     */
    val state: Parcelable

    sealed class Event {
        class GoForwardToScreen(
            val screenViewModel: ScreenViewModelInterface,
            val appBarState: AppBarState
        ): Event()

        class GoBackwardToScreen(
            val screenViewModel: ScreenViewModelInterface,
            val appBarState: AppBarState
        ): Event()

        /**
         * This event signifies that the view should immediately display the given view model.
         */
        class WarpToScreen(
            val screenViewModel: ScreenViewModelInterface,
            val appBarState: AppBarState
        ): Event()

        class ViewEvent(
            val event: ExperienceViewEvent
        ): Event()
    }

    data class AppBarState(
        val color: Int
    )
}

/**
 * These are events that are emitted by the experience navigation view model but must be passed up
 * by the containing ExperienceViewModel.
 *
 * TODO: I split this out so ExperienceViewModel could pass these events along.  However, it seems
 * to align very closely to my future intention to split all the Event concerns between
 */
// TODO: rename to navigationevent
sealed class ExperienceViewEvent {
    /**
     * This event signifies that the LayoutParams of the containing window should either be set
     * to either 1 or [WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE].
     */
    class SetBacklightBoost(val extraBright: Boolean): ExperienceViewEvent()

    // TODO: we may want to do an (optional) internal web browser like iOS, but there is less call for it
    // because Android has its back button.  Will discuss.

    class OpenExternalWebBrowser(val uri: URI): ExperienceViewEvent()

    /**
     * Containing view should pop itself ([Activity.finish], etc.) in the surrounding navigation
     * flow, whatever it happens to be.
     */
    class Exit(): ExperienceViewEvent()
}

/**
 * View Model for a block that contains no content (other than its own border and
 * background).
 */
interface RectangleBlockViewModelInterface : LayoutableViewModel, BlockViewModelInterface, BackgroundViewModelInterface, BorderViewModelInterface

/**
 * View Model for a block that contains rich text content (decorated with strong, italic, and
 * underline HTML tags).
 */
interface TextBlockViewModelInterface : LayoutableViewModel, BlockViewModelInterface, BackgroundViewModelInterface, BorderViewModelInterface, TextViewModelInterface

interface ImageBlockViewModelInterface : LayoutableViewModel, BlockViewModelInterface, BackgroundViewModelInterface, BorderViewModelInterface, ImageViewModelInterface

interface WebViewBlockViewModelInterface : LayoutableViewModel, BlockViewModelInterface, BackgroundViewModelInterface, BorderViewModelInterface, WebViewModelInterface

interface BarcodeBlockViewModelInterface : LayoutableViewModel, BlockViewModelInterface, BackgroundViewModelInterface, BarcodeViewModelInterface, BorderViewModelInterface

interface ButtonBlockViewModelInterface: LayoutableViewModel, BlockViewModelInterface, ButtonViewModelInterface

interface ButtonStateViewModelInterface: BindableViewModel, TextViewModelInterface, BackgroundViewModelInterface, BorderViewModelInterface

enum class StateOfButton {
    Normal,
    Disabled,
    Highlighted,
    Selected
}