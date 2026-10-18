package com.josem.misgastos.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface GastosDao {
    @Query("SELECT * FROM categories ORDER BY id")
    suspend fun allCategories(): List<CategoryEntity>

    @Query("SELECT * FROM accounts ORDER BY id")
    suspend fun allAccounts(): List<AccountEntity>

    @Query("SELECT * FROM movements ORDER BY id")
    suspend fun allMovements(): List<MovementEntity>

    @Query("SELECT * FROM categories WHERE active = 1 ORDER BY block, name")
    fun observeCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM accounts WHERE active = 1 ORDER BY name")
    fun observeAccounts(): Flow<List<AccountEntity>>

    @Query(
        """
        SELECT m.id, m.categoryId, m.type, m.amountCents, m.dateEpochDay,
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

    @Query("UPDATE categories SET name = :name, block = :block, defaultType = :type WHERE id = :id")
    suspend fun updateCategory(id: Long, name: String, block: String, type: MovementType)

    @Query("UPDATE categories SET active = 0 WHERE id = :id")
    suspend fun deactivateCategory(id: Long)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAccount(account: AccountEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMovement(movement: MovementEntity): Long

    @Query("UPDATE movements SET type = :type, amountCents = :amountCents, dateEpochDay = :dateEpochDay, categoryId = :categoryId, notes = :notes WHERE id = :id")
    suspend fun updateMovement(
        id: Long,
        type: MovementType,
        amountCents: Long,
        dateEpochDay: Long,
        categoryId: Long,
        notes: String,
    )

    @Query("DELETE FROM movements WHERE id = :id")
    suspend fun deleteMovement(id: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMovements(movements: List<MovementEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun restoreCategories(categories: List<CategoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun restoreAccounts(accounts: List<AccountEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun restoreMovements(movements: List<MovementEntity>)

    @Query("DELETE FROM movements")
    suspend fun deleteAllMovements()

    @Query("DELETE FROM categories")
    suspend fun deleteAllCategories()

    @Query("DELETE FROM accounts")
    suspend fun deleteAllAccounts()

    @Transaction
    suspend fun replaceAll(
        categories: List<CategoryEntity>,
        accounts: List<AccountEntity>,
        movements: List<MovementEntity>,
    ) {
        deleteAllMovements()
        deleteAllCategories()
        deleteAllAccounts()
        restoreCategories(categories)
        restoreAccounts(accounts)
        restoreMovements(movements)
    }

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
