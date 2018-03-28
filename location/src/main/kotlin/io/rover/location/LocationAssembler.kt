package io.rover.location

import io.rover.rover.core.container.Assembler
import io.rover.rover.core.container.Container
import io.rover.rover.core.container.Scope

class LocationAssembler : Assembler {
    override fun assemble(container: Container) {
        container.register(Scope.Singleton, )
    }
}