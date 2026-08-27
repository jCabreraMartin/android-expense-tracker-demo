package com.josem.misgastos.data

import android.content.Context
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

class GastosRepository(private val dao: GastosDao) {
    val movements: Flow<List<MovementRow>> = dao.observeMovements()
    val categories: Flow<List<CategoryEntity>> = dao.observeCategories()
    val accounts: Flow<List<AccountEntity>> = dao.observeAccounts()

    suspend fun addMovement(
        type: MovementType,
        amountCents: Long,
        date: LocalDate,
        categoryId: Long,
        accountId: Long?,
        notes: String,
    ) = dao.insertMovement(
        MovementEntity(
            type = type,
            amountCents = amountCents,
            dateEpochDay = date.toEpochDay(),
            categoryId = categoryId,
            accountId = accountId,
            notes = notes.trim(),
        ),
    )

    suspend fun addCategory(name: String, block: String, type: MovementType) =
        dao.insertCategory(CategoryEntity(name = name.trim(), block = block.trim(), defaultType = type))

    suspend fun addAccount(name: String) = dao.insertAccount(AccountEntity(name = name.trim()))

    suspend fun importInitialData(context: Context) {
        if (dao.movementCount() == 0) InitialDataImporter(context, dao).run()
        dao.getOrCreateCategory("Nómina José", "Casa", MovementType.INCOME)
    }
}

