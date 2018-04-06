package io.rover.rover.core.permissions

import android.content.Context
import android.content.pm.PackageManager
import android.support.v4.content.ContextCompat
import io.rover.rover.core.logging.log
import io.rover.rover.core.streams.Observable
import io.rover.rover.core.streams.PublishSubject
import io.rover.rover.core.streams.Publisher
import io.rover.rover.core.streams.doOnNext
import io.rover.rover.core.streams.filter
import io.rover.rover.core.streams.filterNulls
import io.rover.rover.core.streams.share

class PermissionsNotifier(
    private val applicationContext: Context
): PermissionsNotifierInterface {
    override fun permissionGranted(permissionId: String) {
        grantedPermissions.onNext(permissionId)
    }

    override fun notifyForPermission(permissionId: String): Publisher<String> {
        return Observable.concat(
            Observable.just(
                if(ContextCompat.checkSelfPermission(
                    applicationContext,
                    permissionId
                ) == PackageManager.PERMISSION_GRANTED) {
                    log.v("Permission $permissionId already granted.")
                    permissionId
                } else null
            ),
            updates.filter { it == permissionId }
        ).filterNulls()
    }

    private val grantedPermissions = PublishSubject<String>()

    private val updates = grantedPermissions.doOnNext { permission ->
        log.v("Permission granted: $permission")
    }.share()
}
