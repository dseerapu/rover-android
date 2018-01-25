package io.rover.rover.plugins.data.http

import android.os.AsyncTask

class AsyncTaskNetworkTask(
    private val asyncTask: AsyncTask<*, *, *>
) : NetworkTask {
    override fun cancel() {
        asyncTask.cancel(false)
    }

    override fun resume() {
        asyncTask.execute()
    }
}