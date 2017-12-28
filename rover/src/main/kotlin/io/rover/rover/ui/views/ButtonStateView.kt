package io.rover.rover.ui.views

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View
import io.rover.rover.streams.Observable
import io.rover.rover.streams.PublishSubject
import io.rover.rover.streams.share
import io.rover.rover.ui.viewmodels.ButtonStateViewModelInterface

class ButtonStateView: View, BindableView<ButtonStateViewModelInterface> {
    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)

    override var viewModel: ButtonStateViewModelInterface? = null
        set(buttonStateViewModel) {
            field = buttonStateViewModel

            viewBackground.backgroundViewModel = buttonStateViewModel
            viewBorder.borderViewModel = buttonStateViewModel
        }

    private val viewComposition = ViewComposition()
    private val viewBackground = ViewBackground(this)
    private val viewBorder = ViewBorder(this, viewComposition)

    override fun onDraw(canvas: Canvas) {
        viewComposition.beforeOnDraw(canvas)
        super.onDraw(canvas)
        viewComposition.afterOnDraw(canvas)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        viewComposition.onSizeChanged(w, h, oldw, oldh)
    }
//
//    override fun onAnimationEnd() {
//        super.onAnimationEnd()
//        // workaround for a bug with Android Animator listeners.
//        animationEndSubject.onNext(Unit)
//    }
//
//    private val animationEndSubject = PublishSubject<Unit>()
//
//    val animationEnds: Observable<Unit> = animationEndSubject.share()
}
