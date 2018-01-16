package io.rover.rover.ui.views

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Handler
import android.support.v4.widget.TextViewCompat
import android.support.v7.app.ActionBar
import android.support.v7.view.menu.MenuBuilder
import android.support.v7.widget.AppCompatTextView
import android.support.v7.widget.Toolbar
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.Window
import android.widget.TextView
import io.rover.rover.core.logging.log
import io.rover.rover.streams.androidLifecycleDispose
import io.rover.rover.streams.subscribe
import io.rover.rover.ui.types.ToolbarConfiguration
import io.rover.rover.ui.viewmodels.ExperienceNavigationViewModel
import io.rover.rover.ui.viewmodels.ExperienceToolbarViewModelInterface


class ViewExperienceToolbar(
    private val hostWindowForStatusBar: Window,
    private val experienceNavigationViewModel: ExperienceNavigationViewModel
): ViewExperienceToolbarInterface {


    companion object {

        fun generateToolbar(
            hostView: View,
            viewModel: ExperienceToolbarViewModelInterface,
            context: Context,
            toolbarHost: ExperienceView.ToolbarHost,
            defaultStatusBarColor: Int
        ): Toolbar {
            val configuration = viewModel.configuration

            // TODO: theme overlay on context
            val toolbar = Toolbar(context)

            val actionBarWrapper = toolbarHost.setToolbarAsActionBar(toolbar)
                .androidLifecycleDispose(hostView)
                .subscribe { (actionBar, menu)  ->
                    actionBar.setDisplayHomeAsUpEnabled(true)

                    // TODO: I'm stacking up duplicates menu items.  Perhaps this should be a
                    // persistent object after all with state, namely tracking the already-added
                    // Close button. ANDREW START HERE

                    val closeMenuItem = menu.add("Close").apply {
                        isVisible = false
                        setOnMenuItemClickListener {
                            viewModel.pressedClose()
                            true
                        }
                        setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)

                        title = "Close"
    //                    if(viewModel.configuration.useExistingStyle) {
    //                        "Close"
    //                    } else {
    //                        SpannableStringBuilder("Close").apply {
    //                            setSpan(ForegroundColorSpan(Color.RED), 0, "Close".length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    //                        }
    //                    }

                        // TODO: this one must be changed to style method
                        toolbar.title = if (configuration.useExistingStyle) {
                            configuration.appBarText
                        } else {
                            SpannableStringBuilder(configuration.appBarText).apply {
                                setSpan(ForegroundColorSpan(configuration.textColor), 0, configuration.appBarText.length, 0)
                            }
                        }

                        if (!configuration.useExistingStyle) {
                            // TODO may do with the style above instead
                            toolbar.background = ColorDrawable(configuration.color)
                        }

                        // status bar color only supported on Lollipop and greater.
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            toolbarHost.provideWindow().statusBarColor = if (configuration.useExistingStyle) defaultStatusBarColor else {
                                configuration.statusBarColor
                            }
                        }

                    }

                    closeMenuItem.isVisible = viewModel.configuration.closeButton
                }

            return toolbar
        }
    }
}
