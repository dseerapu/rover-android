package io.rover.rover.plugins.userexperience.experience.blocks.web

import io.rover.rover.plugins.userexperience.experience.layout.ViewType
import io.rover.rover.plugins.userexperience.experience.blocks.concerns.background.BackgroundViewModelInterface
import io.rover.rover.plugins.userexperience.experience.blocks.concerns.layout.BlockViewModelInterface
import io.rover.rover.plugins.userexperience.experience.blocks.concerns.border.BorderViewModelInterface

class WebViewBlockViewModel(
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