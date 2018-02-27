package io.rover.rover.plugins.userexperience.notificationcentre

import io.rover.rover.core.logging.log
import io.rover.rover.core.streams.Observable
import io.rover.rover.core.streams.PublishSubject
import io.rover.rover.core.streams.Publisher
import io.rover.rover.core.streams.doOnSubscribe
import io.rover.rover.core.streams.map
import io.rover.rover.core.streams.share
import io.rover.rover.plugins.data.domain.Notification

class NotificationCenterListViewModel(
    private val notificationsRepository: NotificationsRepositoryInterface
): NotificationCenterListViewModelInterface {
    override fun events(): Observable<NotificationCenterListViewModelInterface.Event> = epic.doOnSubscribe {
        // Infer from a new subscriber that it's a newly displayed view, and, thus, an
        // automatic refresh should be kicked off.
        requestRefresh()
    }

    override fun notificationClicked(notification: Notification) {
        notificationsRepository.markRead(notification)
    }

    override fun deleteNotification(notification: Notification) {
        notificationsRepository.delete(notification)
    }

    override fun requestRefresh() {
        notificationsRepository.refresh()
    }

    private val actions = PublishSubject<Action>()

    private val epic: Publisher<NotificationCenterListViewModelInterface.Event> =
        Observable.merge(
            actions.share().map { action ->
                when(action) {
                    is Action.ErrorEmerged -> { NotificationCenterListViewModelInterface.Event.DisplayProblemMessage() }
                    // if it turns out I should return Publishers back from the delete/mark-as-read
                    // methods on the Store I should do it here.  If not, then TODO delete the entire actions subject.
                }
            },
            notificationsRepository.updates().map { update ->
                NotificationCenterListViewModelInterface.Event.ListUpdated(update.notifications)
            },
            notificationsRepository.events().map { repositoryEvent ->
                when(repositoryEvent) {
                    is NotificationsRepositoryInterface.Emission.Event.Refreshing -> {
                        NotificationCenterListViewModelInterface.Event.Refreshing(repositoryEvent.refreshing)
                    }
                    is NotificationsRepositoryInterface.Emission.Event.FetchFailure -> {
                        NotificationCenterListViewModelInterface.Event.DisplayProblemMessage()
                    }
                }
            }
        ).share()

    private sealed class Action() {
//        data class NotificationClicked(val notification: Notification): Action()
//        data class DeleteNotification(val notification: Notification): Action()
        class ErrorEmerged: Action()
    }
}
