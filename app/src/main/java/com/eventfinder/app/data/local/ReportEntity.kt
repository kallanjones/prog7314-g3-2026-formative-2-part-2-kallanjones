package com.eventfinder.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reports")
data class ReportEntity(
    @PrimaryKey val id: String,
    val reporterId: String,
    val targetType: String, // "event" | "user" | "organizer"
    val targetId: String,
    val reason: String,
    val createdAt: Long,
    val resolved: Boolean = false
)

// Keyed on both ids so one user can block more than one other user.
@Entity(tableName = "blocked_users", primaryKeys = ["userId", "blockedUserId"])
data class BlockedUserEntity(
    val userId: String,
    val blockedUserId: String,
    val createdAt: Long
)