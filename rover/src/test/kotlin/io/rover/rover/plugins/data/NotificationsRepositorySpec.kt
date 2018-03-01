package io.rover.rover.plugins.data

import io.rover.rover.Rover
import io.rover.rover.core.container.Assembler
import io.rover.rover.core.container.Container
import io.rover.rover.core.container.Resolver
import io.rover.rover.core.logging.JvmLogger
import io.rover.rover.core.logging.LogEmitter
import io.rover.rover.core.logging.log
import io.rover.rover.core.streams.Scheduler
import io.rover.rover.core.streams.blockForResult
import io.rover.rover.core.streams.doOnNext
import io.rover.rover.core.streams.doOnSubscribe
import io.rover.rover.core.streams.subscribe
import io.rover.rover.platform.DateFormatting
import io.rover.rover.platform.KeyValueStorage
import io.rover.rover.platform.LocalStorage
import io.rover.rover.plugins.data.domain.DeviceState
import io.rover.rover.plugins.data.domain.Notification
import io.rover.rover.plugins.data.domain.Profile
import io.rover.rover.plugins.data.domain.PushNotificationAction
import io.rover.rover.plugins.data.http.NetworkTask
import io.rover.rover.plugins.events.EventsPluginInterface
import io.rover.rover.plugins.userexperience.notificationcentre.NotificationsRepository
import org.amshove.kluent.mock
import org.amshove.kluent.shouldEqual
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import java.util.Date
import java.util.concurrent.Executor


class NotificationsRepositorySpec: Spek({
    given("a notifications repository") {
        // set up a minimal Rover instance just to get logging working.
        try {
            Rover.initialize(
                object : Assembler {
                    override fun register(container: Container) {
                        container.register(LogEmitter::class.java) { resolver ->
                            JvmLogger()
                        }
                    }
                }
            )
        } catch (e: RuntimeException) {
            // handle the double-initalize warning
            System.out.println(e.message)
        }

        var deviceStateToReturn: NetworkResult<DeviceState>? = null

        val dataPlugin : DataPluginInterface = object : DataPluginInterface by mock() {
            override fun fetchStateTask(completionHandler: (NetworkResult<DeviceState>) -> Unit): NetworkTask {
                return object : NetworkTask {
                    override fun cancel() { }

                    override fun resume() {
                        log.v("Yielding $deviceStateToReturn")
                        completionHandler(deviceStateToReturn ?: throw RuntimeException("Data plugin mock's fetchStateTask() called before deviceStateToReturn set!"))
                    }
                }
            }
        }
        val eventsPlugin : EventsPluginInterface = mock()

        val keyValueStorage : MutableMap<String, String?> = mutableMapOf()

        val notificationsRepository = NotificationsRepository(
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

        on("subscribing") {

            deviceStateToReturn = NetworkResult.Success(
                DeviceState(
                    profile = Profile("donut", hashMapOf()),
                    regions = emptySet(),
                    notifications = listOf(
                        Notification(
                            "41C7F235-7B47-4DC9-9ED8-E1C937F6C6D1",
                            null, "I am title", "I am body", false, false, true, null, Date(), PushNotificationAction.PresentExperience(
                                "deadbeef"
                            )
                        )
                    )
                )
            )

            it("fetches the current notifications and yields them") {
                notificationsRepository.updates().subscribe { emission ->
                    log.v("Got emission $emission")
                    emission.notifications.first().id.shouldEqual("41C7F235-7B47-4DC9-9ED8-E1C937F6C6D1")
                }

                // TODO: START HERE AND IMPLEMENT FIRST SO I CAN USE FIRST().BLOCKFORRESULT

                notificationsRepository.refresh()
            }
        }
    }


})