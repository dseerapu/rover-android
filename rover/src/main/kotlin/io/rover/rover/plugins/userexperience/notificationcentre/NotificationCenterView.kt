package io.rover.rover.plugins.userexperience.notificationcentre

import android.content.Context
import android.os.Build
import android.support.annotation.RequiresApi
import android.util.AttributeSet
import android.view.View
import io.rover.rover.plugins.userexperience.experience.concerns.BindableView

/**
 * Embed this view to embed a list of previously received push notifications for the user to browse
 * through.
 *
 * In order to display the list, use the implementation of
 * [ViewModelFactoryInterface.viewModelForExperience] to create an instance of the needed Experience
 * view model, and then bind it with [setViewModel].
 *
 * Note about Android state restoration: Rover SDK views handle state saving & restoration through
 * their view models, so you will need store a Parcelable on behalf of ExperienceView and
 * [NotificationCentreViewModel] (grabbing the state parcelable from the view model at save time and
 * restoring it by passing it to the view model factory at restart time).
 *
 * See [StandaloneExperienceHostActivity] for an example of how to integrate.
 */
class NotificationCenterView : View, BindableView<NotificationCenterViewModelInterface> {
    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)

    // TODO: view model setter
    override var viewModel: NotificationCenterViewModelInterface? = null
        set(notificationViewModel) {
            field = notificationViewModel
            notificationViewModel
        }
}
