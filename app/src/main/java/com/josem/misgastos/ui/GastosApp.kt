package com.josem.misgastos.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.josem.misgastos.HomeState
import com.josem.misgastos.MainViewModel
import com.josem.misgastos.data.CategoryEntity
import com.josem.misgastos.data.MovementRow
import com.josem.misgastos.data.MovementType
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GastosApp(viewModel: MainViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var tab by remember { mutableIntStateOf(0) }
    var showAdd by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                NavigationBarItem(tab == 0, { tab = 0 }, { Icon(Icons.Default.Home, null) }, label = { Text("Inicio") })
                NavigationBarItem(tab == 1, { tab = 1 }, { Icon(Icons.Default.BarChart, null) }, label = { Text("Resumen") })
                NavigationBarItem(tab == 2, { tab = 2 }, { Icon(Icons.Default.Settings, null) }, label = { Text("Ajustes") })
            }
        },
        floatingActionButton = {
            if (tab == 0) FloatingActionButton(onClick = { showAdd = true }) {
                Icon(Icons.Default.Add, contentDescription = "Añadir movimiento")
            }
        },
    ) { padding ->
        when (tab) {
            0 -> HomeScreen(state, Modifier.padding(padding))
            1 -> SummaryScreen(state, Modifier.padding(padding))
            else -> SettingsScreen(state, viewModel, Modifier.padding(padding))
        }
    }

    if (showAdd) AddMovementDialog(
        state = state,
        onDismiss = { showAdd = false },
        onSave = { amount, date, type, categoryId, notes ->
            viewModel.addMovement(amount, date, type, categoryId, null, notes) {
                if (it) showAdd = false
            }
        },
    )
}

@Composable
private fun HomeScreen(state: HomeState, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Spacer(Modifier.height(16.dp))
            Text("Mis gastos", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Tu economía, de un vistazo", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Balance de este mes", style = MaterialTheme.typography.labelLarge)
                    Text(money(state.currentMonth.balanceCents), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Ingresos ${money(state.currentMonth.incomeCents)}", color = Color(0xFF2F6656))
                        Text("Gastos ${money(state.currentMonth.expenseCents)}", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
        item { Text("Últimos movimientos", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold) }
        if (state.movements.isEmpty()) {
            item { Text("Todavía no hay movimientos. Pulsa + para añadir el primero.") }
        } else {
            items(state.movements.take(30), key = { it.id }) { MovementItem(it) }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun MovementItem(row: MovementRow) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.weight(1f)) {
                Text(row.categoryName, fontWeight = FontWeight.SemiBold)
                Text("${row.block} · ${LocalDate.ofEpochDay(row.dateEpochDay).format(DateTimeFormatter.ofPattern("d MMM yyyy"))}", style = MaterialTheme.typography.bodySmall)
            }
            Text(
                (if (row.type == MovementType.INCOME) "+" else "−") + money(row.amountCents),
                color = if (row.type == MovementType.INCOME) Color(0xFF2F6656) else MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun SummaryScreen(state: HomeState, modifier: Modifier = Modifier) {
    LazyColumn(modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Resumen", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }
        item { SummaryCard("Ingresos del mes", state.currentMonth.incomeCents, Color(0xFF2F6656)) }
        item { SummaryCard("Gastos del mes", state.currentMonth.expenseCents, MaterialTheme.colorScheme.error) }
        item { SummaryCard("Balance", state.currentMonth.balanceCents, MaterialTheme.colorScheme.primary) }
        item { Text("Año actual", style = MaterialTheme.typography.titleLarge) }
        item { SummaryCard("Ingresos del año", state.currentYearIncomeCents, Color(0xFF2F6656)) }
        item { SummaryCard("Gastos del año", state.currentYearExpenseCents, MaterialTheme.colorScheme.error) }
        item { SummaryCard("Balance anual", state.currentYearIncomeCents - state.currentYearExpenseCents, MaterialTheme.colorScheme.primary) }
        item { Text("Gastos por categoría", style = MaterialTheme.typography.titleLarge) }
        items(state.expensesByCategory) { (category, cents) -> SummaryCard(category, cents, MaterialTheme.colorScheme.secondary) }
    }
}

@Composable
private fun SummaryCard(label: String, cents: Long, color: Color) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(18.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label)
            Text(money(cents), color = color, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SettingsScreen(state: HomeState, viewModel: MainViewModel, modifier: Modifier = Modifier) {
    var category by remember { mutableStateOf("") }
    var block by remember { mutableStateOf("Casa") }
    var categoryType by remember { mutableStateOf(MovementType.EXPENSE) }
    LazyColumn(modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Categorías", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }
        item { OutlinedTextField(category, { category = it }, label = { Text("Nueva categoría") }, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(block, { block = it }, label = { Text("Bloque") }, modifier = Modifier.fillMaxWidth()) }
        item {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                MovementType.entries.forEachIndexed { index, value ->
                    SegmentedButton(
                        selected = categoryType == value,
                        onClick = { categoryType = value },
                        shape = SegmentedButtonDefaults.itemShape(index, 2),
                    ) { Text(if (value == MovementType.EXPENSE) "Gasto" else "Ingreso") }
                }
            }
        }
        item { Button({ viewModel.addCategory(category, block, categoryType); category = "" }) { Text("Crear categoría") } }
        items(state.categories, key = { it.id }) { Text("${it.block} · ${it.name}") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddMovementDialog(
    state: HomeState,
    onDismiss: () -> Unit,
    onSave: (String, LocalDate, MovementType, Long, String) -> Unit,
) {
    var amount by remember { mutableStateOf("") }
    var dateText by remember { mutableStateOf(LocalDate.now().toString()) }
    var notes by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(MovementType.EXPENSE) }
    var selectedBlock by remember { mutableStateOf<String?>(null) }
    var category by remember { mutableStateOf<CategoryEntity?>(null) }
    val compatibleCategories = state.categories.filter { it.defaultType == type }
    val blocks = compatibleCategories.map { it.block }.distinct()
    val blockCategories = compatibleCategories.filter { it.block == selectedBlock }

    LaunchedEffect(type, state.categories) {
        if (selectedBlock !in blocks) selectedBlock = blocks.firstOrNull()
        if (category?.defaultType != type || category?.block != selectedBlock) category = null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuevo movimiento") },
        text = {
            Column(
                Modifier.heightIn(max = 560.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    MovementType.entries.forEachIndexed { index, value ->
                        SegmentedButton(
                            selected = type == value,
                            onClick = {
                                type = value
                                selectedBlock = null
                                category = null
                            },
                            shape = SegmentedButtonDefaults.itemShape(index, 2),
                        ) { Text(if (value == MovementType.EXPENSE) "Gasto" else "Ingreso") }
                    }
                }
                OutlinedTextField(amount, { amount = it }, label = { Text("Importe") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(dateText, { dateText = it }, label = { Text("Fecha (AAAA-MM-DD)") }, modifier = Modifier.fillMaxWidth())
                if (compatibleCategories.isEmpty()) {
                    Text("Crea primero una categoría desde Ajustes.")
                } else {
                    Text("1. Bloque")
                    blocks.forEach { option ->
                        OutlinedButton(
                            onClick = {
                                selectedBlock = option
                                category = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text((if (selectedBlock == option) "✓ " else "") + option)
                        }
                    }
                    Text("2. Categoría")
                    blockCategories.forEach { option ->
                        OutlinedButton(onClick = { category = option }, modifier = Modifier.fillMaxWidth()) {
                            Text((if (category?.id == option.id) "✓ " else "") + option.name)
                        }
                    }
                }
                OutlinedTextField(notes, { notes = it }, label = { Text("Notas (opcional)") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            val date = runCatching { LocalDate.parse(dateText) }.getOrNull()
            TextButton(
                onClick = { if (date != null) category?.let { onSave(amount, date, type, it.id, notes) } },
                enabled = category != null && date != null,
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

private fun money(cents: Long): String = NumberFormat.getCurrencyInstance(Locale("es", "ES")).format(cents / 100.0)

