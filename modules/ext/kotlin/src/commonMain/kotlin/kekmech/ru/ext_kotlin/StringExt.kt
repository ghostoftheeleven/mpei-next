@file:OptIn(ExperimentalEncodingApi::class)

package kekmech.ru.ext_kotlin

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

private val urlSafeEncoder = Base64.UrlSafe

// The values stored by older versions were encoded by android.util.Base64: line breaks every 76 characters and,
// possibly, no padding. They must stay readable, so the line breaks are dropped and the padding is optional.
private val urlSafeDecoder = Base64.UrlSafe.withPadding(Base64.PaddingOption.PRESENT_OPTIONAL)

public fun String.toBase64(): String = urlSafeEncoder.encode(encodeToByteArray())

public fun String.fromBase64(): String =
    runCatching { urlSafeDecoder.decode(filterNot(Char::isWhitespace)).decodeToString() }
        .getOrDefault(this)

public fun String.capitalizeSafe(): String =
    replaceFirstChar { it.uppercase() }
