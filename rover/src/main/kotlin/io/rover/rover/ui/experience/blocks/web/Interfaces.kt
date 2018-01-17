@file:JvmName("Interfaces")

package io.rover.rover.ui.experience.blocks.web

import io.rover.rover.ui.experience.blocks.concerns.background.BackgroundViewModelInterface
import io.rover.rover.ui.experience.blocks.concerns.border.BorderViewModelInterface
import io.rover.rover.ui.experience.blocks.concerns.layout.BlockViewModelInterface
import io.rover.rover.ui.experience.blocks.concerns.layout.LayoutableViewModel
import java.net.URL

interface ViewWebInterface {
    var webViewModel: WebViewModelInterface?
}

interface WebViewModelInterface {
    val url: URL
    val scrollingEnabled: Boolean
}

interface WebViewBlockViewModelInterface :
    LayoutableViewModel,
    BlockViewModelInterface,
    BackgroundViewModelInterface,
    BorderViewModelInterface,
    WebViewModelInterface