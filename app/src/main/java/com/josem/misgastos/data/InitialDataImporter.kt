package com.josem.misgastos.data

import android.content.Context
import java.time.LocalDate
import org.json.JSONObject

class InitialDataImporter(
    private val context: Context,
    private val dao: GastosDao,
) {
    suspend fun run() {
        val json = context.assets.open("initial_movements.json")
            .bufferedReader(Charsets.UTF_8)
            .use { JSONObject(it.readText()) }

        val categories = json.getJSONArray("categories")
        repeat(categories.length()) { index ->
            val item = categories.getJSONObject(index)
            dao.getOrCreateCategory(
                name = item.getString("name"),
                block = item.getString("block"),
                type = MovementType.valueOf(item.getString("type")),
            )
        }

        val accounts = json.getJSONArray("accounts")
        repeat(accounts.length()) { index -> dao.getOrCreateAccount(accounts.getString(index)) }

        val movements = json.getJSONArray("movements")
        val rows = buildList {
            repeat(movements.length()) { index ->
                val item = movements.getJSONObject(index)
                val accountName = item.optString("account").takeIf { it.isNotBlank() && it != "null" }
                add(
                    MovementEntity(
                        type = MovementType.valueOf(item.getString("type")),
                        amountCents = item.getLong("amountCents"),
                        dateEpochDay = LocalDate.parse(item.getString("date")).toEpochDay(),
                        categoryId = dao.getOrCreateCategory(
                            name = item.getString("category"),
                            block = item.getString("block"),
                            type = MovementType.valueOf(item.getString("type")),
                        ),
                        accountId = accountName?.let { dao.getOrCreateAccount(it) },
                        notes = item.optString("notes"),
                        reviewStatus = ReviewStatus.CONFIRMED,
                        importKey = item.getString("importKey"),
                    ),
                )
            }
        }
        dao.insertMovements(rows)
    }
}
