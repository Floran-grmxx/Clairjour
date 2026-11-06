package com.clairjour.app.data.repository

import com.clairjour.app.data.db.AddictionDao
import com.clairjour.app.data.db.AddictionEntity
import com.clairjour.app.data.db.MilestoneDao
import com.clairjour.app.data.db.MilestoneReachedEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.days

class AddictionRepositoryTest {

    private fun addiction(startDate: Instant) = AddictionEntity(
        id = "a1",
        name = "Tabac",
        type = "TOBACCO",
        startDate = startDate,
        costPerDay = null,
        unitPerDay = null,
        unitLabel = null,
        colorSeed = 0,
        isPrimary = false,
        isActive = true,
        createdAt = Clock.System.now(),
        personalReasons = emptyList()
    )

    @Test
    fun `moving the start date back marks past milestones as already seen`() = runTest {
        val dao: AddictionDao = mockk(relaxed = true)
        val milestoneDao: MilestoneDao = mockk(relaxed = true)
        val inserted = mutableListOf<MilestoneReachedEntity>()
        coEvery { milestoneDao.insert(capture(inserted)) } returns Unit

        AddictionRepository(dao, milestoneDao).update(addiction(Clock.System.now() - 47.days))

        // 1, 3, 7, 14 and 30 days are behind: no celebration cascade on Home.
        assertEquals(listOf(1, 3, 7, 14, 30), inserted.map { it.milestoneDays })
        assertTrue(inserted.all { it.seenByUser })
        coVerify { dao.update(any()) }
    }
}
