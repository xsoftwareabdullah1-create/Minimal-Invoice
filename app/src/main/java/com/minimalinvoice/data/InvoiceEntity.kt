package com.minimalinvoice.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import org.json.JSONArray
import org.json.JSONObject

data class InvoiceLine(
    val description: String,
    val quantity: Double,
    val rate: Double
) {
    val amount: Double get() = quantity * rate
}

@Entity(
    tableName = "invoices",
    indices = [Index(value = ["invoiceNumber"], unique = true)]
)
data class InvoiceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val invoiceNumber: Int,
    val clientName: String,
    val createdAt: Long,
    val dueAt: Long,
    val notes: String,
    val subtotal: Double,
    val taxPercent: Double,
    val total: Double,
    val paid: Boolean,
    val itemsJson: String
)

fun List<InvoiceLine>.toJson(): String {
    val array = JSONArray()
    forEach { line ->
        array.put(
            JSONObject()
                .put("description", line.description)
                .put("quantity", line.quantity)
                .put("rate", line.rate)
        )
    }
    return array.toString()
}

fun String.toInvoiceLines(): List<InvoiceLine> = try {
    val array = JSONArray(this)
    (0 until array.length()).map { index ->
        val item = array.getJSONObject(index)
        InvoiceLine(
            description = item.optString("description"),
            quantity = item.optDouble("quantity", 1.0),
            rate = item.optDouble("rate", 0.0)
        )
    }
} catch (_: Exception) {
    emptyList()
}
