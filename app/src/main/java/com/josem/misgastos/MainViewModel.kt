package com.josem.misgastos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.josem.misgastos.data.AccountEntity
import com.josem.misgastos.data.CategoryEntity
import com.josem.misgastos.data.GastosRepository
import com.josem.misgastos.data.MonthlySummary
import com.josem.misgastos.data.MovementRow
import com.josem.misgastos.data.MovementType
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeState(
    val movements: List<MovementRow> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val accounts: List<AccountEntity> = emptyList(),
    val currentMonth: MonthlySummary = MonthlySummary(LocalDate.now().year, LocalDate.now().monthValue, 0, 0),
    val currentYearIncomeCents: Long = 0,
    val currentYearExpenseCents: Long = 0,
    val expensesByCategory: List<Pair<String, Long>> = emptyList(),
)

class MainViewModel(private val repository: GastosRepository) : ViewModel() {
    val state: StateFlow<HomeState> = combine(
        repository.movements,
        repository.categories,
        repository.accounts,
    ) { movements, categories, accounts ->
        val now = LocalDate.now()
        val monthRows = movements.filter {
            val date = LocalDate.ofEpochDay(it.dateEpochDay)
            date.year == now.year && date.monthValue == now.monthValue
        }
        val yearRows = movements.filter { LocalDate.ofEpochDay(it.dateEpochDay).year == now.year }
        val yearExpenses = yearRows.filter { it.type == MovementType.EXPENSE }
        HomeState(
            movements = movements,
            categories = categories,
            accounts = accounts,
            currentMonth = MonthlySummary(
                year = now.year,
                month = now.monthValue,
                incomeCents = monthRows.filter { it.type == MovementType.INCOME }.sumOf { it.amountCents },
                expenseCents = monthRows.filter { it.type == MovementType.EXPENSE }.sumOf { it.amountCents },
            ),
            currentYearIncomeCents = yearRows.filter { it.type == MovementType.INCOME }.sumOf { it.amountCents },
            currentYearExpenseCents = yearExpenses.sumOf { it.amountCents },
            expensesByCategory = yearExpenses
                .groupBy { it.categoryName }
                .map { (name, rows) -> name to rows.sumOf { it.amountCents } }
                .sortedByDescending { it.second },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeState())

    fun addMovement(
        amountText: String,
        date: LocalDate,
        type: MovementType,
        categoryId: Long,
        accountId: Long?,
        notes: String,
        onResult: (Boolean) -> Unit,
    ) {
        val cents = amountText.replace(',', '.').toBigDecimalOrNull()
            ?.multiply(BigDecimal(100))
            ?.setScale(0, RoundingMode.HALF_UP)
            ?.longValueExact()
        if (cents == null || cents <= 0) return onResult(false)
        viewModelScope.launch {
            repository.addMovement(type, cents, date, categoryId, accountId, notes)
            onResult(true)
        }
    }

    fun addCategory(name: String, block: String, type: MovementType) {
        if (name.isBlank() || block.isBlank()) return
        viewModelScope.launch { repository.addCategory(name, block, type) }
    }

    fun addAccount(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { repository.addAccount(name) }
    }

    class Factory(private val repository: GastosRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = MainViewModel(repository) as T
    }
}
