package io.rover.rover.ui.viewmodels

import android.annotation.SuppressLint
import android.os.Parcelable
import io.rover.rover.core.domain.Experience
import kotlinx.android.parcel.Parcelize

/**
 * Behaviour for
 */
class ExperienceViewModel(
    val experience: Experience
): BindableViewModel {
    // concerns:
    // start at home Screen.
    // maintain a backstack of ScreenView/ScreenViewModels.
    // persist state of which screen it's on.
    // have a canGoBack query method and a goBack() action creator, akin to WebView.

    // be aware that user extensions must be possible, but achieve it by writing a concise,
    // non-tightly coupled implementation and then think about extension points afterwards.

    // coming up with Events is a bit tricky: the View itself is going to want some degree
    // of state so it can do caching of views.  or maybe it can just cache by screen id, and
    // be gloriously ignorant of the actual stack? that's the ideal

    fun registerForEvents(callback: EventCallback) {
        eventRegistrations.add(callback)
    }

    // in this case, we'll injections actions by subscribing to events coming from the viewmodels

    private var state = State(
        listOf()
    )

    private val eventRegistrations: MutableSet<EventCallback> = mutableSetOf()

    // sigh. do I want to evaluate bringing in MicroRx after all?  In which case I'll want to be
    // sure it complies reasonably with Reactive Streams spec.

    // Do I truly need it?  Let's try writing this with just shitty callbacks and see what happens.

    // @Parcelize Kotlin synthetics are generating the CREATOR method for us.
    @SuppressLint("ParcelCreator")
    @Parcelize
    data class BackStackFrame(
        val screenId: String
    ): Parcelable

    // @Parcelize Kotlin synthetics are generating the CREATOR method for us.
    @SuppressLint("ParcelCreator")
    @Parcelize
    data class State(
        val backStack: List<BackStackFrame>
    ): Parcelable
}

sealed class Event {
    class SwitchToScreen(val screenViewModel: ScreenViewModelInterface): Event()
    // TODO: there may be an "Open external URI" event here.
}

typealias EventCallback = (event: Event) -> Unit

