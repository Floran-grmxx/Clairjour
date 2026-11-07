package com.clairjour.app.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.Instant

@Entity(tableName = "addictions")
data class AddictionEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "type") val type: String,
    @ColumnInfo(name = "start_date") val startDate: Instant,
    @ColumnInfo(name = "cost_per_day") val costPerDay: Double?,
    @ColumnInfo(name = "unit_per_day") val unitPerDay: Double?,
    @ColumnInfo(name = "unit_label") val unitLabel: String?,
    @ColumnInfo(name = "color_seed") val colorSeed: Int,
    @ColumnInfo(name = "is_primary") val isPrimary: Boolean,
    @ColumnInfo(name = "is_active") val isActive: Boolean,
    @ColumnInfo(name = "created_at") val createdAt: Instant,
    // Personal reasons ("why") written by the user. Empty by default.
    @ColumnInfo(name = "personal_reasons", defaultValue = "[]")
    val personalReasons: List<String> = emptyList()
)
