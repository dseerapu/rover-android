package io.rover.rover.plugins.userexperience.notificationcentre

import android.content.Context
import android.support.design.widget.CoordinatorLayout
import android.support.design.widget.Snackbar
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import android.widget.TextView
import io.rover.rover.R
import io.rover.rover.core.logging.log
import io.rover.rover.core.streams.doOnError
import io.rover.rover.core.streams.subscribe
import io.rover.rover.platform.whenNotNull
import io.rover.rover.plugins.data.domain.Notification
import io.rover.rover.plugins.userexperience.experience.ViewModelBinding
import io.rover.rover.plugins.userexperience.experience.concerns.BindableView

/**
 * Embed this view to embed a list of previously received push notifications for the user to browse
 * through, as an "Inbox", "Notification Center", or similar.  You can even embed and configure this
 * view directly into your XML layouts.
 *
 * In order to display the list, use the implementation (either the provided one or your own custom
 * version) of [ViewModelFactoryInterface.viewModelForNotificationCenterList] to create an instance
 * of the needed [NotificationCenterListViewModel] view model, and then bind it with [setViewModel].
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

    // UI graph:

    // CoordinatorLayout -> SwipeRefreshLayout -> FrameLayout with [Empty View/List View]


    /**
     * This method will generate a row view.
     *
     * We bundle a basic row view, but if you would like to use your own row view, then you may
     * override this method.
     */
    open fun makeNotificationRowView(): View {
        return TextView(context).apply {
            layoutParams = RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT)
        }
    }

    /**
     * This method will bind a view to a notification.  If you are using a custom layout, you will
     * need to override this.
     */
    open fun bindNotificationToRow(view: View, notification: Notification) {
        (view as TextView).text = notification.title
    }

    override var viewModel: NotificationCenterListViewModelInterface? by ViewModelBinding { viewModel, subscriptionCallback ->
        swipeRefreshLayout.isRefreshing = false

        if(viewModel == null) {
            setUpUnboundState()
        } else {
            viewModel.events()
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
                        log.w("NAV NOT SUPPORTED YET")
                    }
                }
            }, { throw(it) }, { subscription -> subscriptionCallback(subscription) })

            swipeRefreshLayout.setOnRefreshListener {
                viewModel.requestRefresh()
            }

            // won't need to subscribe to any events I don't think.  *possibly* may need to be
            // informed of the deletes from the list.
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
            // TODO, create a viewmodel for the given list item, and bind it to the notification
            // view holder.
            // do I bloody well need a viewmodel for this?!  Where would I handle the clicks? Just the ItemHolder.
            // actually the HOlder is kind of like a viewmodel. it might do here.

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

    init {
        setUpUnboundState()

        this.addView(swipeRefreshLayout)

        itemsView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        itemsView.adapter = adapter

        // TODO: in design mode, put a description!

        // TODO: and will create the gesture observation stuff needed for swipe-to-delete
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
    // specifying the delete action colour



}
