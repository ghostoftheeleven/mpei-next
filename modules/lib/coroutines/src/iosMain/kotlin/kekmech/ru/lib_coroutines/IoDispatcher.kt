package kekmech.ru.lib_coroutines

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

internal actual fun ioDispatcher(): CoroutineDispatcher = Dispatchers.IO
