package com.clairjour.app.shield

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShieldKeepAliveTest {

    @Test
    fun `battery restricted is never done`() {
        assertFalse(ShieldKeepAlive.isDone(batteryUnrestricted = false, autostartAllowed = true, hasAutostartSettings = true))
    }

    @Test
    fun `autostart refused blocks on phones that have the setting`() {
        assertFalse(ShieldKeepAlive.isDone(batteryUnrestricted = true, autostartAllowed = false, hasAutostartSettings = true))
    }

    @Test
    fun `autostart allowed or unknown is done`() {
        assertTrue(ShieldKeepAlive.isDone(batteryUnrestricted = true, autostartAllowed = true, hasAutostartSettings = true))
        assertTrue(ShieldKeepAlive.isDone(batteryUnrestricted = true, autostartAllowed = null, hasAutostartSettings = true))
    }

    @Test
    fun `attention only when wanted and stopped by the system`() {
        assertTrue(ShieldKeepAlive.needsAttention(ShieldServiceState.STOPPED_BY_SYSTEM, shieldWanted = true))
        assertFalse(ShieldKeepAlive.needsAttention(ShieldServiceState.STOPPED_BY_SYSTEM, shieldWanted = false))
        assertFalse(ShieldKeepAlive.needsAttention(ShieldServiceState.RUNNING, shieldWanted = true))
        assertFalse(ShieldKeepAlive.needsAttention(ShieldServiceState.OFF, shieldWanted = true))
    }

    @Test
    fun `phones without autostart setting only need the battery`() {
        assertTrue(ShieldKeepAlive.isDone(batteryUnrestricted = true, autostartAllowed = null, hasAutostartSettings = false))
    }
}
