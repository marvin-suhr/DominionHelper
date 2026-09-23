package dev.msuhr.dominionkingdoms.platform

import android.util.Log
import java.util.UUID

actual fun nowMillis(): Long = System.currentTimeMillis()

actual fun randomUuid(): String = UUID.randomUUID().toString()

actual fun logD(tag: String, message: String) { Log.d(tag, message) }

actual fun logI(tag: String, message: String) { Log.i(tag, message) }

actual fun logW(tag: String, message: String) { Log.w(tag, message) }

actual fun logE(tag: String, message: String, error: Throwable?) {
    if (error != null) Log.e(tag, message, error) else Log.e(tag, message)
}
