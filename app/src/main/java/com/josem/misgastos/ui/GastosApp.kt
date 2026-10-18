package com.josem.misgastos.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.josem.misgastos.BuildConfig
import com.josem.misgastos.HomeState
import com.josem.misgastos.BlockSummary
import com.josem.misgastos.MainViewModel
import com.josem.misgastos.data.CategoryEntity
import com.josem.misgastos.data.MovementRow
import com.josem.misgastos.data.MovementType
import com.josem.misgastos.sync.SyncStatus
import java.text.NumberFormat
import java.math.BigDecimal
import java.time.LocalDate
import java.time.Instant
import java.time.ZoneOffset
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GastosApp(viewModel: MainViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var tab by remember { mutableIntStateOf(0) }
    var showAdd by remember { mutableStateOf(false) }
    var editingMovement by remember { mutableStateOf<MovementRow?>(null) }
    var deletingMovement by remember { mutableStateOf<MovementRow?>(null) }

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
            0 -> HomeScreen(state, onMovementClick = { editingMovement = it }, modifier = Modifier.padding(padding))
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
    editingMovement?.let { movement ->
        AddMovementDialog(
            state = state,
            initialMovement = movement,
            onDismiss = { editingMovement = null },
            onDelete = {
                deletingMovement = movement
                editingMovement = null
            },
            onSave = { amount, date, type, categoryId, notes ->
                viewModel.updateMovement(movement.id, amount, date, type, categoryId, notes) {
                    if (it) editingMovement = null
                }
            },
        )
    }
    deletingMovement?.let { movement ->
        AlertDialog(
            onDismissRequest = { deletingMovement = null },
            title = { Text("Eliminar movimiento") },
            text = { Text("Se eliminará definitivamente ${movement.categoryName}, por importe de ${money(movement.amountCents)}.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteMovement(movement.id)
                    deletingMovement = null
                }) { Text("Eliminar") }
            },
            dismissButton = { TextButton(onClick = { deletingMovement = null }) { Text("Cancelar") } },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreen(
    state: HomeState,
    onMovementClick: (MovementRow) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedMonth by remember { mutableStateOf<YearMonth?>(null) }
    var monthMenuExpanded by remember { mutableStateOf(false) }
    val availableMonths = state.movements
        .map { YearMonth.from(LocalDate.ofEpochDay(it.dateEpochDay)) }
        .distinct()
    val visibleMovements = selectedMonth?.let { month ->
        state.movements.filter { YearMonth.from(LocalDate.ofEpochDay(it.dateEpochDay)) == month }
    } ?: state.movements.take(30)
    val groupedMovements = visibleMovements
        .groupBy { YearMonth.from(LocalDate.ofEpochDay(it.dateEpochDay)) }
    val expandedMonths = remember { mutableStateMapOf<YearMonth, Boolean>() }
    val latestMonth = groupedMovements.keys.firstOrNull()
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
        item { Text("Movimientos", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold) }
        item {
            ExposedDropdownMenuBox(
                expanded = monthMenuExpanded,
                onExpandedChange = { monthMenuExpanded = it },
            ) {
                OutlinedTextField(
                    value = selectedMonth?.let { monthLabel(it) } ?: "Últimos 30 movimientos",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Filtrar por mes") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = monthMenuExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                )
                ExposedDropdownMenu(
                    expanded = monthMenuExpanded,
                    onDismissRequest = { monthMenuExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("Últimos 30 movimientos") },
                        onClick = {
                            selectedMonth = null
                            monthMenuExpanded = false
                        },
                    )
                    availableMonths.forEach { month ->
                        DropdownMenuItem(
                            text = { Text(monthLabel(month)) },
                            onClick = {
                                selectedMonth = month
                                monthMenuExpanded = false
                            },
                        )
                    }
                }
            }
        }
        if (state.movements.isEmpty()) {
            item { Text("Todavía no hay movimientos. Pulsa + para añadir el primero.") }
        } else {
            groupedMovements.forEach { (month, rows) ->
                    val expanded = expandedMonths[month] ?: (month == latestMonth)
                    item(key = "month-$month") {
                        val monthLabel = month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale("es", "ES")))
                            .replaceFirstChar { it.titlecase(Locale("es", "ES")) }
                        Card(Modifier.fillMaxWidth().clickable { expandedMonths[month] = !expanded }) {
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(monthLabel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Text(if (expanded) "▲" else "▼", color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                    if (expanded) items(rows, key = { it.id }) { MovementItem(it, onClick = { onMovementClick(it) }) }
                }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun MovementItem(row: MovementRow, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        ),
    ) {
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
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    LazyColumn(modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Resumen", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }
        item { SummaryCard("Ingresos del mes", state.currentMonth.incomeCents, Color(0xFF2F6656)) }
        item { SummaryCard("Gastos del mes", state.currentMonth.expenseCents, MaterialTheme.colorScheme.error) }
        item { SummaryCard("Balance", state.currentMonth.balanceCents, MaterialTheme.colorScheme.primary) }
        item { Text("Año actual", style = MaterialTheme.typography.titleLarge) }
        item { SummaryCard("Ingresos del año", state.currentYearIncomeCents, Color(0xFF2F6656)) }
        item { SummaryCard("Gastos del año", state.currentYearExpenseCents, MaterialTheme.colorScheme.error) }
        item { SummaryCard("Balance anual", state.currentYearIncomeCents - state.currentYearExpenseCents, MaterialTheme.colorScheme.primary) }
        item { Text("Balance por bloque · Año actual", style = MaterialTheme.typography.titleLarge) }
        items(state.currentYearByBlock, key = { it.block }) { summary ->
            BlockSummaryCard(summary)
        }
        item { Text("Gastos por categoría", style = MaterialTheme.typography.titleLarge) }
        items(state.expensesByCategory) { (category, cents) ->
            SummaryCard(category, cents, MaterialTheme.colorScheme.secondary) { selectedCategory = category }
        }
    }
    selectedCategory?.let { categoryName ->
        val rows = state.movements.filter {
            it.type == MovementType.EXPENSE && it.categoryName == categoryName &&
                LocalDate.ofEpochDay(it.dateEpochDay).year == LocalDate.now().year
        }
        AlertDialog(
            onDismissRequest = { selectedCategory = null },
            title = { Text(categoryName) },
            text = {
                LazyColumn(Modifier.heightIn(max = 460.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(rows, key = { it.id }) { row ->
                        Column {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(LocalDate.ofEpochDay(row.dateEpochDay).format(DateTimeFormatter.ofPattern("d MMM yyyy")))
                                Text(money(row.amountCents), fontWeight = FontWeight.Bold)
                            }
                            Text(row.block, style = MaterialTheme.typography.bodySmall)
                            if (row.notes.isNotBlank()) Text(row.notes, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { selectedCategory = null }) { Text("Cerrar") } },
        )
    }
}

@Composable
private fun BlockSummaryCard(summary: BlockSummary) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(summary.block, fontWeight = FontWeight.SemiBold)
                Text(
                    money(summary.balanceCents),
                    color = if (summary.balanceCents >= 0) Color(0xFF2F6656) else MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold,
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Ingresos ${money(summary.incomeCents)}", style = MaterialTheme.typography.bodySmall, color = Color(0xFF2F6656))
                Text("Gastos ${money(summary.expenseCents)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun SummaryCard(label: String, cents: Long, color: Color, onClick: (() -> Unit)? = null) {
    val modifier = if (onClick == null) Modifier.fillMaxWidth() else Modifier.fillMaxWidth().clickable(onClick = onClick)
    Card(modifier) {
        Row(Modifier.fillMaxWidth().padding(18.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label)
            Text(money(cents), color = color, fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(state: HomeState, viewModel: MainViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()
    var category by remember { mutableStateOf("") }
    var block by remember { mutableStateOf("Casa") }
    var categoryType by remember { mutableStateOf(MovementType.EXPENSE) }
    var blockMenuExpanded by remember { mutableStateOf(false) }
    var newBlockMode by remember { mutableStateOf(false) }
    val existingBlocks = state.categories.map { it.block }.distinct().sorted()
    var message by remember { mutableStateOf<String?>(null) }
    var pendingRestore by remember { mutableStateOf<String?>(null) }
    var editingCategory by remember { mutableStateOf<CategoryEntity?>(null) }
    var deletingCategory by remember { mutableStateOf<CategoryEntity?>(null) }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) viewModel.createBackup { result ->
            result.onSuccess { backup ->
                runCatching { context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(backup) } ?: error("No se pudo abrir el archivo") }
                    .onSuccess { message = "Copia guardada correctamente" }
                    .onFailure { message = "No se pudo guardar la copia" }
            }.onFailure { message = "No se pudo crear la copia" }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: error("No se pudo abrir el archivo") }
                .onSuccess {
                    pendingRestore = it
                    Toast.makeText(context, "Copia seleccionada", Toast.LENGTH_SHORT).show()
                }
                .onFailure {
                    message = "No se pudo leer la copia seleccionada"
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                }
        } else {
            message = "No se seleccionó ninguna copia"
        }
    }
    LazyColumn(modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Copia de seguridad", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }
        item { Text("Guarda todos tus movimientos y categorías en un archivo que puedes llevar a otro dispositivo.") }
        item {
            val statusText = when (syncStatus) {
                SyncStatus.DISCONNECTED -> if (BuildConfig.DROPBOX_APP_KEY.isBlank()) "Dropbox opcional: configura tu App key para conectar" else "Dropbox no conectado"
                SyncStatus.IDLE -> "Dropbox conectado"
                SyncStatus.SYNCING -> "Sincronizando con Dropbox…"
                SyncStatus.SUCCESS -> "Copia de Dropbox actualizada"
                SyncStatus.ERROR -> "No se pudo sincronizar con Dropbox"
            }
            Text(statusText, color = if (syncStatus == SyncStatus.ERROR) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
        }
        item {
            if (syncStatus == SyncStatus.DISCONNECTED) {
                Button({ viewModel.connectDropbox(context) }, enabled = BuildConfig.DROPBOX_APP_KEY.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("Conectar Dropbox") }
            } else {
                Button(
                    viewModel::syncNow,
                    enabled = syncStatus != SyncStatus.SYNCING,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Sincronizar ahora") }
            }
        }
        item { Button({ exportLauncher.launch("MisGastos-copia.json") }, modifier = Modifier.fillMaxWidth()) { Text("Exportar copia") } }
        item { OutlinedButton({ importLauncher.launch("*/*") }, modifier = Modifier.fillMaxWidth()) { Text("Importar copia") } }
        message?.let { item { Text(it, color = MaterialTheme.colorScheme.primary) } }
        item { Text("Categorías", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }
        item { OutlinedTextField(category, { category = it }, label = { Text("Nueva categoría") }, modifier = Modifier.fillMaxWidth()) }
        item {
            if (newBlockMode) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedTextField(block, { block = it }, label = { Text("Nombre del nuevo bloque") }, modifier = Modifier.fillMaxWidth())
                    TextButton(onClick = {
                        newBlockMode = false
                        block = existingBlocks.firstOrNull() ?: "Casa"
                    }) { Text("Elegir un bloque existente") }
                }
            } else {
                ExposedDropdownMenuBox(
                    expanded = blockMenuExpanded,
                    onExpandedChange = { blockMenuExpanded = it },
                ) {
                    OutlinedTextField(
                        value = block,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Bloque") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = blockMenuExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                    )
                    ExposedDropdownMenu(
                        expanded = blockMenuExpanded,
                        onDismissRequest = { blockMenuExpanded = false },
                    ) {
                        existingBlocks.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    block = option
                                    blockMenuExpanded = false
                                },
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Añadir nuevo bloque…") },
                            onClick = {
                                block = ""
                                newBlockMode = true
                                blockMenuExpanded = false
                            },
                        )
                    }
                }
            }
        }
        item {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                MovementType.entries.forEachIndexed { index, value ->
                    SegmentedButton(
                        selected = categoryType == value,
                        onClick = { categoryType = value },
                        shape = SegmentedButtonDefaults.itemShape(index, 2),
                        colors = movementTypeButtonColors(),
                    ) { Text(if (value == MovementType.EXPENSE) "Gasto" else "Ingreso") }
                }
            }
        }
        item { Button({ viewModel.addCategory(category, block, categoryType); category = "" }) { Text("Crear categoría") } }
        items(state.categories, key = { it.id }) { item ->
            Card(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("${item.block} · ${item.name}", modifier = Modifier.weight(1f))
                    TextButton(onClick = { editingCategory = item }) { Text("Editar") }
                    TextButton(onClick = { deletingCategory = item }) { Text("Eliminar") }
                }
            }
        }
    }
    if (pendingRestore != null) AlertDialog(
        onDismissRequest = { pendingRestore = null },
        title = { Text("Restaurar copia") },
        text = { Text("Se sustituirán todos los datos actuales de este dispositivo por los de la copia. Esta acción no se puede deshacer.") },
        confirmButton = { TextButton(onClick = {
            val backup = pendingRestore ?: return@TextButton
            pendingRestore = null
            viewModel.restoreBackup(backup) { result ->
                message = if (result.isSuccess) "Copia restaurada correctamente" else "El archivo no es una copia válida"
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
        }) { Text("Restaurar") } },
        dismissButton = { TextButton(onClick = { pendingRestore = null }) { Text("Cancelar") } },
    )
    editingCategory?.let { item ->
        var editName by remember(item.id) { mutableStateOf(item.name) }
        var editBlock by remember(item.id) { mutableStateOf(item.block) }
        var editType by remember(item.id) { mutableStateOf(item.defaultType) }
        AlertDialog(
            onDismissRequest = { editingCategory = null },
            title = { Text("Editar categoría") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(editName, { editName = it }, label = { Text("Nombre") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(editBlock, { editBlock = it }, label = { Text("Bloque") }, modifier = Modifier.fillMaxWidth())
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        MovementType.entries.forEachIndexed { index, value ->
                            SegmentedButton(
                                selected = editType == value,
                                onClick = { editType = value },
                                shape = SegmentedButtonDefaults.itemShape(index, 2),
                                colors = movementTypeButtonColors(),
                            ) { Text(if (value == MovementType.EXPENSE) "Gasto" else "Ingreso") }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.updateCategory(item.id, editName, editBlock, editType)
                        editingCategory = null
                    },
                    enabled = editName.isNotBlank() && editBlock.isNotBlank(),
                ) { Text("Guardar") }
            },
            dismissButton = { TextButton(onClick = { editingCategory = null }) { Text("Cancelar") } },
        )
    }
    deletingCategory?.let { item ->
        AlertDialog(
            onDismissRequest = { deletingCategory = null },
            title = { Text("Eliminar categoría") },
            text = { Text("${item.name} dejará de aparecer al crear movimientos. Los movimientos anteriores se conservarán.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteCategory(item.id)
                    deletingCategory = null
                }) { Text("Eliminar") }
            },
            dismissButton = { TextButton(onClick = { deletingCategory = null }) { Text("Cancelar") } },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddMovementDialog(
    state: HomeState,
    initialMovement: MovementRow? = null,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)? = null,
    onSave: (String, LocalDate, MovementType, Long, String) -> Unit,
) {
    var amount by remember(initialMovement?.id) {
        mutableStateOf(initialMovement?.let { BigDecimal(it.amountCents).movePointLeft(2).stripTrailingZeros().toPlainString().replace('.', ',') } ?: "")
    }
    var date by remember(initialMovement?.id) { mutableStateOf(initialMovement?.let { LocalDate.ofEpochDay(it.dateEpochDay) } ?: LocalDate.now()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var notes by remember(initialMovement?.id) { mutableStateOf(initialMovement?.notes ?: "") }
    var type by remember(initialMovement?.id) { mutableStateOf(initialMovement?.type ?: MovementType.EXPENSE) }
    var selectedBlock by remember(initialMovement?.id) { mutableStateOf(initialMovement?.block) }
    var category by remember(initialMovement?.id, state.categories) {
        mutableStateOf(state.categories.firstOrNull { it.id == initialMovement?.categoryId })
    }
    val compatibleCategories = state.categories.filter { it.defaultType == type }
    val blocks = compatibleCategories.map { it.block }.distinct()
    val blockCategories = compatibleCategories.filter { it.block == selectedBlock }

    LaunchedEffect(type, state.categories) {
        if (selectedBlock !in blocks) selectedBlock = blocks.firstOrNull()
        if (category?.defaultType != type || category?.block != selectedBlock) category = null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialMovement == null) "Nuevo movimiento" else "Editar movimiento") },
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
                            colors = movementTypeButtonColors(),
                        ) { Text(if (value == MovementType.EXPENSE) "Gasto" else "Ingreso") }
                    }
                }
                OutlinedTextField(
                    amount,
                    { value -> if (value.all { it.isDigit() || it == ',' || it == '.' }) amount = value },
                    label = { Text("Importe") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Fecha: ${date.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("es", "ES")))}")
                }
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
            TextButton(
                onClick = { category?.let { onSave(amount, date, type, it.id, notes) } },
                enabled = category != null,
            ) { Text(if (initialMovement == null) "Guardar" else "Guardar cambios") }
        },
        dismissButton = {
            Row {
                if (onDelete != null) TextButton(onClick = onDelete) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
                TextButton(onClick = onDismiss) { Text("Cancelar") }
            }
        },
    )
    if (showDatePicker) {
        val pickerState = androidx.compose.material3.rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        date = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") } },
        ) { DatePicker(state = pickerState) }
    }
}

@Composable
private fun movementTypeButtonColors() = SegmentedButtonDefaults.colors(
    activeContainerColor = MaterialTheme.colorScheme.primary,
    activeContentColor = MaterialTheme.colorScheme.onPrimary,
    inactiveContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
)

private fun monthLabel(month: YearMonth): String = month
    .format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale("es", "ES")))
    .replaceFirstChar { it.titlecase(Locale("es", "ES")) }

private fun money(cents: Long): String = NumberFormat.getCurrencyInstance(Locale("es", "ES")).format(cents / 100.0)
