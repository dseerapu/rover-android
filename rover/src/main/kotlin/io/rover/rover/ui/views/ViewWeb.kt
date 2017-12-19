package io.rover.rover.ui.views

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
    }

    override var webViewModel: WebViewModelInterface? = null
        set(viewModel) {
            field = viewModel

            webView.loadUrl(
                viewModel?.url?.toString() ?: "about://blank"
            )
        }
}