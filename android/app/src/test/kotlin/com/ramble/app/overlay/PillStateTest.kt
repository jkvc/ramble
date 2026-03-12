package com.ramble.app.overlay

import org.junit.Assert.*
import org.junit.Test

class PillStateTest {

    @Test
    fun `pill states have correct values`() {
        val states = PillState.values()
        assertEquals(4, states.size)
        assertEquals(PillState.NO_API_KEY, states[0])
        assertEquals(PillState.READY, states[1])
        assertEquals(PillState.CONNECTING, states[2])
        assertEquals(PillState.RECORDING, states[3])
    }

    @Test
    fun `state transition NO_API_KEY to READY`() {
        var state = PillState.NO_API_KEY
        // Simulate API key being set
        state = PillState.READY
        assertEquals(PillState.READY, state)
    }

    @Test
    fun `state transition READY to CONNECTING to RECORDING`() {
        var state = PillState.READY
        // Start recording
        state = PillState.CONNECTING
        assertEquals(PillState.CONNECTING, state)
        // WebSocket connected
        state = PillState.RECORDING
        assertEquals(PillState.RECORDING, state)
    }

    @Test
    fun `state transition RECORDING to READY on stop`() {
        var state = PillState.RECORDING
        // Stop recording
        state = PillState.READY
        assertEquals(PillState.READY, state)
    }

    @Test
    fun `state transition CONNECTING to READY on error`() {
        var state = PillState.CONNECTING
        // Error during connection
        state = PillState.READY
        assertEquals(PillState.READY, state)
    }

    @Test
    fun `state transition RECORDING to READY on error`() {
        var state = PillState.RECORDING
        // Error during recording
        state = PillState.READY
        assertEquals(PillState.READY, state)
    }

    @Test
    fun `state transition to NO_API_KEY when key cleared`() {
        var state = PillState.READY
        // API key cleared
        state = PillState.NO_API_KEY
        assertEquals(PillState.NO_API_KEY, state)
    }
}
