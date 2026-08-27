package com.josem.misgastos.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class MovementType { INCOME, EXPENSE }
enum class ReviewStatus { CONFIRMED, NEEDS_REVIEW }

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val block: String,
    val defaultType: MovementType = MovementType.EXPENSE,
    val active: Boolean = true,
)

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val active: Boolean = true,
)

@Entity(
    tableName = "movements",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("categoryId"), Index("accountId"), Index("dateEpochDay")],
)
data class MovementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: MovementType,
    val amountCents: Long,
    val dateEpochDay: Long,
    val categoryId: Long,
    val accountId: Long? = null,
    val notes: String = "",
    val reviewStatus: ReviewStatus = ReviewStatus.CONFIRMED,
    val importKey: String? = null,
)

data class MovementRow(
    val id: Long,
    val type: MovementType,
    val amountCents: Long,
    val dateEpochDay: Long,
    val categoryName: String,
    val block: String,
    val accountName: String?,
    val notes: String,
    val reviewStatus: ReviewStatus,
)

data class MonthlySummary(
    val year: Int,
    val month: Int,
    val incomeCents: Long,
    val expenseCents: Long,
) {
    val balanceCents: Long get() = incomeCents - expenseCents
}
