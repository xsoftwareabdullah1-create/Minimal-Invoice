package com.minimalinvoice

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.minimalinvoice.data.InvoiceEntity
import com.minimalinvoice.data.toInvoiceLines
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfMaker {
    fun create(
        context: Context,
        invoice: InvoiceEntity,
        businessName: String,
        currency: String
    ): android.net.Uri {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = document.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val regular = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.BLACK
            textSize = 11f
        }
        val bold = Paint(regular).apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val title = Paint(bold).apply { textSize = 27f }
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

        fun draw(text: String, x: Float, y: Float, paint: Paint = regular) {
            canvas.drawText(text.take(76), x, y, paint)
        }

        draw(businessName.ifBlank { "Your business" }, 42f, 58f, bold)
        draw("INVOICE", 42f, 116f, title)
        draw("INV-${invoice.invoiceNumber}", 42f, 139f)
        draw("BILL TO", 42f, 194f, bold)
        draw(invoice.clientName, 42f, 214f)
        draw("Issued: ${dateFormat.format(Date(invoice.createdAt))}", 365f, 194f)
        draw("Due: ${dateFormat.format(Date(invoice.dueAt))}", 365f, 214f)

        canvas.drawLine(42f, 246f, 553f, 246f, regular)
        draw("DESCRIPTION", 42f, 272f, bold)
        draw("QTY", 345f, 272f, bold)
        draw("RATE", 414f, 272f, bold)
        draw("AMOUNT", 490f, 272f, bold)

        var y = 302f
        invoice.itemsJson.toInvoiceLines().forEach { item ->
            draw(item.description, 42f, y)
            draw("%.2f".format(item.quantity), 345f, y)
            draw("$currency%.2f".format(item.rate), 414f, y)
            draw("$currency%.2f".format(item.amount), 490f, y)
            y += 25f
        }

        canvas.drawLine(42f, y + 5f, 553f, y + 5f, regular)
        y += 35f
        draw("Subtotal", 390f, y)
        draw("$currency%.2f".format(invoice.subtotal), 490f, y)
        y += 23f
        draw("Tax (${invoice.taxPercent}%)", 390f, y)
        draw("$currency%.2f".format(invoice.total - invoice.subtotal), 490f, y)
        y += 29f
        draw("TOTAL", 390f, y, bold)
        draw("$currency%.2f".format(invoice.total), 490f, y, bold)

        if (invoice.notes.isNotBlank()) {
            draw("NOTES", 42f, 700f, bold)
            draw(invoice.notes, 42f, 720f)
        }
        draw(if (invoice.paid) "PAID" else "PAYMENT DUE", 42f, 790f, bold)

        document.finishPage(page)
        val directory = File(context.cacheDir, "invoices").apply { mkdirs() }
        val output = File(directory, "Invoice-${invoice.invoiceNumber}.pdf")
        try {
            FileOutputStream(output).use { document.writeTo(it) }
        } finally {
            document.close()
        }
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            output
        )
    }
}
