package io.rover.rover.experiences.ui.blocks.web

import io.rover.rover.experiences.ui.blocks.concerns.background.BackgroundViewModelInterface
import io.rover.rover.experiences.ui.blocks.concerns.border.BorderViewModelInterface
import io.rover.rover.experiences.ui.blocks.concerns.layout.BlockViewModelInterface
import io.rover.rover.experiences.ui.blocks.concerns.layout.CompositeBlockViewModelInterface
import io.rover.rover.experiences.ui.blocks.concerns.layout.LayoutableViewModel
import java.net.URL

interface ViewWebInterface {
    var webViewModel: WebViewModelInterface?
}

interface WebViewModelInterface {
    val url: URL
    val scrollingEnabled: Boolean
}

interface WebViewBlockViewModelInterface :
    CompositeBlockViewModelInterface,
    LayoutableViewModel,
    BlockViewModelInterface,
    BackgroundViewModelInterface,
    BorderViewModelInterface,
    WebViewModelInterface