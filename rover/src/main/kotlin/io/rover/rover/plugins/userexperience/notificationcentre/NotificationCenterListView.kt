package io.rover.rover.plugins.userexperience.notificationcentre

import android.content.Context
import android.support.design.widget.CoordinatorLayout
import android.support.design.widget.Snackbar
import android.support.v4.content.ContextCompat
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.RelativeLayout
import android.widget.TextView
import io.rover.rover.R
import io.rover.rover.Rover
import io.rover.rover.core.logging.log
import io.rover.rover.core.streams.androidLifecycleDispose
import io.rover.rover.core.streams.subscribe
import io.rover.rover.platform.whenNotNull
import io.rover.rover.plugins.data.domain.Notification
import io.rover.rover.plugins.userexperience.NotificationOpenInterface
import io.rover.rover.plugins.userexperience.experience.ViewModelBinding
import io.rover.rover.plugins.userexperience.experience.concerns.BindableView

/**
 * Embed this view to embed a list of previously received push notifications for the user to browse
 * through, as an "Inbox", "Notification Center", or similar.  You can even embed and configure this
 * view directly into your XML layouts.
 *
 * In order to display the list, there are several steps.
 *
 * 1. Add [NotificationCenterListView] to your layout, either in XML or progammatically.
 *
 * 2. Set [notificationCenterHost] with your own implementation of [NotificationCenterHost].  This
 * is needed for navigation in response to tapping notifications to work correctly.
 *
 * 3. Then use the implementation of [ViewModelFactoryInterface.viewModelForNotificationCenterList]
 * (either the provided one or your own custom version) to create an instance of the needed
 * [NotificationCenterListViewModel] view model, and then bind it with [setViewModel].
 *
 * You may specify the row item view by either setting xml property TODO or with TODO
 *
 * Also you must override [PushPlugin.notificationCenterIntent] to produce an Intent that will get
 * your app to the state where your usage of NotificationCenterView is being displayed.
 *
 * Note about Android state restoration: Rover SDK views handle state saving & restoration through
 * their view models, so you will need store a Parcelable on behalf of ExperienceView and
 * [NotificationCentreViewModel] (grabbing the state parcelable from the view model at save time and
 * restoring it by passing it to the view model factory at restart time).
 *
 * See [StandaloneNotificationCenterActivity] for an example of how to integrate.
 */
open class NotificationCenterListView : CoordinatorLayout, BindableView<NotificationCenterListViewModelInterface> {
    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle)

    /**
     * Implement this (either directly or with an anonymous class) in your containing view for
     * [NotificationCenterListView].  You will need to provide this before you bind the view model.
     */
    interface NotificationCenterHost {
        /**
         * Provide access to the host Activity containing the notification center list view.
         */
        val provideActivity: AppCompatActivity
    }

    /**
     * You must provide a [NotificationCenterHost] before binding the view model.
     */
    var notificationCenterHost: NotificationCenterHost? = null

    /**
     * This method will generate a row view.
     *
     * We bundle a basic row view, but if you would like to use your own row view, then you may
     * override this method.
     */
    open fun makeNotificationRowView(): View {
        // Override this method to inflate (or programmatically create) your own row view.
        val inflater = LayoutInflater.from(context)
        return inflater.inflate(R.layout.notification_center_default_item, null)
    }

    /**
     * This method will bind a view to a notification.  If you are using a custom layout, you will
     * need to override this.
     */
    open fun bindNotificationToRow(view: View, notification: Notification) {
        (view as RelativeLayout).apply {
            view.findViewById<TextView>(R.id.body_text).text = notification.body
            view.findViewById<TextView>(R.id.title_text).text = notification.title
        }
    }

    override var viewModel: NotificationCenterListViewModelInterface? by ViewModelBinding { viewModel, subscriptionCallback ->
        swipeRefreshLayout.isRefreshing = false

        if(viewModel == null) {
            setUpUnboundState()
        } else {
            viewModel.events()
                .androidLifecycleDispose(this)
                .subscribe({ event ->
                when(event) {
                    is NotificationCenterListViewModelInterface.Event.ListUpdated -> {
                        // update the adapter
                        log.v("List replaced with ${event.notifications.size} notifications")
                        itemsView.visibility = if(event.notifications.isNotEmpty()) View.VISIBLE else View.GONE
                        emptyLayout.visibility = if(event.notifications.isEmpty()) View.VISIBLE else View.GONE
                        currentNotificationsList = event.notifications
                        adapter.notifyDataSetChanged()
                    }
                    is NotificationCenterListViewModelInterface.Event.Refreshing -> {
                        swipeRefreshLayout.isRefreshing = event.refreshing
                    }
                    is NotificationCenterListViewModelInterface.Event.DisplayProblemMessage -> {
                        // TODO: make error resource overridable.
                        Snackbar.make(this, R.string.generic_problem, Snackbar.LENGTH_LONG).show()
                    }
                    is NotificationCenterListViewModelInterface.Event.Navigate -> {
                        log.v("Navigating to action: ${event.notification.action}")
                        val host = (notificationCenterHost ?: throw RuntimeException("Please set notificationCenterHost on NotificationCenterListView.  Otherwise, navigation cannot work."))
                        host.provideActivity.startActivity(
                            notificationOpen.intentForDirectlyOpeningNotification(event.notification)
                        )
                    }
                }
            }, { throw(it) }, { subscription -> subscriptionCallback(subscription) })

            swipeRefreshLayout.setOnRefreshListener {
                viewModel.requestRefresh()
            }
        }
    }

    private val swipeRefreshLayout = SwipeRefreshLayout(
        context
    )

    private val emptySwitcherLayout = FrameLayout(
        context
    ).apply { swipeRefreshLayout.addView(this) }

    private val emptyLayout = TextView(context).apply {
        text = "No notifications yet, lol"
    }.apply {
        gravity = Gravity.CENTER_VERTICAL or Gravity.CENTER_HORIZONTAL
        emptySwitcherLayout.addView(this)
    }

    private val itemsView = RecyclerView(
        context
    ).apply { emptySwitcherLayout.addView(this) }

    // State:
    private var currentNotificationsList: List<Notification>? = null

    private val adapter = object : RecyclerView.Adapter<NotificationViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): NotificationViewHolder {
            return NotificationViewHolder(this@NotificationCenterListView, makeNotificationRowView())
        }

        override fun getItemCount(): Int {
            return currentNotificationsList?.size ?: 0
        }

        override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
            val notification = currentNotificationsList?.get(position)
            notification.whenNotNull { holder.notification = it }
        }
    }

    private fun setUpUnboundState() {
        emptyLayout.visibility = View.GONE
        itemsView.visibility = View.GONE

        swipeRefreshLayout.isRefreshing = false

        swipeRefreshLayout.setOnRefreshListener {
            log.e("Swipe refresh gesture happened before view model bound.")
            swipeRefreshLayout.isRefreshing = false
        }
    }

    private val notificationOpen: NotificationOpenInterface by lazy {
        Rover.sharedInstance.notificationOpen
    }

    init {
        setUpUnboundState()

        this.addView(swipeRefreshLayout)

        itemsView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        itemsView.adapter = adapter

        // TODO: in design mode, put a description!

        // TODO: and will create the gesture observation stuff needed for swipe-to-delete

        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            // no drag and drop desired.
            override fun onMove(recyclerView: RecyclerView?, viewHolder: RecyclerView.ViewHolder?, target: RecyclerView.ViewHolder?): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                log.d("Deleting notification at location")
                val notification = currentNotificationsList?.get(viewHolder.adapterPosition)

                notification.whenNotNull { viewModel?.deleteNotification(it) }
                log.d("... it was $notification")
            }
        }).attachToRecyclerView(itemsView)
    }

    private fun notificationClicked(notification: Notification) {
        viewModel?.notificationClicked(notification)
    }


    private class NotificationViewHolder(
        private val listView: NotificationCenterListView,
        private val view: View
    ): RecyclerView.ViewHolder(view) {

        // TODO: is it worth trying to factor this out into a NotificationRowViewModel?

        var notification: Notification? = null
            set(value) {
                field = value

                // delegate to the possibly-overridden binding method.
                notification.whenNotNull { listView.bindNotificationToRow(view, it) }
            }

        init {
            view.isClickable = true
            view.setOnClickListener {
                notification.whenNotNull { listView.notificationClicked(it) }
            }
        }
    }

    // structure:

    // FrameLayout for switching between the RecyclerView and Empty.

    // Arguments we will support:

    // specifying the empty screen layout
    // specifying the row layout AND binding logic
    // specifying the delete action colour and drawable

}
