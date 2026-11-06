package com.clairjour.app.data.repository

import com.clairjour.app.data.db.AddictionDao
import com.clairjour.app.data.db.AddictionEntity
import com.clairjour.app.data.db.MilestoneDao
import com.clairjour.app.data.db.MilestoneReachedEntity
import com.clairjour.app.domain.AddictionType
import com.clairjour.app.domain.Milestones
import com.clairjour.app.domain.Streak
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.util.UUID

class AddictionRepository(
    private val dao: AddictionDao,
    private val milestoneDao: MilestoneDao? = null
) {

    fun observeActive(): Flow<List<AddictionEntity>> = dao.observeActive()
    fun observePrimary(): Flow<AddictionEntity?> = dao.observePrimary()
    fun observeById(id: String): Flow<AddictionEntity?> = dao.observeById(id)

    suspend fun getById(id: String): AddictionEntity? = dao.getById(id)

    suspend fun create(
        name: String,
        type: AddictionType,
        startDate: Instant,
        costPerDay: Double?,
        unitPerDay: Double?,
        unitLabel: String?,
        isPrimary: Boolean,
        personalReasons: List<String> = emptyList()
    ): String {
        val id = UUID.randomUUID().toString()
        val now = Clock.System.now()
        if (isPrimary) dao.clearPrimary()
        dao.insert(
            AddictionEntity(
                id = id,
                name = name.ifBlank { "?" },
                type = type.name,
                startDate = startDate,
                costPerDay = costPerDay,
                unitPerDay = unitPerDay,
                unitLabel = unitLabel,
                colorSeed = type.ordinal * 37,
                isPrimary = isPrimary,
                isActive = true,
                createdAt = now,
                personalReasons = personalReasons
            )
        )
        // The user may pre-date their sobriety: no celebration cascade on the first Home render.
        markPastMilestonesSeen(id, startDate, now)
        return id
    }

    suspend fun update(entity: AddictionEntity) {
        if (entity.isPrimary) dao.clearPrimary()
        dao.update(entity)
        // Moving the start date back must not celebrate milestones that are already behind.
        markPastMilestonesSeen(entity.id, entity.startDate, Clock.System.now())
    }

    /**
     * Inserts milestones already reached at [now] as seen. Existing rows are kept
     * (insert ignores conflicts), so milestones the user has not seen yet stay unseen.
     */
    private suspend fun markPastMilestonesSeen(addictionId: String, startDate: Instant, now: Instant) {
        if (milestoneDao == null) return
        val elapsedDays = Streak.daysSince(startDate, now)
        Milestones.reached(elapsedDays).forEach { milestone ->
            milestoneDao.insert(
                MilestoneReachedEntity(
                    id = "${addictionId}_${milestone.days}",
                    addictionId = addictionId,
                    milestoneDays = milestone.days,
                    reachedAt = now,
                    seenByUser = true
                )
            )
        }
    }

    suspend fun markPrimary(id: String) {
        dao.clearPrimary()
        dao.markPrimary(id)
    }

    suspend fun softDelete(id: String) = dao.softDelete(id)

    suspend fun resetStart(id: String, newStart: Instant) = dao.resetStart(id, newStart)
}
