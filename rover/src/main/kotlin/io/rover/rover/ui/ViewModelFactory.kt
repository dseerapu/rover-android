package io.rover.rover.ui

import io.rover.rover.core.domain.BarcodeBlock
import io.rover.rover.core.domain.Block
import io.rover.rover.core.domain.ButtonBlock
import io.rover.rover.core.domain.ButtonState
import io.rover.rover.core.domain.ImageBlock
import io.rover.rover.core.domain.RectangleBlock
import io.rover.rover.core.domain.Row
import io.rover.rover.core.domain.Screen
import io.rover.rover.core.domain.TextBlock
import io.rover.rover.core.domain.WebViewBlock
import io.rover.rover.services.assets.AssetService
import io.rover.rover.services.assets.ImageOptimizationServiceInterface
import io.rover.rover.ui.viewmodels.BackgroundViewModel
import io.rover.rover.ui.viewmodels.BarcodeBlockViewModel
import io.rover.rover.ui.viewmodels.BarcodeViewModel
import io.rover.rover.ui.viewmodels.BlockViewModel
import io.rover.rover.ui.viewmodels.BlockViewModelInterface
import io.rover.rover.ui.viewmodels.BorderViewModel
import io.rover.rover.ui.viewmodels.ButtonBlockViewModel
import io.rover.rover.ui.viewmodels.ButtonStateViewModel
import io.rover.rover.ui.viewmodels.ButtonStateViewModelInterface
import io.rover.rover.ui.viewmodels.ButtonViewModel
import io.rover.rover.ui.viewmodels.ImageBlockViewModel
import io.rover.rover.ui.viewmodels.ImageViewModel
import io.rover.rover.ui.viewmodels.RectangleBlockViewModel
import io.rover.rover.ui.viewmodels.RowViewModel
import io.rover.rover.ui.viewmodels.RowViewModelInterface
import io.rover.rover.ui.viewmodels.ScreenViewModel
import io.rover.rover.ui.viewmodels.ScreenViewModelInterface
import io.rover.rover.ui.viewmodels.TextBlockViewModel
import io.rover.rover.ui.viewmodels.TextViewModel
import io.rover.rover.ui.viewmodels.WebViewBlockViewModel
import io.rover.rover.ui.viewmodels.WebViewModel

interface ViewModelFactoryInterface {
    fun viewModelForBlock(block: Block): BlockViewModelInterface

    fun viewModelForRow(row: Row): RowViewModelInterface

    fun viewModelForScreen(screen: Screen): ScreenViewModelInterface

    fun viewModelForButtonState(buttonState: ButtonState): ButtonStateViewModelInterface
}

class ViewModelFactory(
    private val measurementService: MeasurementService,
    private val assetService: AssetService,
    private val imageOptimizationService: ImageOptimizationServiceInterface
) : ViewModelFactoryInterface {
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
}
