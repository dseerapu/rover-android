package io.rover.rover.ui.viewmodels

import android.app.Activity
import android.graphics.Bitmap
import android.os.Parcelable
import android.util.DisplayMetrics
import android.view.WindowManager
import io.rover.rover.core.domain.Experience
import io.rover.rover.core.domain.Screen
import io.rover.rover.services.network.NetworkTask
import io.rover.rover.streams.Observable
import io.rover.rover.ui.BlockAndRowLayoutManager
import io.rover.rover.ui.experience.blocks.concerns.background.BackgroundViewModelInterface
import io.rover.rover.ui.experience.blocks.concerns.border.BorderViewModelInterface
import io.rover.rover.ui.experience.blocks.concerns.layout.LayoutPaddingDeflection
import io.rover.rover.ui.experience.blocks.concerns.layout.LayoutableViewModel
import io.rover.rover.ui.types.Alignment
import io.rover.rover.ui.experience.toolbar.ToolbarConfiguration
import io.rover.rover.ui.types.DisplayItem
import io.rover.rover.ui.types.Font
import io.rover.rover.ui.types.FontAppearance
import io.rover.rover.ui.types.Insets
import io.rover.rover.ui.types.Layout
import io.rover.rover.ui.types.NavigateTo
import io.rover.rover.ui.types.PixelSize
import io.rover.rover.ui.types.RectF
import java.net.URI
import java.net.URL

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
        data class DisplayState(
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

    // TODO: the following may be moved into a mixin view model for Interactions, even though our
    // domain model atm has block actions being included for all blocks.

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
        data class Clicked(
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
     *
     * In particular it aggregates all the navigation events from the contained rows.
     */
    val events: Observable<NavigateTo>

    val needsBrightBacklight: Boolean

    val appBarConfiguration: ToolbarConfiguration
}

/**
 * Responsible for fetching and displaying an Experience, with the appropriate Android toolbar along
 * the top.
 */
interface ExperienceViewModelInterface: BindableViewModel {
    val events: Observable<Event>

    fun pressBack()

    sealed class Event {
        data class ExperienceReady(
            val experienceNavigationViewModel: ExperienceNavigationViewModelInterface
        ): Event()

        data class SetActionBar(
            val toolbarViewModel: ExperienceToolbarViewModelInterface
        ): Event()

        data class DisplayError(
            val message: String
        ): Event()

        /**
         * This event signifies that the LayoutParams of the containing window should either be set
         * to either 1 or [WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE].
         */
        data class SetBacklightBoost(val extraBright: Boolean): Event()

        /**
         * The user should be navigated somewhere external to
         */
        data class ExternalNavigation(
            val event: ExperienceExternalNavigationEvent
        ): Event()
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
        data class GoToScreen(
            val screenViewModel: ScreenViewModelInterface,
            val backwards: Boolean,
            val animate: Boolean
        ): Event()

//        data class GoForwardToScreen(
//            val screenViewModel: ScreenViewModelInterface
//        ): Event()
//
//        data class GoBackwardToScreen(
//            val screenViewModel: ScreenViewModelInterface
//        ): Event()
//
//        /**
//         * This event signifies that the view should immediately display the given view model.
//         */
//        data class WarpToScreen(
//            val screenViewModel: ScreenViewModelInterface
//        ): Event()

        data class ViewEvent(
            val event: ExperienceExternalNavigationEvent
        ): Event()

        data class SetActionBar(
            val experienceToolbarViewModel: ExperienceToolbarViewModelInterface
        ): Event()

        /**
         * This event signifies that the LayoutParams of the containing window should either be set
         * to either 1 or [WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE].
         */
        data class SetBacklightBoost(val extraBright: Boolean): Event()
    }
}

/**
 * These are navigation that are emitted by the experience navigation view model but because they
 * are for destinations external to the experience they must be passed up by the containing
 * ExperienceViewModel.
 */
sealed class ExperienceExternalNavigationEvent {
    // TODO: we may want to do an (optional) internal web browser like iOS, but there is less call for it
    // because Android has its back button.  Will discuss.

    // TODO: add an Event here for customers to insert a custom navigation event that their own code
    // can handle on the outer side of ExperienceViewModel for navigating to other screens in their
    // app and such.

    /**
     *  Containing view context should launch a web browser for the given URI in the surrounding
     *  navigation flow (such as the general Android backstack, Conductor backstack, etc.) external
     *  to the internal Rover ExperienceNavigationViewModel, whatever it happens to be in the
     *  surrounding app.
     */
    data class OpenExternalWebBrowser(val uri: URI): ExperienceExternalNavigationEvent()

    /**
     * Containing view context (hosting the Experience) should pop itself ([Activity.finish], etc.)
     * in the surrounding navigation flow (such as the general Android backstack, Conductor
     * backstack, etc.) external to the internal Rover ExperienceNavigationViewModel, whatever it
     * happens to be in the surrounding app.
     */
    class Exit(): ExperienceExternalNavigationEvent()
}

interface ExperienceAppBarViewModelInterface {
    val events: Observable<Event>

    class Event(
        val appBarConfiguration: ToolbarConfiguration
    )
}

interface ExperienceToolbarViewModelInterface {
    val toolbarEvents: Observable<Event>

    val configuration: ToolbarConfiguration

    fun pressedBack()

    fun pressedClose()

    sealed class Event {
        class PressedBack(): Event()
        class PressedClose(): Event()
    }
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
interface TextBlockViewModelInterface :
    LayoutableViewModel,
    BlockViewModelInterface,
    BackgroundViewModelInterface,
    BorderViewModelInterface,
    TextViewModelInterface

interface ImageBlockViewModelInterface :
    LayoutableViewModel,
    BlockViewModelInterface,
    BackgroundViewModelInterface,
    BorderViewModelInterface,
    ImageViewModelInterface

interface WebViewBlockViewModelInterface :
    LayoutableViewModel,
    BlockViewModelInterface,
    BackgroundViewModelInterface,
    BorderViewModelInterface,
    WebViewModelInterface

interface BarcodeBlockViewModelInterface :
    LayoutableViewModel,
    BlockViewModelInterface,
    BackgroundViewModelInterface,
    BarcodeViewModelInterface,
    BorderViewModelInterface

interface ButtonBlockViewModelInterface :
    LayoutableViewModel,
    BlockViewModelInterface,
    ButtonViewModelInterface

interface ButtonStateViewModelInterface:
    BindableViewModel,
    TextViewModelInterface,
    BackgroundViewModelInterface,
    BorderViewModelInterface

enum class StateOfButton {
    Normal,
    Disabled,
    Highlighted,
    Selected
}