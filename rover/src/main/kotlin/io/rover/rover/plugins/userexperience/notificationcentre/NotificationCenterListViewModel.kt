package io.rover.rover.plugins.userexperience.notificationcentre

import io.rover.rover.core.logging.log
import io.rover.rover.core.streams.Observable
import io.rover.rover.core.streams.PublishSubject
import io.rover.rover.core.streams.Publisher
import io.rover.rover.core.streams.doOnSubscribe
import io.rover.rover.core.streams.filterNulls
import io.rover.rover.core.streams.flatMap
import io.rover.rover.core.streams.map
import io.rover.rover.core.streams.share
import io.rover.rover.core.streams.shareHotAndReplay
import io.rover.rover.plugins.data.domain.Notification
import io.rover.rover.plugins.events.EventsPluginInterface
import io.rover.rover.plugins.events.domain.Event
import io.rover.rover.plugins.push.NotificationActionRoutingBehaviourInterface

class NotificationCenterListViewModel(
    private val notificationsRepository: NotificationsRepositoryInterface,
    private val eventsPlugin: EventsPluginInterface
): NotificationCenterListViewModelInterface {
    override fun events(): Observable<NotificationCenterListViewModelInterface.Event> = epic.doOnSubscribe {
        // Infer from a new subscriber that it's a newly displayed view, and, thus, an
        // automatic refresh should be kicked off.
        requestRefresh()
    }

    override fun notificationClicked(notification: Notification) {
        actions.onNext(Action.NotificationClicked(notification))
    }

    override fun deleteNotification(notification: Notification) {
        actions.onNext(Action.DeleteNotification(notification))
    }

    override fun requestRefresh() {
        notificationsRepository.refresh()
    }

    private val actions = PublishSubject<Action>()

    private val epic: Publisher<NotificationCenterListViewModelInterface.Event> =
        Observable.merge(
            actions.share().map { action ->
                when(action) {
                    is Action.NotificationClicked -> {
                        // the delete operation is entirely asynchronous, as a side-effect.
                        notificationsRepository.markRead(action.notification)

                        eventsPlugin.trackEvent(
                            Event(
                                "Notification Opened",
                                hashMapOf()
                            )
                        )

                        NotificationCenterListViewModelInterface.Event.Navigate(
                           action.notification.action
                        )
                    }

                    is Action.DeleteNotification -> {
                        notificationsRepository.markRead(action.notification)
                        null
                    }
                }
            }.filterNulls(),
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
        ).shareHotAndReplay(0)

    private sealed class Action() {
        data class NotificationClicked(val notification: Notification): Action()
        data class DeleteNotification(val notification: Notification): Action()
    }
}
