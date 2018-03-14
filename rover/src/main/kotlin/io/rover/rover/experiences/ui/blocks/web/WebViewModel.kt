package io.rover.rover.experiences.ui.blocks.web

import io.rover.rover.core.data.domain.WebViewBlock
import java.net.URL

class WebViewModel(
    private val webViewBlock: WebViewBlock
) : WebViewModelInterface {

    override val url: URL
        get() = webViewBlock.url.toURL()

    override val scrollingEnabled: Boolean
        get() = webViewBlock.isScrollingEnabled
}
