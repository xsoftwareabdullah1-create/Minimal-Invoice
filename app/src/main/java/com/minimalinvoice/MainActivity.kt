package com.minimalinvoice

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minimalinvoice.data.InvoiceDao
import com.minimalinvoice.data.InvoiceDatabase
import com.minimalinvoice.data.InvoiceEntity
import com.minimalinvoice.data.InvoiceLine
import com.minimalinvoice.data.toInvoiceLines
import com.minimalinvoice.data.toJson
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val Accent = Color(0xFF6366F1)
private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dao = InvoiceDatabase.get(this).invoiceDao()
        val preferences = getSharedPreferences("invoice_settings", MODE_PRIVATE)

        setContent {
            val dark = isSystemInDarkTheme()
            val colors = if (dark) {
                darkColorScheme(primary = Accent)
            } else {
                lightColorScheme(primary = Accent)
            }
            MaterialTheme(colorScheme = colors) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MinimalInvoiceApp(dao, preferences)
                }
            }
        }
    }
}

@Composable
private fun MinimalInvoiceApp(
    dao: InvoiceDao,
    preferences: android.content.SharedPreferences
) {
    val invoices by dao.observeAll().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var screen by remember { mutableStateOf("home") }
    var selectedInvoice by remember { mutableStateOf<InvoiceEntity?>(null) }
    var businessName by remember { mutableStateOf(preferences.getString("business", "") ?: "") }
    var currency by remember { mutableStateOf(preferences.getString("currency", "$") ?: "$") }
    var defaultTax by remember { mutableStateOf(preferences.getString("tax", "0") ?: "0") }

    when (screen) {
        "create" -> CreateInvoiceScreen(
            dao = dao,
            defaultTax = defaultTax.toDoubleOrNull() ?: 0.0,
            currency = currency,
            onBack = { screen = "home" },
            onSave = { invoice ->
                scope.launch {
                    dao.insert(invoice)
                    screen = "home"
                }
            }
        )
        "detail" -> {
            val invoice = selectedInvoice
            if (invoice == null) {
                screen = "home"
            } else {
                InvoiceDetailScreen(
                    invoice = invoice,
                    businessName = businessName,
                    currency = currency,
                    onBack = { screen = "home" },
                    onPaidToggle = {
                        scope.launch {
                            dao.setPaid(invoice.id, !invoice.paid)
                            selectedInvoice = invoice.copy(paid = !invoice.paid)
                        }
                    },
                    onDelete = {
                        scope.launch {
                            dao.deleteById(invoice.id)
                            selectedInvoice = null
                            screen = "home"
                        }
                    }
                )
            }
        }
        "settings" -> SettingsScreen(
            businessName = businessName,
            currency = currency,
            tax = defaultTax,
            onBack = { screen = "home" },
            onSave = { name, symbol, taxRate ->
                businessName = name
                currency = symbol
                defaultTax = taxRate
                preferences.edit()
                    .putString("business", name)
                    .putString("currency", symbol)
                    .putString("tax", taxRate)
                    .apply()
                screen = "home"
            }
        )
        else -> InvoiceHomeScreen(
            invoices = invoices,
            currency = currency,
            onCreate = { screen = "create" },
            onSettings = { screen = "settings" },
            onSelect = { selectedInvoice = it; screen = "detail" }
        )
    }
}

@Composable
private fun InvoiceHomeScreen(
    invoices: List<InvoiceEntity>,
    currency: String,
    onCreate: () -> Unit,
    onSettings: () -> Unit,
    onSelect: (InvoiceEntity) -> Unit
) {
    val outstanding = invoices.filterNot { it.paid }.sumOf { it.total }
    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreate,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = Accent,
                shape = RoundedCornerShape(12.dp),
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New invoice") },
                modifier = Modifier.animateContentSize().border(BorderStroke(1.dp, Accent), RoundedCornerShape(12.dp)),
                elevation = FloatingActionButtonDefaults.elevation(0.dp)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 22.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Minimal Invoice",
                        fontSize = 25.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.7).sp
                    )
                    Text(
                        "Your business, at a glance",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                }
                IconButton(onClick = onSettings) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                }
            }

            Text(
                "OUTSTANDING",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "$currency%,.2f".format(outstanding),
                fontSize = 34.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 5.dp, bottom = 22.dp)
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Invoices", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    "${invoices.size} total",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            }

            if (invoices.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("No invoices yet", fontSize = 17.sp, fontWeight = FontWeight.Medium)
                    Text(
                        "Create your first invoice to get started.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(top = 12.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(invoices, key = { it.id }) { invoice ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { onSelect(invoice) },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(0.dp)
                        ) {
                            Row(
                                Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(invoice.clientName, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                                    Text(
                                        "INV-${invoice.invoiceNumber} · Due ${dateFormat.format(Date(invoice.dueAt))}",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("$currency%,.2f".format(invoice.total), fontWeight = FontWeight.SemiBold)
                                    Text(
                                        if (invoice.paid) "Paid" else "Unpaid",
                                        color = if (invoice.paid) Color(0xFF16A34A) else Accent,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateInvoiceScreen(
    dao: InvoiceDao,
    defaultTax: Double,
    currency: String,
    onBack: () -> Unit,
    onSave: (InvoiceEntity) -> Unit
) {
    val context = LocalContext.current
    var invoiceNumber by remember { mutableIntStateOf(0) }
    var clientName by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var taxText by remember { mutableStateOf(defaultTax.toString()) }
    var dueAt by remember { mutableLongStateOf(System.currentTimeMillis() + 7L * 24L * 60L * 60L * 1000L) }
    var lines by remember { mutableStateOf(listOf(InvoiceLine("", 1.0, 0.0))) }

    LaunchedEffect(Unit) { invoiceNumber = dao.nextInvoiceNumber() }

    val subtotal = lines.sumOf { it.amount }
    val taxRate = (taxText.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)
    val total = subtotal * (1.0 + taxRate / 100.0)

    Scaffold(topBar = { SimpleTopBar("New invoice", onBack) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp)) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                item {
                    Text("INVOICE NUMBER", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        if (invoiceNumber == 0) "Preparing…" else "INV-$invoiceNumber",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
                    )
                    MinimalTextField(clientName, { clientName = it }, "Client name")
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Line items", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        TextButton(onClick = { lines = lines + InvoiceLine("", 1.0, 0.0) }) {
                            Text("+ Add item")
                        }
                    }
                }
                items(lines.size) { index ->
                    val item = lines[index]
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 9.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                MinimalTextField(
                                    item.description,
                                    { value ->
                                        lines = lines.toMutableList().also {
                                            it[index] = item.copy(description = value)
                                        }
                                    },
                                    "Description",
                                    Modifier.weight(1f)
                                )
                                if (lines.size > 1) {
                                    IconButton(onClick = {
                                        lines = lines.toMutableList().also { it.removeAt(index) }
                                    }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Remove item")
                                    }
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                MinimalTextField(
                                    item.quantity.toString(),
                                    { value ->
                                        lines = lines.toMutableList().also {
                                            it[index] = item.copy(quantity = value.toDoubleOrNull() ?: 0.0)
                                        }
                                    },
                                    "Qty", Modifier.weight(1f), KeyboardType.Decimal
                                )
                                MinimalTextField(
                                    item.rate.toString(),
                                    { value ->
                                        lines = lines.toMutableList().also {
                                            it[index] = item.copy(rate = value.toDoubleOrNull() ?: 0.0)
                                        }
                                    },
                                    "Rate", Modifier.weight(1f), KeyboardType.Decimal
                                )
                                Column(Modifier.weight(1f).padding(top = 9.dp)) {
                                    Text("AMOUNT", fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("$currency%.2f".format(item.amount), fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MinimalTextField(
                            taxText, { taxText = it }, "Tax %",
                            Modifier.weight(1f), KeyboardType.Decimal
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Due date", fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            OutlinedButton(
                                onClick = {
                                    val calendar = Calendar.getInstance().apply { timeInMillis = dueAt }
                                    DatePickerDialog(
                                        context,
                                        { _, year, month, day ->
                                            dueAt = Calendar.getInstance().apply {
                                                set(year, month, day, 12, 0, 0)
                                                set(Calendar.MILLISECOND, 0)
                                            }.timeInMillis
                                        },
                                        calendar.get(Calendar.YEAR),
                                        calendar.get(Calendar.MONTH),
                                        calendar.get(Calendar.DAY_OF_MONTH)
                                    ).show()
                                },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                Text(dateFormat.format(Date(dueAt)), maxLines = 1)
                            }
                        }
                    }
                    MinimalTextField(notes, { notes = it }, "Notes (optional)")
                    Spacer(Modifier.height(12.dp))
                    InvoiceSummaryRow("Subtotal", subtotal, currency)
                    InvoiceSummaryRow("Tax", total - subtotal, currency)
                    InvoiceSummaryRow("Total", total, currency, true)
                }
            }

            OutlinedButton(
                onClick = {
                    if (invoiceNumber <= 0 || clientName.isBlank() ||
                        lines.none { it.description.isNotBlank() && it.quantity > 0.0 }
                    ) return@OutlinedButton

                    val now = System.currentTimeMillis()
                    onSave(
                        InvoiceEntity(
                            invoiceNumber = invoiceNumber,
                            clientName = clientName.trim(),
                            createdAt = now,
                            dueAt = dueAt,
                            notes = notes.trim(),
                            subtotal = subtotal,
                            taxPercent = taxRate,
                            total = total,
                            paid = false,
                            itemsJson = lines.filter {
                                it.description.isNotBlank() && it.quantity > 0.0
                            }.toJson()
                        )
                    )
                },
                enabled = invoiceNumber > 0 && clientName.isNotBlank() &&
                    lines.any { it.description.isNotBlank() && it.quantity > 0.0 },
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).height(52.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Accent),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Accent)
            ) {
                Text("Save invoice")
            }
        }
    }
}

@Composable
private fun InvoiceDetailScreen(
    invoice: InvoiceEntity,
    businessName: String,
    currency: String,
    onBack: () -> Unit,
    onPaidToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    Scaffold(topBar = { SimpleTopBar("Invoice preview", onBack) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(22.dp)) {
            Text(businessName.ifBlank { "Your business" },
                fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("INVOICE", fontSize = 30.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 20.dp))
            Text("INV-${invoice.invoiceNumber}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(25.dp))
            Text("BILL TO", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(invoice.clientName, fontSize = 18.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 4.dp))
            Text("Due ${dateFormat.format(Date(invoice.dueAt))}",
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            HorizontalDivider(Modifier.padding(vertical = 20.dp))
            Row(Modifier.fillMaxWidth()) {
                Text("Description", Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                Text("Amount", fontWeight = FontWeight.SemiBold)
            }
            invoice.itemsJson.toInvoiceLines().forEach { line ->
                Row(Modifier.fillMaxWidth().padding(top = 14.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(line.description)
                        Text(
                            "${line.quantity} × $currency%.2f".format(line.rate),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text("$currency%.2f".format(line.amount), fontWeight = FontWeight.Medium)
                }
            }
            Spacer(Modifier.height(20.dp))
            InvoiceSummaryRow("Subtotal", invoice.subtotal, currency)
            InvoiceSummaryRow("Tax (${invoice.taxPercent}%)", invoice.total - invoice.subtotal, currency)
            InvoiceSummaryRow("Total", invoice.total, currency, true)
            if (invoice.notes.isNotBlank()) {
                Text("Note: ${invoice.notes}", modifier = Modifier.padding(top = 18.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.weight(1f))
            OutlinedButton(
                onClick = onPaidToggle,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (invoice.paid) "Mark as unpaid" else "Mark as paid")
            }
            OutlinedButton(
                onClick = {
                    val uri = PdfMaker.create(context, invoice, businessName, currency)
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share invoice PDF"))
                },
                modifier = Modifier.fillMaxWidth().padding(top = 9.dp).height(50.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Accent),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Accent)
            ) {
                Text("Share as PDF")
            }
            TextButton(
                onClick = onDelete,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Delete invoice", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    businessName: String,
    currency: String,
    tax: String,
    onBack: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var business by remember { mutableStateOf(businessName) }
    var symbol by remember { mutableStateOf(currency) }
    var taxRate by remember { mutableStateOf(tax) }

    Scaffold(topBar = { SimpleTopBar("Settings", onBack) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(22.dp)) {
            Text("BUSINESS", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            MinimalTextField(business, { business = it }, "Business name")
            Spacer(Modifier.height(18.dp))
            Text("CURRENCY", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            MinimalTextField(symbol, { symbol = it }, "Currency symbol")
            Spacer(Modifier.height(18.dp))
            Text("DEFAULT TAX RATE", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            MinimalTextField(taxRate, { taxRate = it }, "Tax %", keyboard = KeyboardType.Decimal)
            Spacer(Modifier.weight(1f))
            OutlinedButton(
                onClick = {
                    onSave(business.trim(), symbol.ifBlank { "$" }, (taxRate.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0).toString())
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Accent),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Accent)
            ) {
                Text("Save settings")
            }
        }
    }
}

@Composable
private fun SimpleTopBar(title: String, onBack: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
        Text(title, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun MinimalTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    keyboard: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier,
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboard)
    )
}

@Composable
private fun InvoiceSummaryRow(
    label: String,
    amount: Double,
    currency: String,
    emphasized: Boolean = false
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontWeight = if (emphasized) FontWeight.Bold else FontWeight.Normal)
        Text(
            "$currency%,.2f".format(amount),
            fontWeight = if (emphasized) FontWeight.Bold else FontWeight.Normal
        )
    }
}
