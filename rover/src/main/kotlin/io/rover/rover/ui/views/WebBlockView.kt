package io.rover.rover.ui.views

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.webkit.WebView
import io.rover.rover.core.logging.log
import io.rover.rover.ui.viewmodels.WebViewBlockViewModelInterface
import android.view.MotionEvent



class WebBlockView : WebView, LayoutableView<WebViewBlockViewModelInterface> {
    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)

    // mixins (TODO: injections)
    private val viewComposition = ViewComposition()

    private val viewBackground = ViewBackground(this)
    private val viewBorder = ViewBorder(this, viewComposition)
    private val viewBlock = ViewBlock(this)
    private val viewWeb = ViewWeb(this)

    override var viewModel: WebViewBlockViewModelInterface? = null
        set(value) {
            field = value

            viewBorder.borderViewModel = viewModel
            viewBlock.blockViewModel = viewModel
            viewBackground.backgroundViewModel = viewModel
            viewWeb.webViewModel = viewModel
        }

    override fun onDraw(canvas: Canvas) {
        viewComposition.beforeOnDraw(canvas)
        super.onDraw(canvas)
        viewComposition.afterOnDraw(canvas)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        viewComposition.onSizeChanged(w, h, oldw, oldh)
    }

    @SuppressLint("MissingSuperCall")
    override fun requestLayout() {
        log.v("Tried to invalidate layout.  Inhibited.")
    }

    override fun forceLayout() {
        log.v("Tried to forcefully invalidate layout.  Inhibited.")
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // TODO: sadly this cannot be delegated readily to ViewWeb because it requires using this
        // override, so we'll ask the view model from here.  While I could teach ViewComposition
        // about TouchEvent, because handlers can consume events it is unclear

        requestDisallowInterceptTouchEvent((viewModel?.scrollingEnabled) ?: true)
        return super.onTouchEvent(event)
    }
}
