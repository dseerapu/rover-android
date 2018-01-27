package io.rover.rover.plugins.userexperience.experience

import android.os.Parcelable
import io.rover.rover.plugins.data.domain.BarcodeBlock
import io.rover.rover.plugins.data.domain.Block
import io.rover.rover.plugins.data.domain.ButtonBlock
import io.rover.rover.plugins.data.domain.ButtonState
import io.rover.rover.plugins.data.domain.Experience
import io.rover.rover.plugins.data.domain.ImageBlock
import io.rover.rover.plugins.data.domain.RectangleBlock
import io.rover.rover.plugins.data.domain.Row
import io.rover.rover.plugins.data.domain.Screen
import io.rover.rover.plugins.data.domain.TextBlock
import io.rover.rover.plugins.data.domain.WebViewBlock
import io.rover.rover.plugins.userexperience.assets.AssetService
import io.rover.rover.plugins.userexperience.assets.ImageOptimizationServiceInterface
import io.rover.rover.plugins.data.DataPluginInterface
import io.rover.rover.plugins.userexperience.MeasurementService
import io.rover.rover.plugins.userexperience.UserExperiencePlugin
import io.rover.rover.plugins.userexperience.experience.blocks.BlockViewModelFactory
import io.rover.rover.plugins.userexperience.experience.blocks.BlockViewModelFactoryInterface
import io.rover.rover.plugins.userexperience.experience.toolbar.ToolbarConfiguration
import io.rover.rover.plugins.userexperience.experience.blocks.concerns.background.BackgroundViewModel
import io.rover.rover.plugins.userexperience.experience.blocks.barcode.BarcodeBlockViewModel
import io.rover.rover.plugins.userexperience.experience.blocks.barcode.BarcodeViewModel
import io.rover.rover.plugins.userexperience.experience.blocks.concerns.layout.BlockViewModel
import io.rover.rover.plugins.userexperience.experience.blocks.concerns.layout.BlockViewModelInterface
import io.rover.rover.plugins.userexperience.experience.blocks.concerns.border.BorderViewModel
import io.rover.rover.plugins.userexperience.experience.blocks.button.ButtonBlockViewModel
import io.rover.rover.plugins.userexperience.experience.blocks.button.ButtonStateViewModel
import io.rover.rover.plugins.userexperience.experience.blocks.button.ButtonStateViewModelInterface
import io.rover.rover.plugins.userexperience.experience.blocks.button.ButtonViewModel
import io.rover.rover.plugins.userexperience.experience.navigation.ExperienceNavigationViewModel
import io.rover.rover.plugins.userexperience.experience.navigation.ExperienceNavigationViewModelInterface
import io.rover.rover.plugins.userexperience.experience.toolbar.ExperienceToolbarViewModel
import io.rover.rover.plugins.userexperience.experience.toolbar.ExperienceToolbarViewModelInterface
import io.rover.rover.plugins.userexperience.experience.blocks.image.ImageBlockViewModel
import io.rover.rover.plugins.userexperience.experience.blocks.image.ImageViewModel
import io.rover.rover.plugins.userexperience.experience.blocks.rectangle.RectangleBlockViewModel
import io.rover.rover.plugins.userexperience.experience.layout.row.RowViewModel
import io.rover.rover.plugins.userexperience.experience.layout.row.RowViewModelInterface
import io.rover.rover.plugins.userexperience.experience.layout.screen.ScreenViewModel
import io.rover.rover.plugins.userexperience.experience.layout.screen.ScreenViewModelInterface
import io.rover.rover.plugins.userexperience.experience.blocks.text.TextBlockViewModel
import io.rover.rover.plugins.userexperience.experience.blocks.concerns.text.TextViewModel
import io.rover.rover.plugins.userexperience.experience.blocks.web.WebViewBlockViewModel
import io.rover.rover.plugins.userexperience.experience.blocks.web.WebViewModel

/**
 * Constructs the standard versions of the view models for all the given Experience blocks.
 *
 * If you wish to override the View Model creation behaviour, please see [UserExperiencePlugin] and
 * override the [ViewModelFactoryInterface] methods there.
 */
class StockViewModelFactory(
    private val blockViewModelFactory: BlockViewModelFactoryInterface,
    private val dataPlugin: DataPluginInterface
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

                // FACK! the overridden versions of these methods from DataPlugin won't make it
                // here, and passing it down breaks the DAG.  It needs it for building the
                // navigation view model, and indeed from there the rest of the view model graph.


                this,


                icicle
            )
        }
    }


}
