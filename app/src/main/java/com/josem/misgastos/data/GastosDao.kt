package com.josem.misgastos.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface GastosDao {
    @Query("SELECT * FROM categories WHERE active = 1 ORDER BY block, name")
    fun observeCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM accounts WHERE active = 1 ORDER BY name")
    fun observeAccounts(): Flow<List<AccountEntity>>

    @Query(
        """
        SELECT m.id, m.type, m.amountCents, m.dateEpochDay,
               c.name AS categoryName, c.block AS block,
               a.name AS accountName, m.notes, m.reviewStatus
        FROM movements m
        JOIN categories c ON c.id = m.categoryId
        LEFT JOIN accounts a ON a.id = m.accountId
        ORDER BY m.dateEpochDay DESC, m.id DESC
        """,
    )
    fun observeMovements(): Flow<List<MovementRow>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAccount(account: AccountEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMovement(movement: MovementEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMovements(movements: List<MovementEntity>)

    @Query("SELECT COUNT(*) FROM movements")
    suspend fun movementCount(): Int

    @Query("SELECT id FROM categories WHERE name = :name AND block = :block LIMIT 1")
    suspend fun categoryId(name: String, block: String): Long?

    @Query("SELECT id FROM accounts WHERE name = :name LIMIT 1")
    suspend fun accountId(name: String): Long?

    @Transaction
    suspend fun getOrCreateCategory(name: String, block: String, type: MovementType): Long =
        categoryId(name, block) ?: insertCategory(CategoryEntity(name = name, block = block, defaultType = type))

    @Transaction
    suspend fun getOrCreateAccount(name: String): Long =
        accountId(name) ?: insertAccount(AccountEntity(name = name))
}
