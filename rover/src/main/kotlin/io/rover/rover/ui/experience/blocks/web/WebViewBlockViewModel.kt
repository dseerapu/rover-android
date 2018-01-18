package io.rover.rover.ui.experience.blocks.web

import io.rover.rover.core.domain.WebViewBlock
import io.rover.rover.ui.experience.layout.ViewType
import io.rover.rover.ui.experience.blocks.concerns.background.BackgroundViewModelInterface
import io.rover.rover.ui.experience.blocks.concerns.layout.BlockViewModelInterface
import io.rover.rover.ui.experience.blocks.concerns.border.BorderViewModelInterface

class WebViewBlockViewModel(
    private val block: WebViewBlock,
    private val blockViewModel: BlockViewModelInterface,
    private val backgroundViewModel: BackgroundViewModelInterface,
    private val borderViewModel: BorderViewModelInterface,
    private val webViewModel: WebViewModelInterface
) : WebViewBlockViewModelInterface,
    BlockViewModelInterface by blockViewModel,
    BackgroundViewModelInterface by backgroundViewModel,
    BorderViewModelInterface by borderViewModel,
    WebViewModelInterface by webViewModel {
    override val viewType: ViewType = ViewType.WebView
}