package io.rover.rover.ui.experience

import android.os.Parcelable
import io.rover.rover.core.domain.BarcodeBlock
import io.rover.rover.core.domain.Block
import io.rover.rover.core.domain.ButtonBlock
import io.rover.rover.core.domain.ButtonState
import io.rover.rover.core.domain.Experience
import io.rover.rover.core.domain.ImageBlock
import io.rover.rover.core.domain.RectangleBlock
import io.rover.rover.core.domain.Row
import io.rover.rover.core.domain.Screen
import io.rover.rover.core.domain.TextBlock
import io.rover.rover.core.domain.WebViewBlock
import io.rover.rover.services.assets.AssetService
import io.rover.rover.services.assets.ImageOptimizationServiceInterface
import io.rover.rover.services.network.NetworkServiceInterface
import io.rover.rover.ui.MeasurementService
import io.rover.rover.ui.experience.toolbar.ToolbarConfiguration
import io.rover.rover.ui.experience.blocks.concerns.background.BackgroundViewModel
import io.rover.rover.ui.experience.blocks.barcode.BarcodeBlockViewModel
import io.rover.rover.ui.experience.blocks.barcode.BarcodeViewModel
import io.rover.rover.ui.experience.blocks.concerns.layout.BlockViewModel
import io.rover.rover.ui.experience.blocks.concerns.layout.BlockViewModelInterface
import io.rover.rover.ui.experience.blocks.concerns.border.BorderViewModel
import io.rover.rover.ui.experience.blocks.button.ButtonBlockViewModel
import io.rover.rover.ui.experience.blocks.button.ButtonStateViewModel
import io.rover.rover.ui.experience.blocks.button.ButtonStateViewModelInterface
import io.rover.rover.ui.experience.blocks.button.ButtonViewModel
import io.rover.rover.ui.experience.navigation.ExperienceNavigationViewModel
import io.rover.rover.ui.experience.navigation.ExperienceNavigationViewModelInterface
import io.rover.rover.ui.experience.toolbar.ExperienceToolbarViewModel
import io.rover.rover.ui.experience.toolbar.ExperienceToolbarViewModelInterface
import io.rover.rover.ui.experience.blocks.image.ImageBlockViewModel
import io.rover.rover.ui.experience.blocks.image.ImageViewModel
import io.rover.rover.ui.experience.blocks.rectangle.RectangleBlockViewModel
import io.rover.rover.ui.experience.layout.row.RowViewModel
import io.rover.rover.ui.experience.layout.row.RowViewModelInterface
import io.rover.rover.ui.experience.layout.screen.ScreenViewModel
import io.rover.rover.ui.experience.layout.screen.ScreenViewModelInterface
import io.rover.rover.ui.experience.blocks.text.TextBlockViewModel
import io.rover.rover.ui.experience.blocks.concerns.text.TextViewModel
import io.rover.rover.ui.experience.blocks.web.WebViewBlockViewModel
import io.rover.rover.ui.experience.blocks.web.WebViewModel

class ViewModelFactory(
    private val measurementService: MeasurementService,
    private val assetService: AssetService,
    private val imageOptimizationService: ImageOptimizationServiceInterface,
    private val networkService: NetworkServiceInterface
) : ViewModelFactoryInterface {
    /**
     * We'll cache the experience view models.  We don't need to worry about caching the others,
     * though: ExperienceViewModel itself is the start of the whole tree, and it will hold the rest.
     */
    private val cachedExperienceViewModels: MutableMap<String, ExperienceViewModelInterface> = hashMapOf()

    override fun viewModelForBlock(block: Block): BlockViewModelInterface {
        return when (block) {
            is RectangleBlock -> {
                val borderViewModel = BorderViewModel(block)
                val backgroundViewModel = BackgroundViewModel(block, assetService, imageOptimizationService)
                RectangleBlockViewModel(BlockViewModel(block), backgroundViewModel, borderViewModel)
            }
            is TextBlock -> {
                val textViewModel = TextViewModel(block, measurementService)
                val borderViewModel = BorderViewModel(block)
                TextBlockViewModel(
                    BlockViewModel(block, setOf(borderViewModel), textViewModel),
                    textViewModel,
                    BackgroundViewModel(block, assetService, imageOptimizationService),
                    borderViewModel
                )
            }
            is ImageBlock -> {
                val imageViewModel = ImageViewModel(block, assetService, imageOptimizationService)
                val borderViewModel = BorderViewModel(block)
                ImageBlockViewModel(
                    BlockViewModel(block, setOf(borderViewModel), imageViewModel),
                    BackgroundViewModel(block, assetService, imageOptimizationService),
                    imageViewModel,
                    borderViewModel
                )
            }
            is ButtonBlock -> {
                val blockViewModel = BlockViewModel(block)
                ButtonBlockViewModel(blockViewModel, ButtonViewModel(block, blockViewModel, this))
            }
            is WebViewBlock -> {
                val blockViewModel = BlockViewModel(block)
                WebViewBlockViewModel(block,
                    blockViewModel,
                    BackgroundViewModel(block, assetService, imageOptimizationService),
                    BorderViewModel(block), WebViewModel(block)
                )
            }
            is BarcodeBlock -> {
                val barcodeViewModel = BarcodeViewModel(
                    block,
                    measurementService
                )
                val borderViewModel = BorderViewModel(
                    block
                )
                val blockViewModel = BlockViewModel(block, setOf(borderViewModel, borderViewModel), barcodeViewModel)
                BarcodeBlockViewModel(
                    blockViewModel,
                    barcodeViewModel,
                    BackgroundViewModel(block, assetService, imageOptimizationService),
                    borderViewModel
                )
            }

            else -> throw Exception(
                "This Rover UI block type is not yet supported by the 2.0 SDK: ${block.javaClass.simpleName}."
            )
        }
    }

    override fun viewModelForRow(row: Row): RowViewModelInterface {
        return RowViewModel(
            row,
            this,
            BackgroundViewModel(
                row,
                assetService,
                imageOptimizationService
            )
        )
    }

    override fun viewModelForScreen(screen: Screen): ScreenViewModelInterface {
        return ScreenViewModel(screen,
            BackgroundViewModel(
                screen,
                assetService,
                imageOptimizationService
            ),
            this)
    }

    override fun viewModelForButtonState(buttonState: ButtonState): ButtonStateViewModelInterface {
        val borderViewModel = BorderViewModel(buttonState)

        return ButtonStateViewModel(
            buttonState,
            borderViewModel,
            BackgroundViewModel(buttonState, assetService, imageOptimizationService),
            TextViewModel(buttonState, measurementService, true, true)
        )
    }

    override fun viewModelForExperienceNavigation(experience: Experience, icicle: Parcelable?): ExperienceNavigationViewModelInterface {
        return ExperienceNavigationViewModel(
            experience,
            this,
            icicle
        )
    }

    override fun viewModelForExperience(experienceId: String, icicle: Parcelable?): ExperienceViewModelInterface {
        // Experience view models are singletons: ie., only one for each given experience. return a
        // cached live instance if we already have one, or recreate it from state if needed.
        return cachedExperienceViewModels.getOrPut(experienceId) {
            ExperienceViewModel(
                experienceId,
                networkService,
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
}
