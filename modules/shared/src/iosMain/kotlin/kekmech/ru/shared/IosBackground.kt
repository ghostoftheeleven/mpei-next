package kekmech.ru.shared

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

/**
 * The work of the app when iOS wakes it up in the background (iosApp/iosApp/BackgroundRefresh.swift): it looks for the
 * changes in the schedule and plans the reminders again.
 *
 * @param onDone is called when the work is over: `true` if it went well or there was nothing to do, `false` if the
 * network did not answer
 * @return the function that stops the work (iOS asks for it when the time that it gave is over)
 */
public fun runBackgroundRefresh(onDone: (Boolean) -> Unit): () -> Unit {
    val job = backgroundScope.launch {
        val success = try {
            IosAppGraph.dependencies.backgroundSync.run()
        } catch (e: CancellationException) {
            throw e
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            false
        }
        onDone(success)
    }
    return { job.cancel() }
}
