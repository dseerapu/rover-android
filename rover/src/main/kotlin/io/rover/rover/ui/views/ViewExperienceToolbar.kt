package io.rover.rover.ui.views

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
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
    private val toolbar: Toolbar,
    private val hostWindowForStatusBar: Window
): ViewExperienceToolbarInterface {
    private val defaultStyleBackground = toolbar.background
    private val defaultStatusBarColor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        hostWindowForStatusBar.statusBarColor
    } else 0

    override var menu: Menu? = null
        set(newMenu) {
            field = newMenu

            // TODO: use string resource.
            closeMenuItem = menu?.add("Close")?.apply {
                isVisible = false
                setOnMenuItemClickListener {
                    experienceToolbarViewModel?.pressedClose()
                    true
                }
                setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
            }

            toolbar.setNavigationOnClickListener {
                experienceToolbarViewModel?.pressedBack()
            }

            applyMenu(newMenu, toolbarConfiguration, actionBarWrapper)
        }

    override var actionBarWrapper: ActionBar? = null
        set(bar) {
            field = bar

            applyMenu(menu, toolbarConfiguration, bar)
        }

    private fun applyMenu(menu: Menu?, configuration: ToolbarConfiguration?, actionBarWrapper: ActionBar?) {
        if(menu != null && configuration != null && actionBarWrapper != null) {
            actionBarWrapper.setDisplayHomeAsUpEnabled(configuration.backButton)
            
            closeMenuItem?.isVisible = configuration.closeButton

            closeMenuItem?.title =
                if(configuration.useExistingStyle) {
                    "Close"
                } else {
                    SpannableStringBuilder("Close").apply {
                        setSpan(ForegroundColorSpan(Color.RED), 0, "Close".length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    }
                }
        }
    }

    private var closeMenuItem: MenuItem? = null
    private var toolbarConfiguration: ToolbarConfiguration? = null

    private fun recursiveColorHack() {

        // we want to apply our colour programmatically, since the supported method of using an
        // Android style/theme at view-creation time doesn't suit.

        val views = ArrayList<View>()

        (0 until toolbar.childCount).mapTo(views) { toolbar.getChildAt(it) }

        views.forEach { view ->
            log.v("Discovered a view of type ${view.javaClass.name}")
            when(view) {
                is TextView -> view.setTextColor(Color.GREEN)
                is AppCompatTextView -> view.setTextColor(Color.GREEN)
            }
        }

    }

    override var experienceToolbarViewModel: ExperienceToolbarViewModelInterface? = null
        set(viewModel) {
            field = viewModel

            toolbarConfiguration = null

            viewModel?.toolbarEvents
                ?.androidLifecycleDispose(toolbar)
                ?.subscribe { event ->
                    when(event) {
                        is ExperienceToolbarViewModelInterface.Event.SetToolbar -> {
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


                            // recursiveColorHack()

                            toolbar.setTitleTextColor(Color.GREEN)


                            // the toolbar only because available asynchronously, and it's not
                            // readily available as an Rx stream, so we'll just have a method check
                            // if the action can be performed in either case.

                            toolbarConfiguration = configuration
                            applyMenu(menu, configuration, actionBarWrapper)
                        }
                    }
                }
        }
}
