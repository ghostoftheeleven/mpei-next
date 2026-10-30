package kekmech.ru.mpeiapp

import timber.log.Timber
import money.vivid.elmslie.core.logger.strategy.LogStrategy

internal object TimberLogger {

    val E: LogStrategy = invoke(Timber::e)
    val D: LogStrategy = invoke(Timber::d)

    private fun invoke(
        log: (Throwable?, String) -> Unit
    ) = LogStrategy { _, _, message, throwable -> log(throwable, message) }
}
