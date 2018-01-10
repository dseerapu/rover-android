package io.rover.rover.ui.containers

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
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
import io.rover.rover.streams.subscribe
import io.rover.rover.ui.AndroidMeasurementService
import io.rover.rover.ui.AndroidRichTextToSpannedTransformer
import io.rover.rover.ui.ViewModelFactory
import io.rover.rover.ui.viewmodels.ExperienceViewEvent
import io.rover.rover.ui.viewmodels.ExperienceViewModelInterface
import io.rover.rover.ui.views.ExperienceView
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
                        is ExperienceViewModelInterface.Event.ViewEvent -> {
                            log.v("Received a view event: ${event.event}")
                            when(event.event) {
                                is ExperienceViewEvent.Exit -> {
                                    finish()
                                }
                                is ExperienceViewEvent.OpenExternalWebBrowser -> {
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(
            experiencesView
        )
        // The View needs to know about the Activity-level window in order to temporarily change the
        // backlight.
        experiencesView.attachedWindow = this.window
        // setSupportActionBar(experiencesView.toolbar)

        experienceViewModel = blockViewModelFactory.viewModelForExperience(
            experienceId, savedInstanceState?.getParcelable("experienceState")
        )
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
