package io.rover.rover.ui.viewmodels

import io.rover.rover.core.domain.WebViewBlock
import io.rover.rover.core.logging.log
import java.net.URL


class WebViewModel(
    private val webViewBlock: WebViewBlock
) : WebViewModelInterface {

    override val url: URL
        get() = webViewBlock.url.toURL()

    override val scrollingEnabled: Boolean
        get() = webViewBlock.isScrollingEnabled
}
