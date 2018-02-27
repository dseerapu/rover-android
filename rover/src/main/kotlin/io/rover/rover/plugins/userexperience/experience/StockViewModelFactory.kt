package io.rover.rover.plugins.userexperience.experience

import android.content.Context
import android.os.Parcelable
import io.rover.rover.platform.DateFormatting
import io.rover.rover.platform.LocalStorage
import io.rover.rover.platform.SharedPreferencesLocalStorage
import io.rover.rover.plugins.data.DataPluginInterface
import io.rover.rover.plugins.data.domain.Experience
import io.rover.rover.plugins.events.EventsPluginInterface
import io.rover.rover.plugins.userexperience.UserExperiencePlugin
import io.rover.rover.plugins.userexperience.experience.blocks.BlockViewModelFactoryInterface
import io.rover.rover.plugins.userexperience.experience.navigation.ExperienceNavigationViewModel
import io.rover.rover.plugins.userexperience.experience.navigation.ExperienceNavigationViewModelInterface
import io.rover.rover.plugins.userexperience.experience.toolbar.ExperienceToolbarViewModel
import io.rover.rover.plugins.userexperience.experience.toolbar.ExperienceToolbarViewModelInterface
import io.rover.rover.plugins.userexperience.experience.toolbar.ToolbarConfiguration
import io.rover.rover.plugins.userexperience.notificationcentre.NotificationCenterListViewModel
import io.rover.rover.plugins.userexperience.notificationcentre.NotificationCenterListViewModelInterface
import io.rover.rover.plugins.userexperience.notificationcentre.NotificationsRepository

/**
 * Constructs the standard versions of the view models for all the given Experience blocks.
 *
 * If you wish to override the View Model creation behaviour, please see [UserExperiencePlugin] and
 * override the [ViewModelFactoryInterface] methods there.
 */
open class StockViewModelFactory(
    protected val blockViewModelFactory: BlockViewModelFactoryInterface,
    protected val dataPlugin: DataPluginInterface,
    protected val eventsPlugin: EventsPluginInterface
) : ViewModelFactoryInterface {

    /**
     * We'll cache the experience view models.  We don't need to worry about caching the others,
     * though: ExperienceViewModel itself is the start of the whole tree, and it will hold the rest.
     */
    private val cachedExperienceViewModels: MutableMap<String, ExperienceViewModelInterface> = hashMapOf()

    override fun viewModelForExperienceNavigation(experience: Experience, icicle: Parcelable?): ExperienceNavigationViewModelInterface {
        return ExperienceNavigationViewModel(
            experience,
            blockViewModelFactory,
            this,
            eventsPlugin,
            icicle
        )
    }

    override fun viewModelForExperience(experienceId: String, icicle: Parcelable?): ExperienceViewModelInterface {
        // Experience view models are singletons: ie., only one for each given experience. return a
        // cached live instance if we already have one, or recreate it from state if needed.
        return cachedExperienceViewModels.getOrPut(experienceId) {
            ExperienceViewModel(
                experienceId,
                dataPlugin,
                this,
                icicle
            )
        }
    }

    override fun viewModelForExperienceToolbar(toolbarConfiguration: ToolbarConfiguration): ExperienceToolbarViewModelInterface {
        return ExperienceToolbarViewModel(
            toolbarConfiguration
        )
    }

    override fun viewModelForNotificationCenter(context: Context): NotificationCenterListViewModelInterface {
        return NotificationCenterListViewModel(
            // TODO: redo this for DI reboot
            NotificationsRepository(dataPlugin, DateFormatting(), SharedPreferencesLocalStorage(context))
        )
    }
}
