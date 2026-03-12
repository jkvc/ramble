package com.ramble.app.overlay

import org.junit.Assert.*
import org.junit.Test

class TextInsertionTest {

    @Test
    fun `insert at beginning of text`() {
        val result = TextInsertion.insertTextAtCursor("world", 0, 0, "hello ")
        assertEquals("hello world", result.text)
        assertEquals(6, result.cursorPosition)
    }

    @Test
    fun `insert at middle of text`() {
        val result = TextInsertion.insertTextAtCursor("helloworld", 5, 5, " ")
        assertEquals("hello world", result.text)
        assertEquals(6, result.cursorPosition)
    }

    @Test
    fun `insert at end of text`() {
        val result = TextInsertion.insertTextAtCursor("hello", 5, 5, " world")
        assertEquals("hello world", result.text)
        assertEquals(11, result.cursorPosition)
    }

    @Test
    fun `insert replaces selection`() {
        val result = TextInsertion.insertTextAtCursor("hello world", 6, 11, "there")
        assertEquals("hello there", result.text)
        assertEquals(11, result.cursorPosition)
    }

    @Test
    fun `insert into empty text`() {
        val result = TextInsertion.insertTextAtCursor("", 0, 0, "hello")
        assertEquals("hello", result.text)
        assertEquals(5, result.cursorPosition)
    }

    @Test
    fun `cursor clamped to valid range`() {
        val result = TextInsertion.insertTextAtCursor("abc", 100, 100, "d")
        assertEquals("abcd", result.text)
        assertEquals(4, result.cursorPosition)
    }

    @Test
    fun `remove provisional text`() {
        val provisional = ProvisionalState("world", 6, 5)
        val result = TextInsertion.removeProvisional("hello world", provisional)
        assertEquals("hello ", result.text)
        assertEquals(6, result.cursorPosition)
    }

    @Test
    fun `remove provisional returns unchanged when no provisional`() {
        val result = TextInsertion.removeProvisional("hello", null)
        assertEquals("hello", result.text)
    }

    @Test
    fun `remove provisional gracefully handles mismatch`() {
        val provisional = ProvisionalState("xyz", 6, 3)
        val result = TextInsertion.removeProvisional("hello world", provisional)
        assertEquals("hello world", result.text)
    }

    @Test
    fun `remove provisional gracefully handles out of bounds`() {
        val provisional = ProvisionalState("test", 100, 4)
        val result = TextInsertion.removeProvisional("hello", provisional)
        assertEquals("hello", result.text)
    }

    @Test
    fun `apply final text with no provisional`() {
        val (result, prov) = TextInsertion.applyFinalText(
            "hello ", 6, null, "world"
        )
        assertEquals("hello world", result.text)
        assertEquals(11, result.cursorPosition)
        assertNull(prov)
    }

    @Test
    fun `apply final text replaces provisional`() {
        val provisional = ProvisionalState("worl", 6, 4)
        val (result, prov) = TextInsertion.applyFinalText(
            "hello worl", 10, provisional, "world"
        )
        assertEquals("hello world", result.text)
        assertEquals(11, result.cursorPosition)
        assertNull(prov)
    }

    @Test
    fun `apply provisional text with no existing provisional`() {
        val (result, prov) = TextInsertion.applyProvisionalText(
            "hello ", 6, null, "world"
        )
        assertEquals("hello world", result.text)
        assertEquals(11, result.cursorPosition)
        assertNotNull(prov)
        assertEquals("world", prov!!.text)
        assertEquals(6, prov.position)
    }

    @Test
    fun `apply provisional replaces existing provisional`() {
        val oldProvisional = ProvisionalState("wor", 6, 3)
        val (result, prov) = TextInsertion.applyProvisionalText(
            "hello wor", 9, oldProvisional, "world"
        )
        assertEquals("hello world", result.text)
        assertNotNull(prov)
        assertEquals("world", prov!!.text)
        assertEquals(6, prov.position)
    }

    @Test
    fun `apply empty provisional clears provisional state`() {
        val oldProvisional = ProvisionalState("test", 6, 4)
        val (result, prov) = TextInsertion.applyProvisionalText(
            "hello test", 10, oldProvisional, ""
        )
        assertEquals("hello ", result.text)
        assertNull(prov)
    }

    @Test
    fun `external typing invalidates provisional gracefully`() {
        // User typed something that changed the text, making provisional mismatch
        val oldProvisional = ProvisionalState("world", 6, 5)
        // But someone changed "world" to "worlds" externally
        val (result, prov) = TextInsertion.applyFinalText(
            "hello worlds", 12, oldProvisional, "world"
        )
        // Should fall back gracefully — provisional removal fails, inserts at end
        assertEquals("hello worldsworld", result.text)
        assertNull(prov)
    }
}
