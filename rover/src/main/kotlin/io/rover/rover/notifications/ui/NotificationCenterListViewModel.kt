package io.rover.rover.notifications.ui

import io.rover.rover.core.streams.*
import io.rover.rover.notifications.domain.Notification
import java.util.Date

class NotificationCenterListViewModel(
    private val notificationsRepository: NotificationsRepositoryInterface
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

    // State: stable IDs mapping.  Ensure that we have a 100% consistent stable ID for the lifetime
    // of the view model (which will sufficiently match the lifetime of the recyclerview that
    // requires the stableids).
    private var highestStableId = 0
    private val stableIds: MutableMap<String, Int> = mutableMapOf()

    private val actions = PublishSubject<Action>()

    private val epic: Publisher<NotificationCenterListViewModelInterface.Event> =
        Observable.merge(
            actions.share().map { action ->
                when(action) {
                    is Action.NotificationClicked -> {
                        // the delete operation is entirely asynchronous, as a side-effect.
                        notificationsRepository.markRead(action.notification)

                        NotificationCenterListViewModelInterface.Event.Navigate(
                            action.notification
                        )
                    }

                    is Action.DeleteNotification -> {
                        notificationsRepository.delete(action.notification)
                        null
                    }
                }
            }.filterNulls(),
            notificationsRepository.updates().map { update ->
                update
                    .notifications
                    .filter { !it.isDeleted }
                    .filter { it.expiresAt?.after(Date()) ?: true }
            }.doOnNext { notificationsReadyForDisplay ->
                // side-effect, update the stable ids list map:
                updateStableIds(notificationsReadyForDisplay)
            }.map { notificationsReadyForDisplay ->
                NotificationCenterListViewModelInterface.Event.ListUpdated(
                    notificationsReadyForDisplay,
                    stableIds
                )
            }.doOnNext { updateStableIds(it.notifications) },
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

    private fun updateStableIds(notifications: List<Notification>) {
        notifications.forEach { notification ->
            if(!stableIds.containsKey(notification.id)) {
                stableIds[notification.id] = ++highestStableId
            }
        }
    }

    private sealed class Action {
        data class NotificationClicked(val notification: Notification): Action()
        data class DeleteNotification(val notification: Notification): Action()
    }
}
