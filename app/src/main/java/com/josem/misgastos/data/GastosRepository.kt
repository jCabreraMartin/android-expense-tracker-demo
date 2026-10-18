package com.josem.misgastos.data

import android.content.Context
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject

class GastosRepository(private val dao: GastosDao) {
    var onDataChanged: (() -> Unit)? = null
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
    ) {
        dao.insertMovement(MovementEntity(
            type = type,
            amountCents = amountCents,
            dateEpochDay = date.toEpochDay(),
            categoryId = categoryId,
            accountId = accountId,
            notes = notes.trim(),
        ))
        onDataChanged?.invoke()
    }

    suspend fun updateMovement(
        id: Long,
        type: MovementType,
        amountCents: Long,
        date: LocalDate,
        categoryId: Long,
        notes: String,
    ) {
        dao.updateMovement(id, type, amountCents, date.toEpochDay(), categoryId, notes.trim())
        onDataChanged?.invoke()
    }

    suspend fun deleteMovement(id: Long) {
        dao.deleteMovement(id)
        onDataChanged?.invoke()
    }

    suspend fun addCategory(name: String, block: String, type: MovementType) {
        dao.insertCategory(CategoryEntity(name = name.trim(), block = block.trim(), defaultType = type))
        onDataChanged?.invoke()
    }

    suspend fun updateCategory(id: Long, name: String, block: String, type: MovementType) {
        dao.updateCategory(id, name.trim(), block.trim(), type)
        onDataChanged?.invoke()
    }

    suspend fun deleteCategory(id: Long) {
        dao.deactivateCategory(id)
        onDataChanged?.invoke()
    }

    suspend fun addAccount(name: String) = dao.insertAccount(AccountEntity(name = name.trim()))

    suspend fun importInitialData(context: Context) {
        if (dao.movementCount() == 0) InitialDataImporter(context, dao).run()
        dao.getOrCreateCategory("Salary", "Casa", MovementType.INCOME)
    }

    suspend fun createBackup(): String = JSONObject().apply {
        put("format", "mis-gastos-backup")
        put("version", 1)
        put("categories", JSONArray().apply {
            dao.allCategories().forEach { category -> put(JSONObject().apply {
                put("id", category.id); put("name", category.name); put("block", category.block)
                put("defaultType", category.defaultType.name); put("active", category.active)
            }) }
        })
        put("accounts", JSONArray().apply {
            dao.allAccounts().forEach { account -> put(JSONObject().apply {
                put("id", account.id); put("name", account.name); put("active", account.active)
            }) }
        })
        put("movements", JSONArray().apply {
            dao.allMovements().forEach { movement -> put(JSONObject().apply {
                put("id", movement.id); put("type", movement.type.name); put("amountCents", movement.amountCents)
                put("dateEpochDay", movement.dateEpochDay); put("categoryId", movement.categoryId)
                put("accountId", movement.accountId ?: JSONObject.NULL); put("notes", movement.notes)
                put("reviewStatus", movement.reviewStatus.name); put("importKey", movement.importKey ?: JSONObject.NULL)
            }) }
        })
    }.toString(2)

    suspend fun restoreBackup(text: String, markAsChanged: Boolean = true) {
        val root = JSONObject(text)
        require(root.getString("format") == "mis-gastos-backup" && root.getInt("version") == 1) {
            "Formato de copia no compatible"
        }
        val categoriesJson = root.getJSONArray("categories")
        val accountsJson = root.getJSONArray("accounts")
        val movementsJson = root.getJSONArray("movements")
        val categories = (0 until categoriesJson.length()).map { index -> categoriesJson.getJSONObject(index).run {
            CategoryEntity(getLong("id"), getString("name"), getString("block"), MovementType.valueOf(getString("defaultType")), getBoolean("active"))
        } }
        val accounts = (0 until accountsJson.length()).map { index -> accountsJson.getJSONObject(index).run {
            AccountEntity(getLong("id"), getString("name"), getBoolean("active"))
        } }
        val movements = (0 until movementsJson.length()).map { index -> movementsJson.getJSONObject(index).run {
            MovementEntity(
                id = getLong("id"), type = MovementType.valueOf(getString("type")), amountCents = getLong("amountCents"),
                dateEpochDay = getLong("dateEpochDay"), categoryId = getLong("categoryId"),
                accountId = if (isNull("accountId")) null else getLong("accountId"), notes = getString("notes"),
                reviewStatus = ReviewStatus.valueOf(getString("reviewStatus")),
                importKey = if (isNull("importKey")) null else getString("importKey"),
            )
        } }
        require(categories.isNotEmpty()) { "La copia no contiene categorías" }
        val categoryIds = categories.mapTo(mutableSetOf()) { it.id }
        require(movements.all { it.categoryId in categoryIds }) { "La copia contiene datos incompletos" }
        dao.replaceAll(categories, accounts, movements)
        if (markAsChanged) onDataChanged?.invoke()
    }
}
