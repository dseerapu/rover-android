package io.rover.rover

import io.rover.rover.platform.DateFormattingInterface
import io.rover.rover.platform.DeviceIdentificationInterface
import io.rover.rover.plugins.data.AuthenticationContext
import io.rover.rover.plugins.data.NetworkClient
import io.rover.rover.plugins.data.WireEncoderInterface
import io.rover.rover.plugins.userexperience.MeasurementService
import io.rover.rover.plugins.userexperience.experience.ViewModelFactoryInterface
import java.util.concurrent.ThreadPoolExecutor

interface DataPluginComponents {
    val authenticationContext: AuthenticationContext

    val networkClient: NetworkClient

    val wireEncoder: WireEncoderInterface

    val ioExecutor: ThreadPoolExecutor

    val deviceIdentification: DeviceIdentificationInterface

    val dateFormatting: DateFormattingInterface

    // TODO: strip out the items from the interface that are not directly exposed to the
    // LiveDataPlugin itself, or that should not be extendable/replacable by the customer.
}

interface UserExperiencePluginComponents {
    val stockViewModelFactory: ViewModelFactoryInterface

    val measurementService: MeasurementService
}
