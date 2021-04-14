package wangdaye.com.geometricweather.common.utils

import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

suspend inline fun <T> suspendCoroutineWithTimeout(
        timeout: Long,
        crossinline block: (CancellableContinuation<T>) -> Unit
) : T? {
    var value : T? = null
    withTimeoutOrNull(timeout) {
        value = suspendCancellableCoroutine(block)
    }
    return value
}