package io.rover.rover.ui.containers

import android.arch.lifecycle.Lifecycle
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.RecyclerView
import io.rover.rover.core.domain.ID
import io.rover.rover.core.logging.log
import io.rover.rover.platform.DateFormatting
import io.rover.rover.platform.DeviceIdentification
import io.rover.rover.platform.SharedPreferencesLocalStorage
import io.rover.rover.platform.asAndroidUri
import io.rover.rover.services.assets.AndroidAssetService
import io.rover.rover.services.assets.ImageOptimizationService
import io.rover.rover.services.network.AsyncTaskAndHttpUrlConnectionNetworkClient
import io.rover.rover.services.network.AuthenticationContext
import io.rover.rover.services.network.NetworkResult
import io.rover.rover.services.network.NetworkService
import io.rover.rover.services.network.NetworkServiceInterface
import io.rover.rover.services.network.WireEncoder
import io.rover.rover.streams.subscribe
import io.rover.rover.ui.AndroidMeasurementService
import io.rover.rover.ui.AndroidRichTextToSpannedTransformer
import io.rover.rover.ui.BlockAndRowLayoutManager
import io.rover.rover.ui.BlockAndRowRecyclerAdapter
import io.rover.rover.ui.ViewModelFactory
import io.rover.rover.ui.viewmodels.ExperienceViewModel
import io.rover.rover.ui.viewmodels.ExperienceViewModelInterface
import io.rover.rover.ui.viewmodels.ScreenViewModel
import io.rover.rover.ui.views.ExperienceView
import io.rover.rover.ui.views.ScreenView
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
        get() = this.intent.getStringExtra("SDK_TOKEN") ?: throw RuntimeException("General SDK configuration not yet available; please pass SDK_TOKEN intent argument.")

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

        AsyncTaskAndHttpUrlConnectionNetworkClient()
//        networkClient.registerInterceptor(
//            StethoRoverInterceptor()
//        )
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
            ImageOptimizationService()
        )
    }

    // TODO: there should be a standalone-experience-host-activity view model.
    private var experienceViewModel: ExperienceViewModelInterface? = null
        set(viewModel) {
            field = viewModel

            experiencesView.viewModel = viewModel

            // TODO: this subscription must be lifecycle-managed
            viewModel?.events?.subscribe(
                { event ->
                    when(event) {
                        is ExperienceViewModelInterface.Event.Exit -> {
                            finish()
                        }
                        is ExperienceViewModelInterface.Event.OpenExternalWebBrowser -> {
                            try {
                                ContextCompat.startActivity(
                                    this,
                                    Intent(
                                        Intent.ACTION_VIEW,
                                        event.uri.asAndroidUri()
                                    ),
                                    null
                                )
                            } catch (e: ActivityNotFoundException) {
                                log.w("No way to handle URI ${event.uri}.  Perhaps app is missing an intent filter for a deep link?")
                            }
                        }
                    }
                }, { error ->
                    throw(error)
                }
            )
        }

    override fun onBackPressed() {
        if(experienceViewModel?.canGoBack() == true) {
            experienceViewModel!!.pressBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(
            experiencesView
        )

        // TODO: move into a ExperienceFetchViewModel or something coupled with an ExperienceFetchView (or perhaps StandaloneExperienceView)
        roverSdkNetworkService.fetchExperienceTask(ID(experienceId)) { result ->
            if(lifecycle.currentState != Lifecycle.State.DESTROYED) {
                when (result) {
                    is NetworkResult.Success -> {
                        log.v("Experience fetched successfully! living on thread ${Thread.currentThread().id}")
                        experienceViewModel = ExperienceViewModel(result.response, blockViewModelFactory, savedInstanceState?.getParcelable("experienceState"))
                    }
                    is NetworkResult.Error -> {
                        // Snackbar.make(this.main_content, "Opening ${selectedExperience.name}", Snackbar.LENGTH_SHORT).show()
                        log.w("Unable to retrieve experience: ${result.throwable.message}")

                        // TODO not using snackbar for now because we need to decide if including design support lib is sane. also would need to wrap in coordinatorview
                    }
                }
            }
        }.resume()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // TODO: this will be migrated into a surrounding StandaloneExperienceView.
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
}
