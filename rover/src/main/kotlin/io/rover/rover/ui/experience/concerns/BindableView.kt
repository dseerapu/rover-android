package io.rover.rover.ui.experience.concerns

import android.view.View

/**
 * Wraps a Rover Android [View] that can be bound to a [BindableViewModel].
 *
 * This is usually implemented by the views themselves, and [view] just returns `this`.  This is an
 * interface rather than an abstract [View] subclass in order to allow implementers to inherit from
 * various different [View] subclasses.
 */
interface BindableView<VM : BindableViewModel> {
    var viewModel: VM?

    val view: View
        get() = this as View
}
