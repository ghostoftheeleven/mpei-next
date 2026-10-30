@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package kekmech.ru.lib_bars_client

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDictionaryAddValue
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFMutableDictionaryRef
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFTypeRef
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFBooleanTrue
import platform.CoreFoundation.kCFTypeDictionaryKeyCallBacks
import platform.CoreFoundation.kCFTypeDictionaryValueCallBacks
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Foundation.create
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData
import platform.posix.memcpy

/**
 * Keeps the BARS session in the iOS Keychain (a generic password item), so the cookies are encrypted by the system
 * and are not copied to other devices or to a backup that is restored on another device.
 *
 * The item is readable after the first unlock of the device since the boot, which lets the app refresh data in the
 * background.
 */
class KeychainBarsSessionStore(
    private val service: String = "ru.mpei.hub.bars",
    private val account: String = "session",
) : BarsSessionStore {

    override suspend fun load(): BarsStoredSession? =
        read()?.let { BarsSessionCodec.decode(it.decodeToString()) }

    override suspend fun save(session: BarsStoredSession) {
        write(BarsSessionCodec.encode(session).encodeToByteArray())
    }

    override suspend fun clear() {
        delete()
    }

    private fun read(): ByteArray? {
        val query = baseQuery()
        query.put(kSecReturnData, kCFBooleanTrue)
        query.put(kSecMatchLimit, kSecMatchLimitOne)
        try {
            return memScoped {
                val result = alloc<CFTypeRefVar>()
                val status = SecItemCopyMatching(query.dictionary, result.ptr)
                if (status != errSecSuccess) return@memScoped null
                (CFBridgingRelease(result.value) as? NSData)?.toByteArray()
            }
        } finally {
            query.release()
        }
    }

    private fun write(bytes: ByteArray) {
        // The item is replaced, not updated: there is one session and the old value is of no use
        delete()
        val query = baseQuery()
        query.putObject(kSecValueData, bytes.toNSData())
        query.put(kSecAttrAccessible, kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly)
        try {
            val status = SecItemAdd(query.dictionary, null)
            check(status == errSecSuccess) { "The Keychain did not take the BARS session, status $status" }
        } finally {
            query.release()
        }
    }

    private fun delete() {
        val query = baseQuery()
        try {
            SecItemDelete(query.dictionary)
        } finally {
            query.release()
        }
    }

    private fun baseQuery() = KeychainQuery().apply {
        put(kSecClass, kSecClassGenericPassword)
        putObject(kSecAttrService, service)
        putObject(kSecAttrAccount, account)
    }

    /** A CFDictionary that is built for one call of the Security framework. */
    private class KeychainQuery {

        val dictionary: CFMutableDictionaryRef = checkNotNull(
            CFDictionaryCreateMutable(
                null,
                0,
                kCFTypeDictionaryKeyCallBacks.ptr,
                kCFTypeDictionaryValueCallBacks.ptr,
            ),
        )

        fun put(key: CFTypeRef?, value: CFTypeRef?) {
            CFDictionaryAddValue(dictionary, key, value)
        }

        /** The dictionary retains what it is given, so the reference that [CFBridgingRetain] made can be dropped. */
        fun putObject(key: CFTypeRef?, value: Any) {
            val retained = CFBridgingRetain(value)
            CFDictionaryAddValue(dictionary, key, retained)
            CFRelease(retained)
        }

        fun release() {
            CFRelease(dictionary)
        }
    }

    private fun ByteArray.toNSData(): NSData =
        if (isEmpty()) NSData() else usePinned { NSData.create(bytes = it.addressOf(0), length = size.toULong()) }

    private fun NSData.toByteArray(): ByteArray = ByteArray(length.toInt()).also { bytes ->
        if (bytes.isNotEmpty()) bytes.usePinned { memcpy(it.addressOf(0), this.bytes, length) }
    }
}
