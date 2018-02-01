package io.rover.rover.plugins.userexperience.experience.containers

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import com.facebook.stetho.urlconnection.ByteArrayRequestEntity
import com.facebook.stetho.urlconnection.StethoURLConnectionManager
import io.rover.rover.R
import io.rover.rover.Rover
import io.rover.rover.core.logging.log
import io.rover.rover.core.streams.androidLifecycleDispose
import io.rover.rover.core.streams.subscribe
import io.rover.rover.platform.asAndroidUri
import io.rover.rover.plugins.data.http.AsyncTaskAndHttpUrlConnectionInterception
import io.rover.rover.plugins.data.http.AsyncTaskAndHttpUrlConnectionInterceptor
import io.rover.rover.plugins.data.http.AsyncTaskAndHttpUrlConnectionNetworkClient
import io.rover.rover.plugins.userexperience.experience.ExperienceView
import io.rover.rover.plugins.userexperience.experience.ExperienceViewModelInterface
import io.rover.rover.plugins.userexperience.experience.navigation.ExperienceExternalNavigationEvent
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection

/**
 * This can display a Rover experience in an Activity, self-contained.
 *
 * You may use this either as a subclass for your own Activity, or as a template for embedding
 * a Rover [ExperienceView] in your own Activities.
 */
open class StandaloneExperienceHostActivity : AppCompatActivity() {
    private val experienceId
        get() = this.intent.getStringExtra("EXPERIENCE_ID") ?: throw RuntimeException(
            "Please pass EXPERIENCE_ID. Consider using StandaloneExperienceHostActivity.makeIntent()"
        )

    /**
     * This method is responsible for performing external navigation events: that is, navigation
     * events emitted by an Experience that "break out" of the Experience's intrinsic navigation
     * flow (ie., moving back and forth amongst Screens).  The default implementation handles
     * exiting the Activity and opening up a web browser.
     *
     * You may override this in a subclass if you want to handle the
     * [ExperienceExternalNavigationEvent.Custom] event you may have emitted in view model subclass,
     * to do some other sort of external behaviour in your app, such as open a native login screen.
     */
    protected open fun dispatchExternalNavigationEvent(externalNavigationEvent: ExperienceExternalNavigationEvent) {
        when (externalNavigationEvent) {
            is ExperienceExternalNavigationEvent.Exit -> {
                finish()
            }
            is ExperienceExternalNavigationEvent.OpenExternalWebBrowser -> {
                try {
                    ContextCompat.startActivity(
                        this,
                        Intent(
                            Intent.ACTION_VIEW,
                            externalNavigationEvent.uri.asAndroidUri()
                        ),
                        null
                    )
                } catch (e: ActivityNotFoundException) {
                    log.w("No way to handle URI ${externalNavigationEvent.uri}.  Perhaps app is missing an intent filter for a deep link?")
                }
            }
            is ExperienceExternalNavigationEvent.Custom -> {
                log.w("You have emitted a Custom event: $externalNavigationEvent, but did not handle it in your subclass implementation of StandaloneExperienceHostActivity.dispatchExternalNavigationEvent()")
            }
        }
    }

    // We're actually just showing a single screen for now
    // private val experiencesView by lazy { ScreenView(this) }
    protected val experiencesView by lazy { ExperienceView(this) }


    private val userExperiencePlugin by lazy {
        Rover.sharedInstance.userExperiencePlugin
    }

    private var experienceViewModel: ExperienceViewModelInterface? = null
        set(viewModel) {
            field = viewModel

            experiencesView.viewModel = viewModel

            viewModel
                ?.events
                ?.androidLifecycleDispose(this)
                ?.subscribe(
                    { event ->
                        when (event) {
                            is ExperienceViewModelInterface.Event.ExternalNavigation -> {
                                log.v("Received an external navigation event: ${event.event}")
                                dispatchExternalNavigationEvent(event.event)
                            }
                        }
                    }, { error ->
                        throw(error)
                    }
                )
        }

    override fun onBackPressed() {
        if (experienceViewModel != null) {
            experienceViewModel?.pressBack()
        } else {
            // default to standard Android back-button behaviour (ie., pop the activity) if our view
            // model isn't yet available.
            super.onBackPressed()
        }
    }

    // The View needs to know about the Activity-level window and several other
    // Activity/Fragment context things in order to temporarily change the backlight.
    private val toolbarHost by lazy { ActivityToolbarHost(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val displayNoCustomThemeWarningMessage = this.theme.obtainStyledAttributes(
            intArrayOf(R.attr.displayNoCustomThemeWarningMessage)
        ).getBoolean(0, false)

        if(displayNoCustomThemeWarningMessage) {
            log.w("You have set no theme for StandaloneExperienceHostActivity (or your optional subclass thereof) in your AndroidManifest.xml.\n" +
                "In particular, this means the toolbar will not pick up your brand colours.")
        }

        setContentView(
            experiencesView
        )

        // wire up the toolbar host to the ExperienceView.
        experiencesView.toolbarHost = toolbarHost

        experienceViewModel = userExperiencePlugin.viewModelForExperience(experienceId, savedInstanceState?.getParcelable("experienceState"))
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        // ExperienceView owns the toolbar, this is so ExperienceView can take your menu and include
        // it in its internal toolbar.
        toolbarHost.menu = menu
        return true
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable("experienceState", experienceViewModel?.state)
    }

    companion object {
        @JvmStatic
        @JvmOverloads
        fun makeIntent(packageContext: Context, experienceId: String, activityClass: Class<out Activity> = StandaloneExperienceHostActivity::class.java): Intent {
            return Intent(packageContext, activityClass).apply {
                putExtra("EXPERIENCE_ID", experienceId)
            }
        }
    }

    /**
     * If you want to be able to see the requests made by the Rover SDK to our API in
     * [Stetho's](http://facebook.github.io/stetho/) network inspector, copy this class into your
     * application and set an instance of it on the [AsyncTaskAndHttpUrlConnectionNetworkClient] with
     * [AsyncTaskAndHttpUrlConnectionNetworkClient.registerInterceptor] (DI instructions for
     * users to follow).
     *
     * TODO: this will be deleted, the stetho dependency removed, and just live in the
     * documentation instead.
     */
    class StethoRoverInterceptor : AsyncTaskAndHttpUrlConnectionInterceptor {
        override fun onOpened(httpUrlConnection: HttpURLConnection, requestPath: String, body: ByteArray): AsyncTaskAndHttpUrlConnectionInterception {
            val connManager = StethoURLConnectionManager(requestPath)
            connManager.preConnect(httpUrlConnection, ByteArrayRequestEntity(body))

            return object : AsyncTaskAndHttpUrlConnectionInterception {
                override fun onConnected() {
                    connManager.postConnect()
                }

                override fun onError(exception: IOException) {
                    connManager.httpExchangeFailed(exception)
                }

                override fun sniffStream(source: InputStream): InputStream =
                    connManager.interpretResponseStream(source)
            }
        }
    }
}

