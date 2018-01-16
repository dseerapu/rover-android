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
import io.rover.rover.streams.Publisher
import io.rover.rover.streams.Subscriber
import io.rover.rover.streams.Subscription
import io.rover.rover.streams.androidLifecycleDispose
import io.rover.rover.streams.doOnUnsubscribe
import io.rover.rover.streams.subscribe
import io.rover.rover.ui.types.ToolbarConfiguration
import io.rover.rover.ui.viewmodels.ExperienceNavigationViewModel
import io.rover.rover.ui.viewmodels.ExperienceToolbarViewModelInterface


class ViewExperienceToolbar(
    private val hostView: View,
    private val hostWindowForStatusBar: Window,
    private val context: Context,
    private val toolbarHost: ExperienceView.ToolbarHost
): ViewExperienceToolbarInterface {
    private val menuItemId = View.generateViewId()

    private val defaultStatusBarColor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        hostWindowForStatusBar.statusBarColor
    } else 0

    // we need to create and hold a MenuItem
    private var activeCloseMenuItem: MenuItem? = null

    /**
     * We'll cancel the subscription to the observable chain created in setViewModelAndReturnToolbar()
     */
    private var activeMenuSubscription: Subscription? = null

    private var retrievedMenu : Menu? = null

    override fun setViewModelAndReturnToolbar(
        toolbarViewModel: ExperienceToolbarViewModelInterface
    ): Toolbar {
        val configuration = toolbarViewModel.configuration

        // TODO: theme overlay on context
        val toolbar = Toolbar(context)

        // I need to keep state for the toolbar subscription so I can unsubscribe it when bind.
        activeMenuSubscription?.cancel()

        toolbarHost.setToolbarAsActionBar(toolbar)
            .androidLifecycleDispose(hostView)
            .doOnUnsubscribe {
                log.v("Removing exist item with id $menuItemId from $retrievedMenu")
                retrievedMenu?.removeItem(menuItemId)

            }
            .subscribe({ (actionBar, menu) ->
                actionBar.setDisplayHomeAsUpEnabled(true)

                if(retrievedMenu != menu) {
                    // the menu has changed.  We'll recreate our dynamic menu option.
                    retrievedMenu = menu
                    activeCloseMenuItem = menu.add(Menu.NONE, menuItemId, Menu.NONE, "Close").apply {
                        isVisible = false
                        setOnMenuItemClickListener {
                            toolbarViewModel.pressedClose()
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
                    }
                }

                toolbar.setNavigationOnClickListener {
                    toolbarViewModel.pressedBack()
                }

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

                activeCloseMenuItem!!.isVisible = toolbarViewModel.configuration.closeButton
            }, { error -> throw(error) }, { subscription ->
                activeMenuSubscription = subscription
            })

        return toolbar
    }
}
