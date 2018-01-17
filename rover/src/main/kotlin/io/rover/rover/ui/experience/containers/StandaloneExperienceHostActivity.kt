package io.rover.rover.ui.experience.containers

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.ActionBar
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.Window
import com.facebook.stetho.urlconnection.ByteArrayRequestEntity
import com.facebook.stetho.urlconnection.StethoURLConnectionManager
import io.rover.rover.core.logging.log
import io.rover.rover.platform.DateFormatting
import io.rover.rover.platform.DeviceIdentification
import io.rover.rover.platform.SharedPreferencesLocalStorage
import io.rover.rover.platform.asAndroidUri
import io.rover.rover.services.assets.AndroidAssetService
import io.rover.rover.services.assets.ImageOptimizationService
import io.rover.rover.services.network.AsyncTaskAndHttpUrlConnectionInterception
import io.rover.rover.services.network.AsyncTaskAndHttpUrlConnectionInterceptor
import io.rover.rover.services.network.AsyncTaskAndHttpUrlConnectionNetworkClient
import io.rover.rover.services.network.AuthenticationContext
import io.rover.rover.services.network.NetworkService
import io.rover.rover.services.network.NetworkServiceInterface
import io.rover.rover.services.network.WireEncoder
import io.rover.rover.streams.Observable
import io.rover.rover.streams.PublishSubject
import io.rover.rover.streams.share
import io.rover.rover.streams.subscribe
import io.rover.rover.ui.AndroidMeasurementService
import io.rover.rover.ui.experience.blocks.concerns.text.AndroidRichTextToSpannedTransformer
import io.rover.rover.ui.ViewModelFactory
import io.rover.rover.ui.experience.navigation.ExperienceExternalNavigationEvent
import io.rover.rover.ui.experience.ExperienceViewModelInterface
import io.rover.rover.ui.experience.ExperienceView
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * This can display a Rover experience in an Activity, self-contained.
 */
class StandaloneExperienceHostActivity: AppCompatActivity() {
    // so, a few constraints:

    // may be booted inside any app, so needs to start up our object graph as needed. this story
    // will need to change as we grow a better DI.
    // we then need to get it an auth token. this comes from one of two possibilities: dynamically set (usually only by internal Rover apps), or set by a customer using the Rover SDK facade.

    // probably offer the dynamically set method as an activity argument or something.

    private val authToken
        get() = this.intent.getStringExtra("SDK_TOKEN") ?: throw RuntimeException("General Rover SDK configuration not yet available; please pass SDK_TOKEN intent argument.")

    private val experienceId
        get() = this.intent.getStringExtra("EXPERIENCE_ID") ?: throw RuntimeException("Please pass EXPERIENCE_ID.")

    // We're actually just showing a single screen for now
    // private val experiencesView by lazy { ScreenView(this) }
    private val experiencesView by lazy { ExperienceView(this) }

    // TODO: somehow share this properly
    private val roverSdkNetworkService by lazy {
        NetworkService(
            // "6c546189dc45df1293bddc18c0b54786"
            object : AuthenticationContext {
                override val bearerToken: String?
                    get() = null

                override val sdkToken: String?
                    get() = this@StandaloneExperienceHostActivity.authToken
            },
            URL("https://api.rover.io/graphql"),
            networkClient,
            DeviceIdentification(
                SharedPreferencesLocalStorage(applicationContext)
            ),
            WireEncoder(DateFormatting()),
            null
        ) as NetworkServiceInterface
    }

    private val networkClient by lazy {
        // side-effect: set up global HttpsUrlConnection cache, as required by all Rover SDK
        // consuming apps.
        AsyncTaskAndHttpUrlConnectionNetworkClient.installSaneGlobalHttpCacheCache(this)

        AsyncTaskAndHttpUrlConnectionNetworkClient().apply {
            registerInterceptor(
                StethoRoverInterceptor()
            )
        }
    }

    private val ioExecutor by lazy {
        ThreadPoolExecutor(
            10,
            Runtime.getRuntime().availableProcessors() * 20,
            2,
            TimeUnit.SECONDS,
            LinkedBlockingQueue<Runnable>()
        )
    }

    private val blockViewModelFactory by lazy {
        ViewModelFactory(
            AndroidMeasurementService(resources.displayMetrics, AndroidRichTextToSpannedTransformer()),
            AndroidAssetService(
                networkClient,
                ioExecutor
            ),
            ImageOptimizationService(),
            roverSdkNetworkService
        )
    }

    // private var viewExperienceAppBar : ViewExperienceAppBarInterface? = null

    // TODO: there should be a standalone-experience-host-activity view model.
    private var experienceViewModel: ExperienceViewModelInterface? = null
        set(viewModel) {
            field = viewModel

            experiencesView.viewModel = viewModel

            // TODO: this subscription must be lifecycle-managed
            viewModel?.events?.subscribe(
                { event ->
                    when(event) {
                        is ExperienceViewModelInterface.Event.ExternalNavigation -> {
                            log.v("Received a view event: ${event.event}")
                            when(event.event) {
                                is ExperienceExternalNavigationEvent.Exit -> {
                                    finish()
                                }
                                is ExperienceExternalNavigationEvent.OpenExternalWebBrowser -> {
                                    try {
                                        ContextCompat.startActivity(
                                            this,
                                            Intent(
                                                Intent.ACTION_VIEW,
                                                event.event.uri.asAndroidUri()
                                            ),
                                            null
                                        )
                                    } catch (e: ActivityNotFoundException) {
                                        log.w("No way to handle URI ${event.event.uri}.  Perhaps app is missing an intent filter for a deep link?")
                                    }
                                }
                            }
                        }
                    }
                }, { error ->
                    throw(error)
                }
            )
        }

    override fun onBackPressed() {
        if(experienceViewModel != null) {
            experienceViewModel?.pressBack()
        } else {
            // default to standard Android back-button behaviour (ie., pop the activity) if our view
            // model isn't yet available.
            super.onBackPressed()
        }
    }

    // The View needs to know about the Activity-level window and several other
    // Activity/Fragment context things in order to temporarily change the backlight.
    private val toolbarHost = ActivityToolbarHost(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(
            experiencesView
        )

        experiencesView.toolbarHost = toolbarHost

        // i left off here. options menu?

        experienceViewModel = blockViewModelFactory.viewModelForExperience(
            experienceId, savedInstanceState?.getParcelable("experienceState")
        )
    }

    /**
     * Sadly, menu arrives somewhat asynchronously: specifically, after setSupportActionBar() and
     * the system interrogates the Menu in order to populate things in the toolbar.  In ancient
     * versions of Android, this would occur even later because it was only done on first press of
     * the Menu button.
     */
    // private var capturedMenu: Menu? = null

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        toolbarHost.menu = menu
        // capturedMenu = menu
        return true
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable("experienceState", experienceViewModel?.state)
    }

    companion object {
        fun makeIntent(packageContext: Context, authToken: String, experienceId: String): Intent {
            return Intent(packageContext, StandaloneExperienceHostActivity::class.java).apply {
                putExtra("SDK_TOKEN", authToken)
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

/**
 * Activities that wish to host [ExperienceView] must instantiate [ActivityToolbarHost], and
 * then also implement [AppCompatActivity.onCreateOptionsMenu] wherein they must then set the menu
 * on the toolbar host with [ActivityToolbarHost.menu].
 *
 * This arrangement is required on account of Android lacking
 *
 * It assumes that you are using [AppCompatActivity], which is strongly recommended in standard
 * Android apps.
 *
 * TODO: include direction about either setting the ActionBar feature to off in code or by style.
 */
class ActivityToolbarHost(private val activity: AppCompatActivity): ExperienceView.ToolbarHost {
    var menu: Menu? = null
        set(newMenu) {
            field = newMenu

            emitIfPrerequisitesAvailable()
        }

    private val emitterSubject = PublishSubject<Pair<ActionBar, Menu>>()
    private val emitter = emitterSubject.share()

    private var actionBar: ActionBar? = null

    private fun emitIfPrerequisitesAvailable() {
        val actionBar = actionBar
        val menu = menu

        if (actionBar != null && menu != null) {
            emitterSubject.onNext(Pair(actionBar, menu))
        }
    }

    override fun setToolbarAsActionBar(toolbar: Toolbar): Observable<Pair<ActionBar, Menu>> {
        activity.setSupportActionBar(toolbar)
        // and then retrieved the wrapped actionbar delegate
        actionBar = activity.supportActionBar
        return emitter
    }

    override fun provideWindow(): Window {
        return activity.window
    }
}