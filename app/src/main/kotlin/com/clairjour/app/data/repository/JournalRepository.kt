package com.clairjour.app.data.repository

import com.clairjour.app.data.db.JournalDao
import com.clairjour.app.data.db.JournalEntryEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.util.UUID

class JournalRepository(private val dao: JournalDao) {

    fun observeAll(): Flow<List<JournalEntryEntity>> = dao.observeAll()

    fun observeRecent(limit: Int): Flow<List<JournalEntryEntity>> = dao.observeRecent(limit)

    fun observeByDate(date: LocalDate): Flow<JournalEntryEntity?> = dao.observeByDate(date)

    suspend fun upsert(
        date: LocalDate,
        mood: Int,
        notes: String,
        triggers: List<String>,
        gratitude: String?,
        hadCravings: Boolean
    ) {
        val existing = dao.getByDate(date)
        val now = Clock.System.now()
        val entity = JournalEntryEntity(
            id = existing?.id ?: UUID.randomUUID().toString(),
            date = date,
            mood = mood,
            notes = notes,
            triggers = triggers,
            gratitude = gratitude,
            hadCravings = hadCravings,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now
        )
        dao.upsert(entity)
    }

    suspend fun delete(id: String) = dao.delete(id)

    fun today(): LocalDate =
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
}
