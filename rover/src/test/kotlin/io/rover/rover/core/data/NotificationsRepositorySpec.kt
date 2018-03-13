package io.rover.rover.core.data

import io.rover.rover.Rover
import io.rover.rover.core.container.Assembler
import io.rover.rover.core.container.Container
import io.rover.rover.core.logging.JvmLogger
import io.rover.rover.core.logging.LogEmitter
import io.rover.rover.core.logging.log
import io.rover.rover.core.streams.Scheduler
import io.rover.rover.core.streams.subscribe
import io.rover.rover.platform.DateFormatting
import io.rover.rover.platform.KeyValueStorage
import io.rover.rover.platform.LocalStorage
import io.rover.rover.core.data.domain.DeviceState
import io.rover.rover.core.data.domain.Notification
import io.rover.rover.core.data.domain.Profile
import io.rover.rover.core.data.domain.PushNotificationAction
import io.rover.rover.core.data.http.NetworkTask
import io.rover.rover.core.events.EventQueueServiceInterface
import io.rover.rover.notifications.NotificationsRepository
import io.rover.rover.notifications.ui.NotificationsRepositoryInterface
import org.amshove.kluent.mock
import org.amshove.kluent.shouldEqual
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import java.util.Date
import java.util.concurrent.Executor


// Important notes about spek.  SpekBodies are executed at setup time, so everything by default is
// shared between all tests.  Or so it seems.

class NotificationsRepositorySpec: Spek({
    given("a notifications repository") {
        // set up a minimal Rover instance just to get logging working.

        fun repo(deviceStateToReturn: NetworkResult<DeviceState>): NotificationsRepository {
            try {
                Rover.initialize(
                    object : Assembler {
                        override fun assemble(container: Container) {
                            container.register(LogEmitter::class.java) { _ ->
                                JvmLogger()
                            }
                        }
                    }
                )
            } catch (e: RuntimeException) {
                // handle the double-initalize warning
                System.out.println(e.message)
            }
            val dataPlugin : DataPluginInterface = object : DataPluginInterface by mock() {
                override fun fetchStateTask(completionHandler: (NetworkResult<DeviceState>) -> Unit): NetworkTask {
                    return object : NetworkTask {
                        override fun cancel() { }

                        override fun resume() {
                            log.v("Yielding $deviceStateToReturn")
                            completionHandler(deviceStateToReturn)
                        }
                    }
                }
            }
            val eventsPlugin : EventQueueServiceInterface = mock()
            val keyValueStorage : MutableMap<String, String?> = mutableMapOf()
            return NotificationsRepository(
                dataPlugin,
                DateFormatting(),
                Executor { command -> command.run() },
                object : Scheduler {
                    override fun execute(runnable: () -> Unit) {
                        runnable()
                    }
                },
                eventsPlugin,
                object : LocalStorage {
                    override fun getKeyValueStorageFor(namedContext: String): KeyValueStorage {
                        return object : KeyValueStorage {
                            override fun get(key: String): String? {
                                return keyValueStorage[key]
                            }

                            override fun set(key: String, value: String?) {
                                keyValueStorage[key] = value
                            }
                        }
                    }
                }
            )
        }


        given("the cloud API returns device state with one event") {
            val repository by memoized { repo(
                NetworkResult.Success(
                    DeviceState(
                        Profile(
                            "donut", hashMapOf()
                        ),
                        emptySet(),
                        listOf(
                            Notification(
                                "41C7F235-7B47-4DC9-9ED8-E1C937F6C6D1",
                                null, null, "body", false, false, true, Date(), Date(), PushNotificationAction.PresentExperience("deadbeef"),
                                null
                            )
                        )
                    )
                )
            ) }

            // how do I test the yielding of the current cached contents on disk?
            // how do I test the merge code?
            // how can I split up the repo into a repo and, say, two "stores"? will that work for our async sync model?

            fun emittedEvents(): List<NotificationsRepositoryInterface.Emission.Update> {
                val updates : MutableList<NotificationsRepositoryInterface.Emission.Update> = mutableListOf<NotificationsRepositoryInterface.Emission.Update>()
                repository.updates().subscribe { emission ->
                    updates.add(emission)
                }
                return updates
            }

            on("initial subscription") {
                val updates = emittedEvents()

                it("should have yielded no updates because no cache is yet available") {
                    updates.size.shouldEqual(0)
                }
            }



                on("after refresh called") {
                    val updates = emittedEvents()
                    repository.refresh()
                    it("should yield the first emission") {
                        updates.first().notifications.first().id.shouldEqual("41C7F235-7B47-4DC9-9ED8-E1C937F6C6D1")
                        updates.size.shouldEqual(1)
                    }

                    it("should yield only one emission") {
                        updates.size.shouldEqual(1)
                    }
                }

        }
    }


})