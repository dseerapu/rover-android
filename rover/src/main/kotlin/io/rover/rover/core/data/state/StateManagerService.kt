package io.rover.rover.core.data.state

import android.app.Application
import io.rover.rover.core.data.graphql.GraphQlApiServiceInterface

class StateManagerService(
    private val graphQlApiService: GraphQlApiServiceInterface,
    private val application: Application
): StateManagerServiceInterface {
    override fun enableAutoFetch() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun addStore(stateStore: StateStore) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
