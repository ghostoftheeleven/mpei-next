package kekmech.ru.lib_coroutines

import kotlinx.coroutines.CoroutineDispatcher

// Dispatchers.IO is a member of Dispatchers on the JVM, but on Kotlin/Native it is an extension (kotlinx.coroutines.IO)
internal expect fun ioDispatcher(): CoroutineDispatcher
