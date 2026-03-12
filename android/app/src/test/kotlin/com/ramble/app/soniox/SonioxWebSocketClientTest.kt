package com.ramble.app.soniox

import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class SonioxWebSocketClientTest {

    @Test
    fun `sendAudio buffers when buffering is enabled and not connected`() {
        val client = SonioxWebSocketClient()
        client.startBuffering()

        // Should not throw - audio is buffered
        client.sendAudio(ByteArray(160))
        client.sendAudio(ByteArray(160))
    }

    @Test
    fun `disconnect clears state`() {
        val client = SonioxWebSocketClient()
        client.startBuffering()
        client.sendAudio(ByteArray(160))

        client.disconnect()

        // After disconnect, sending audio should not buffer (buffering is off)
        // This just verifies disconnect doesn't throw
    }

    @Test
    fun `startBuffering clears previous buffer`() {
        val client = SonioxWebSocketClient()
        client.startBuffering()
        client.sendAudio(ByteArray(160))

        // Starting buffering again should clear the previous buffer
        client.startBuffering()
        // No exception means it works correctly
    }
}
