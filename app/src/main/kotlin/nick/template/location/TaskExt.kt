package nick.template.location

import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Task
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine

suspend fun <T> Task<T>.await(
    cancellationTokenSource: CancellationTokenSource? = null
): T {
    // fast path
    if (isComplete) {
        val e = exception
        return if (e == null) {
            if (isCanceled) {
                throw CancellationException("Task $this was cancelled normally.")
            } else {
                @Suppress("UNCHECKED_CAST")
                result as T
            }
        } else {
            throw e
        }
    }

    return suspendCancellableCoroutine { cont ->
        addOnCompleteListener {
            val e = it.exception
            if (e == null) {
                @Suppress("UNCHECKED_CAST")
                if (it.isCanceled) cont.cancel() else cont.resume(it.result as T)
            } else {
                cont.resumeWithException(e)
            }
        }

        if (cancellationTokenSource != null) {
            cont.invokeOnCancellation {
                cancellationTokenSource.cancel()
            }
        }
    }
}
