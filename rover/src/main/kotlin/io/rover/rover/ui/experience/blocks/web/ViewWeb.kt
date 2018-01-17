package io.rover.rover.ui.experience.blocks.web

import android.webkit.WebView
import android.webkit.WebViewClient
import io.rover.rover.ui.viewmodels.WebViewModelInterface

class ViewWeb(
    private val webView: WebView
): ViewWebInterface {
    init {
        WebView.setWebContentsDebuggingEnabled(true);
        // Setting an otherwise unconfigured WebViewClient will have the webview navigate (follow
        // links) internally.
        webView.webViewClient = WebViewClient()
        webView.settings.domStorageEnabled = true
        webView.settings.javaScriptEnabled = true

        // TODO disable the scroll bars if scrolling is disabled, because otherwise they'll appear
        // when you scroll by
    }

    override var webViewModel: WebViewModelInterface? = null
        set(viewModel) {
            field = viewModel

            webView.loadUrl(
                viewModel?.url?.toString() ?: "about://blank"
            )
        }
}