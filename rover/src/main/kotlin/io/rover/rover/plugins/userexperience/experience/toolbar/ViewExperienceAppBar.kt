package io.rover.rover.plugins.userexperience.experience.toolbar

import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.support.v7.app.ActionBar
import android.support.v7.appcompat.R.attr.colorPrimary
import android.support.v7.appcompat.R.attr.colorPrimaryDark
import android.support.v7.widget.Toolbar
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.util.TypedValue
import android.view.View
import android.view.Window
import io.rover.rover.core.streams.androidLifecycleDispose
import io.rover.rover.core.streams.subscribe
import io.rover.rover.plugins.userexperience.experience.ExperienceView

/**
 * This view concern wraps an [ActionBar] (that is, the specialized [Toolbar] that is provided as
 * part of Android appcompat activities and fragments as the so-called "system action bar").
 *
 * This view concern is used when the host activity/fragment of an [ExperienceView] is using
 * said stock system action bar.  If the host activity is using a custom toolbar in lieu of
 * the Android action bar, then see [?????] instead.
 */
class ViewExperienceAppBar(
    val hostView: View,
    val supportActionBar: ActionBar,
    val hostWindowForStatusBar: Window
) : ViewExperienceAppBarInterface {
    // This is a bit of a bug because the action bar style (by default) indirects to colorPrimary
    // from the theme.  However, if the developer has directly replaced or changed the action bar
    // style to do something different than the primary colour, than our attempt to use
    // themeColorPrimary below to "restore" the original state will in fact just clobber their
    // theme.  Retrieve `actionBarStyle` from user's theme, which is the customization point?
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

                    supportActionBar.title = if (configuration.useExistingStyle) {
                        configuration.appBarText
                    } else {
                        SpannableStringBuilder(configuration.appBarText).apply {
                            setSpan(ForegroundColorSpan(configuration.textColor), 0, configuration.appBarText.length, 0)
                        }
                    }

                    val actionBarBackgroundColor = if (configuration.useExistingStyle) themeColorPrimary else configuration.color

                    supportActionBar.setBackgroundDrawable(
                        ColorDrawable(actionBarBackgroundColor)
                    )

                    // status bar color only supported on Lollipop and greater.
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        hostWindowForStatusBar.statusBarColor = if (configuration.useExistingStyle) themeColorPrimaryDark else {
                            configuration.statusBarColor
                        }
                    }
                }
        }
}