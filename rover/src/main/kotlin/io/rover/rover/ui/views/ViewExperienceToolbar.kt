package io.rover.rover.ui.views

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.support.v7.widget.Toolbar
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.View
import android.view.Window
import io.rover.rover.core.logging.log
import io.rover.rover.streams.androidLifecycleDispose
import io.rover.rover.streams.subscribe
import io.rover.rover.ui.viewmodels.ExperienceToolbarViewModelInterface


class ViewExperienceToolbar(
    private val toolbar: Toolbar,
    private val hostWindowForStatusBar: Window
): ViewExperienceToolbarInterface {
    private val defaultStyleBackground = toolbar.background
    private val defaultStatusBarColor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        hostWindowForStatusBar.statusBarColor
    } else 0

    init {
//        toolbar.setBackgroundColor(Color.RED)
//        toolbar.title = "OI"
    }

    override var experienceToolbarViewModel: ExperienceToolbarViewModelInterface? = null
        set(viewModel) {
            field = viewModel

            //toolbar.visibility = View.INVISIBLE

            viewModel?.toolbarEvents
                ?.androidLifecycleDispose(toolbar)
                ?.subscribe { event ->
                    log.v("GOT TOOLBAR CALLBACK")
                    //toolbar.visibility = View.VISIBLE

                    val configuration = event.toolbarConfiguration

                    toolbar.title = if(configuration.useExistingStyle) {
                        configuration.appBarText
                    } else {
                        SpannableStringBuilder(configuration.appBarText).apply {
                            setSpan(ForegroundColorSpan(configuration.textColor), 0, configuration.appBarText.length, 0)
                        }
                    }

                    toolbar.background = if(configuration.useExistingStyle) {
                        defaultStyleBackground
                    } else {
                        ColorDrawable(configuration.color)
                    }

                    // status bar color only supported on Lollipop and greater.
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        hostWindowForStatusBar.statusBarColor = if(configuration.useExistingStyle) defaultStatusBarColor else {
                            configuration.statusBarColor
                        }
                    }
                }
        }
}
