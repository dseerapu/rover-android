package io.rover.rover.plugins.userexperience.notificationcentre

import android.content.Context
import android.support.design.widget.CoordinatorLayout
import android.util.AttributeSet
import io.rover.rover.core.streams.androidLifecycleDispose
import io.rover.rover.core.streams.subscribe
import io.rover.rover.plugins.userexperience.experience.ViewModelBinding
import io.rover.rover.plugins.userexperience.experience.concerns.BindableView

/**
 * Embed this view to embed a list of previously received push notifications for the user to browse
 * through.  You can even embed and configure this view directly into your XML layouts.
 *
 * In order to display the list, use the implementation of
 * [ViewModelFactoryInterface.viewModelForExperience] to create an instance of the needed Experience
 * view model, and then bind it with [setViewModel].
 *
 * You may specify the row item view by either setting xml property TODO or with TODO
 *
 * Also you must override [PushPlugin.notificationCenterIntent] to produce an Intent that will get
 * your app to the state where your usage of NotificationCenterView is being displayed.
 *
 * Note about Android state restoration: Rover SDK views handle state saving & restoration through
 * their view models, so you will need store a Parcelable on behalf of ExperienceView and
 * [NotificationCentreViewModel] (grabbing the state parcelable from the view model at save time and
 * restoring it by passing it to the view model factory at restart time).
 *
 * See [StandaloneNotificationCenterActivity] for an example of how to integrate.
 */
class NotificationCenterView : CoordinatorLayout, BindableView<NotificationCenterViewModelInterface> {
    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

//    constructor(context: Context?) : super(context)
//    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
//    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
//    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
//    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)

    // TODO: confirm if we want to have a built-in snackbar for displaying error messages.  Otherwise, rebase back off of CoordinatorLayout.
    // Oh yeah. so, here's the other


    // Composition stack will be: CoordinatorLayout -> SwipeRefreshLayout AND the "empty layout" (visibility toggle)

    // -> need to be able to display a loading screen, which needs to be customizable.

    // -> should I be based on SwipeRefreshLayout?

    // ->

    override var viewModel: NotificationCenterViewModelInterface? by ViewModelBinding { viewModel, subscriptionCallback ->
        if(viewModel != null) {
            viewModel
                .events()
                .androidLifecycleDispose(this)
                .subscribe({ event ->
                    when(event) {
                        is NotificationCenterViewModelInterface.Event.ListReady -> {
                            // bind the recyclerview
                        }
                    }
                }, { error ->
                    throw error
                }, { subscription -> subscriptionCallback(subscription) })
        }
    }
}
