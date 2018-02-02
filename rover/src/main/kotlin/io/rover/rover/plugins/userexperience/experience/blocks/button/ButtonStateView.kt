package io.rover.rover.plugins.userexperience.experience.blocks.button

import android.content.Context
import android.graphics.Canvas
import android.os.Build
import android.support.annotation.RequiresApi
import android.util.AttributeSet
import android.view.View
import io.rover.rover.plugins.userexperience.experience.ViewModelBinding
import io.rover.rover.plugins.userexperience.experience.blocks.concerns.ViewComposition
import io.rover.rover.plugins.userexperience.experience.blocks.concerns.background.ViewBackground
import io.rover.rover.plugins.userexperience.experience.blocks.concerns.border.ViewBorder
import io.rover.rover.plugins.userexperience.experience.concerns.BindableView

class ButtonStateView : View, BindableView<ButtonStateViewModelInterface> {
    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)

    override var viewModel: ButtonStateViewModelInterface? by ViewModelBinding { viewModel, _ ->
            viewBackground.backgroundViewModel = viewModel
            viewBorder.borderViewModel = viewModel
        }

    private val viewComposition = ViewComposition()
    private val viewBackground = ViewBackground(this, viewComposition)
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
