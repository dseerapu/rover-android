package io.rover.rover.ui.views

import android.view.MotionEvent
import android.webkit.WebView
import io.rover.rover.ui.viewmodels.WebViewModelInterface

class ViewWeb(
    private val webView: WebView
): ViewWebInterface {
    override var webViewModel: WebViewModelInterface? = null
        set(viewModel) {
            field = viewModel

            webView.loadUrl(
                viewModel?.url?.toString() ?: "about://blank"
            )
        }
}