package io.rover.rover.plugins.userexperience.notificationcentre

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet

/**
 * Displays the list of
 *
 * Note that developers should typically use [NotificationCenterView].  That class handles
 * asynchronously refreshing the notifications list, optionally handling pull-to-refresh, and
 * displaying the empty-status screen.
 */
class NotificationCenterListView : RecyclerView {
    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle)

    // Arguments we will support:

    // specifying the
}
