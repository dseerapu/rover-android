package io.rover.rover.core.logging

import android.util.Log
import io.rover.rover.Rover

/**
 * A very simple facade wrapped around the Android logger using Kotlin extension methods.
 */
interface LogReceiver {
    fun e(message: String)
    fun w(message: String)
    fun v(message: String)
    fun i(message: String)
    fun d(message: String)
}

interface LogEmitter {
    fun e(logTag: String, message: String)
    fun w(logTag: String, message: String)
    fun v(logTag: String, message: String)
    fun i(logTag: String, message: String)
    fun d(logTag: String, message: String)
}

internal class GlobalStaticLogHolder {
    companion object {
        // This is the only example of a global scope, mutable, allocated-at-runtime value.  This is
        // to avoid the complexity of trying to inject a logger into all and sundry location.
        var globalLogEmitter: LogEmitter? = null
    }
}

val Any.log: LogReceiver
    get() {
        val receiver = GlobalStaticLogHolder.globalLogEmitter ?: throw RuntimeException("Logger has not yet been configured.")

        val logTag = "Rover::${this.javaClass.simpleName}"

        return object : LogReceiver {

            override fun e(message: String) {
                receiver.e(logTag, message)
            }

            override fun w(message: String) {
                receiver.w(logTag, message)
            }

            override fun v(message: String) {
                receiver.v(logTag, message)
            }

            override fun i(message: String) {
                receiver.i(logTag, message)
            }

            override fun d(message: String) {
                receiver.d(logTag, message)
            }
        }
    }

class AndroidLogger : LogEmitter {
    override fun e(logTag: String, message: String) {
        Log.e(logTag, message)
    }

    override fun w(logTag: String, message: String) {
        Log.w(logTag, message)
    }

    override fun v(logTag: String, message: String) {
        Log.v(logTag, message)
    }

    override fun i(logTag: String, message: String) {
        Log.i(logTag, message)
    }

    override fun d(logTag: String, message: String) {
        Log.d(logTag, message)
    }
}

class JvmLogger : LogEmitter {
    override fun e(logTag: String, message: String) {
        System.out.println("E/$logTag $message")
    }

    override fun w(logTag: String, message: String) {
        System.out.println("/$logTag $message")
    }

    override fun v(logTag: String, message: String) {
        System.out.println("V/$logTag $message")
    }

    override fun i(logTag: String, message: String) {
        System.out.println("I/$logTag $message")
    }

    override fun d(logTag: String, message: String) {
        System.out.println("D/$logTag $message")
    }
}