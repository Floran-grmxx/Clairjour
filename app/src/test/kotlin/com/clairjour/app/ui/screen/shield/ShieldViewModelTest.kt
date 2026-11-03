package com.clairjour.app.ui.screen.shield

import com.clairjour.app.data.prefs.SettingsRepository
import com.clairjour.app.shield.ShieldRule
import com.clairjour.app.shield.ShieldServiceState
import com.clairjour.app.shield.ShieldSettings
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ShieldViewModelTest {

    private val repository = mockk<SettingsRepository>(relaxed = true) {
        every { shieldSettingsFlow } returns flowOf(ShieldSettings())
    }
    private lateinit var viewModel: ShieldViewModel

    @Before fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        viewModel = ShieldViewModel(repository)
    }

    @After fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `switching a rule off is immediate`() {
        viewModel.onRuleChange(ShieldRule.REELS_TAB, enabled = false)
        coVerify(exactly = 1) { repository.setShieldRule(ShieldRule.REELS_TAB, false) }
    }

    @Test
    fun `switching all protections off is immediate`() {
        viewModel.onMasterChange(false)
        coVerify(exactly = 1) { repository.setShieldEnabled(false) }
    }

    @Test
    fun `switching on is immediate`() {
        viewModel.onRuleChange(ShieldRule.STORIES, enabled = true)
        coVerify(exactly = 1) { repository.setShieldRule(ShieldRule.STORIES, true) }
    }

    @Test
    fun `a service stopped by the system is not reported as running`() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }
        viewModel.refreshServiceStatus(ShieldServiceState.RUNNING, batteryUnrestricted = false)
        assertEquals(ShieldServiceState.RUNNING, viewModel.state.value.serviceState)

        viewModel.refreshServiceStatus(ShieldServiceState.STOPPED_BY_SYSTEM, batteryUnrestricted = false)
        assertFalse(viewModel.state.value.serviceRunning)
    }

    @Test
    fun `accepting the disclosure is remembered`() {
        viewModel.acceptDisclosure()
        coVerify(exactly = 1) { repository.setShieldDisclosureAccepted() }
    }
}
