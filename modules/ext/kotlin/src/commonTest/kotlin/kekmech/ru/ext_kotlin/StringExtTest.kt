package kekmech.ru.ext_kotlin

import kotlin.test.Test
import kotlin.test.assertEquals

internal class StringExtTest {

    @Test
    fun `toBase64 encodes UTF-8 as URL-safe Base64 with padding`() {
        assertEquals("0J_RgNC40LLQtdGCLCDQvNC40YA=", "Привет, мир".toBase64())
    }

    @Test
    fun `fromBase64 reverses toBase64`() {
        val text = "Заметка про лабораторную №3\nвторая строка"
        assertEquals(text, text.toBase64().fromBase64())
    }

    @Test
    fun `fromBase64 reads the values of android util Base64 with line breaks`() {
        val text = "Заметка ".repeat(20)
        val wrapped = text.toBase64().chunked(ANDROID_LINE_LENGTH).joinToString(separator = "\n", postfix = "\n")
        assertEquals(text, wrapped.fromBase64())
    }

    @Test
    fun `fromBase64 reads the values without padding`() {
        assertEquals("Привет, мир", "0J_RgNC40LLQtdGCLCDQvNC40YA".fromBase64())
    }

    @Test
    fun `fromBase64 keeps a string that is not Base64`() {
        assertEquals("not base64 ***", "not base64 ***".fromBase64())
    }

    @Test
    fun `capitalizeSafe makes the first letter upper case`() {
        assertEquals("Пара", "пара".capitalizeSafe())
        assertEquals("", "".capitalizeSafe())
    }

    private companion object {
        const val ANDROID_LINE_LENGTH = 76
    }
}
