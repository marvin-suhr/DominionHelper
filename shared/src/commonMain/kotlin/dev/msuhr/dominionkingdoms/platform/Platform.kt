package dev.msuhr.dominionkingdoms.platform

/**
 * Platform abstractions for the shared domain layer.
 * Android actuals live in androidMain, iOS actuals in iosMain.
 */
expect fun nowMillis(): Long

expect fun randomUuid(): String

expect fun logD(tag: String, message: String)

expect fun logI(tag: String, message: String)

expect fun logW(tag: String, message: String)

expect fun logE(tag: String, message: String, error: Throwable? = null)
