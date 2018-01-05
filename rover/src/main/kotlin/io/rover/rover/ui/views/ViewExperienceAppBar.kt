package io.rover.rover.ui.views

import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.support.v7.app.ActionBar
import android.support.v7.appcompat.R.attr.colorPrimary
import android.support.v7.appcompat.R.attr.colorPrimaryDark
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.util.TypedValue
import android.view.View
import android.view.Window
import io.rover.rover.streams.androidLifecycleDispose
import io.rover.rover.streams.subscribe
import io.rover.rover.ui.viewmodels.ExperienceAppBarViewModelInterface

/**
 *
 */
class ViewExperienceAppBar(
    val hostView: View,
    val supportActionBar: ActionBar,
    val hostWindowForStatusBar: Window
): ViewExperienceAppBarInterface {

    // we need to determine what the default background color is so we can restore it when we
    // receive an action bar configuration with [ActionBarConfiguration.useGlobalTheme] turned off.
//    private val actionBarId = hostView.resources.getIdentifier("action_bar", "id", "android");
//    private val actionBarView: View = hostView.findViewById(actionBarId)
//    private val defaultBackground = actionBarView.background

//    private val canary = View(hostView.context)
//    init {
//        supportActionBar.customView = canary
//    }
//    private val actionBarView = canary.parent.parent
//    init {
//        supportActionBar.customView = null
//    }
//    private val defaultBackground = actionBarView.background

    private val themeColorPrimary = TypedValue().apply {
        supportActionBar.themedContext.theme.resolveAttribute(colorPrimary, this, true)
    }.data

    private val themeColorPrimaryDark = TypedValue().apply {
        supportActionBar.themedContext.theme.resolveAttribute(colorPrimaryDark, this, true)
    }.data

    override var experienceAppBarViewModel: ExperienceAppBarViewModelInterface? = null
        set(viewModel) {
            field = viewModel

            supportActionBar.hide()

            viewModel?.events
                ?.androidLifecycleDispose(hostView)
                ?.subscribe { event ->
                    supportActionBar.show()
                    val configuration = event.appBarConfiguration

                    supportActionBar.title = if(configuration.useGlobalTheme) {
                        configuration.appBarText
                    } else {
                        SpannableStringBuilder(configuration.appBarText).apply {
                            setSpan(ForegroundColorSpan(configuration.textColor), 0, configuration.appBarText.length, 0)
                        }
                    }

                    val actionBarBackgroundColor = if(configuration.useGlobalTheme) themeColorPrimary else configuration.color

                    supportActionBar.setBackgroundDrawable(
                        ColorDrawable(actionBarBackgroundColor)
                    )

                    // status bar color only supported on Lollipop and greater.
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        hostWindowForStatusBar.statusBarColor = if(configuration.useGlobalTheme) themeColorPrimaryDark else {
                            configuration.statusBarColor
                        }
                    }
                }
        }
}