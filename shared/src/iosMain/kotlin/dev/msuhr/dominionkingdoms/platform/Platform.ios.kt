package dev.msuhr.dominionkingdoms.platform

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDate
import platform.Foundation.NSLog
import platform.Foundation.NSUUID
import platform.Foundation.timeIntervalSince1970

@OptIn(ExperimentalForeignApi::class)
actual fun nowMillis(): Long = (NSDate().timeIntervalSince1970 * 1000).toLong()

actual fun randomUuid(): String = NSUUID().UUIDString

actual fun logD(tag: String, message: String) { NSLog("D/[$tag] %@", message) }

actual fun logI(tag: String, message: String) { NSLog("I/[$tag] %@", message) }

actual fun logW(tag: String, message: String) { NSLog("W/[$tag] %@", message) }

actual fun logE(tag: String, message: String, error: Throwable?) {
    if (error != null) NSLog("E/[$tag] %@ - %@", message, error.message ?: "unknown")
    else NSLog("E/[$tag] %@", message)
}
