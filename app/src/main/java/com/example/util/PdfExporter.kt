package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.local.entity.TransactionWithItems
import java.io.File
import java.io.FileOutputStream

object PdfExporter {

    fun generateAndSharePdf(
        context: Context,
        businessName: String,
        periodText: String,
        transactions: List<TransactionWithItems>,
        totalIncome: Long,
        totalExpense: Long,
        detailedPerItem: Boolean = true
    ): File? {
        try {
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 standard size in points
            val page = pdfDocument.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            val paint = Paint()
            val textPaint = Paint().apply {
                isAntiAlias = true
                textSize = 10f
                color = Color.DKGRAY
            }

            val titlePaint = Paint().apply {
                isAntiAlias = true
                textSize = 16f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                color = Color.parseColor("#4F46E5")
            }

            val headerPaint = Paint().apply {
                isAntiAlias = true
                textSize = 11f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                color = Color.parseColor("#0F172A")
            }

            val subheaderPaint = Paint().apply {
                isAntiAlias = true
                textSize = 9f
                color = Color.parseColor("#64748B")
            }

            val tableHeaderPaint = Paint().apply {
                isAntiAlias = true
                textSize = 9f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                color = Color.parseColor("#1E1B4B")
            }

            val bgHeaderPaint = Paint().apply {
                color = Color.parseColor("#E0E7FF")
                style = Paint.Style.FILL
            }

            val zebraPaint = Paint().apply {
                color = Color.parseColor("#F8FAFC")
                style = Paint.Style.FILL
            }

            val borderPaint = Paint().apply {
                color = Color.parseColor("#CBD5E1")
                style = Paint.Style.STROKE
                strokeWidth = 0.5f
            }

            var currentY = 40f
            val margin = 36f
            val contentWidth = 595f - (margin * 2)

            // 1. Header
            canvas.drawText(businessName.uppercase(), margin, currentY, titlePaint)
            currentY += 16f
            canvas.drawText("Laporan Keuangan & Transaksi", margin, currentY, headerPaint)
            canvas.drawText("Periode: $periodText", margin, currentY + 14f, subheaderPaint)
            val createdDateText = "Dibuat: ${DateUtils.formatFullDate(System.currentTimeMillis())}"
            val dateWidth = subheaderPaint.measureText(createdDateText)
            canvas.drawText(createdDateText, 595f - margin - dateWidth, currentY + 14f, subheaderPaint)

            currentY += 32f

            // 2. Financial Summary Card
            val summaryHeight = 44f
            paint.color = Color.parseColor("#F1F5F9")
            paint.style = Paint.Style.FILL
            canvas.drawRoundRect(margin, currentY, margin + contentWidth, currentY + summaryHeight, 6f, 6f, paint)
            canvas.drawRoundRect(margin, currentY, margin + contentWidth, currentY + summaryHeight, 6f, 6f, borderPaint)

            val thirdWidth = contentWidth / 3f
            val selisih = totalIncome - totalExpense

            canvas.drawText("Total Pemasukan", margin + 12f, currentY + 16f, subheaderPaint)
            val incPaint = Paint().apply {
                isAntiAlias = true
                textSize = 11f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                color = Color.parseColor("#15803D")
            }
            canvas.drawText(MoneyFormatter.format(totalIncome), margin + 12f, currentY + 34f, incPaint)

            canvas.drawText("Total Pengeluaran", margin + thirdWidth + 12f, currentY + 16f, subheaderPaint)
            val expPaint = Paint().apply {
                isAntiAlias = true
                textSize = 11f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                color = Color.parseColor("#BE123C")
            }
            canvas.drawText(MoneyFormatter.format(totalExpense), margin + thirdWidth + 12f, currentY + 34f, expPaint)

            canvas.drawText("Laba Bersih / Selisih", margin + (thirdWidth * 2) + 12f, currentY + 16f, subheaderPaint)
            val profitPaint = Paint().apply {
                isAntiAlias = true
                textSize = 11f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                color = if (selisih >= 0) Color.parseColor("#15803D") else Color.parseColor("#BE123C")
            }
            canvas.drawText(MoneyFormatter.format(selisih), margin + (thirdWidth * 2) + 12f, currentY + 34f, profitPaint)

            currentY += summaryHeight + 20f

            // 3. Transactions Table Header
            val tableRowHeight = 22f
            canvas.drawRect(margin, currentY, margin + contentWidth, currentY + tableRowHeight, bgHeaderPaint)
            canvas.drawRect(margin, currentY, margin + contentWidth, currentY + tableRowHeight, borderPaint)

            val colNo = margin + 6f
            val colDate = margin + 30f
            val colType = margin + 95f
            val colParty = margin + 155f
            val colItem = margin + 250f
            val colQty = margin + 380f
            val colAmount = margin + contentWidth - 6f

            canvas.drawText("No", colNo, currentY + 14f, tableHeaderPaint)
            canvas.drawText("Tanggal", colDate, currentY + 14f, tableHeaderPaint)
            canvas.drawText("Jenis", colType, currentY + 14f, tableHeaderPaint)
            canvas.drawText("Pihak / Toko", colParty, currentY + 14f, tableHeaderPaint)
            canvas.drawText("Rincian Barang", colItem, currentY + 14f, tableHeaderPaint)
            canvas.drawText("Qty", colQty, currentY + 14f, tableHeaderPaint)

            val amountHeader = "Nominal"
            val amtHeaderWidth = tableHeaderPaint.measureText(amountHeader)
            canvas.drawText(amountHeader, colAmount - amtHeaderWidth, currentY + 14f, tableHeaderPaint)

            currentY += tableRowHeight

            // Table Content
            val rowTextPaint = Paint().apply {
                isAntiAlias = true
                textSize = 8.5f
                color = Color.parseColor("#1E293B")
            }

            var rowIndex = 1
            for (txWithItems in transactions) {
                if (currentY > 780f) break // Simple 1-page bounds check

                val tx = txWithItems.transaction
                val isZebra = rowIndex % 2 == 0
                val rowHeight = if (detailedPerItem && txWithItems.items.size > 1) {
                    16f + (txWithItems.items.size * 13f)
                } else {
                    20f
                }

                if (isZebra) {
                    canvas.drawRect(margin, currentY, margin + contentWidth, currentY + rowHeight, zebraPaint)
                }
                canvas.drawRect(margin, currentY, margin + contentWidth, currentY + rowHeight, borderPaint)

                canvas.drawText(rowIndex.toString(), colNo, currentY + 13f, rowTextPaint)
                canvas.drawText(DateUtils.formatDayMonth(tx.occurredAt), colDate, currentY + 13f, rowTextPaint)
                
                val typeLabel = if (tx.type == "income") "Pemasukan" else "Pengeluaran"
                val typeColorPaint = Paint(rowTextPaint).apply {
                    color = if (tx.type == "income") Color.parseColor("#15803D") else Color.parseColor("#BE123C")
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                canvas.drawText(typeLabel, colType, currentY + 13f, typeColorPaint)

                val partyText = (tx.counterparty ?: "-").take(18)
                canvas.drawText(partyText, colParty, currentY + 13f, rowTextPaint)

                if (detailedPerItem && txWithItems.items.isNotEmpty()) {
                    var itemY = currentY + 13f
                    for (item in txWithItems.items) {
                        canvas.drawText("${item.name.take(22)}", colItem, itemY, rowTextPaint)
                        canvas.drawText("${item.qty} ${item.unit}", colQty, itemY, rowTextPaint)
                        itemY += 13f
                    }
                } else {
                    val summaryItems = if (txWithItems.items.isNotEmpty()) {
                        "${txWithItems.items.size} jenis barang"
                    } else {
                        tx.note ?: "-"
                    }
                    canvas.drawText(summaryItems, colItem, currentY + 13f, rowTextPaint)
                    canvas.drawText("-", colQty, currentY + 13f, rowTextPaint)
                }

                val amountStr = MoneyFormatter.formatSigned(tx.totalAmount, tx.type == "income")
                val amtWidth = typeColorPaint.measureText(amountStr)
                canvas.drawText(amountStr, colAmount - amtWidth, currentY + 13f, typeColorPaint)

                currentY += rowHeight
                rowIndex++
            }

            // Footer
            val footerY = 810f
            canvas.drawText("CatatUsaha • Aplikasi Pembukuan & Stok UMKM", margin, footerY, subheaderPaint)
            val pageNum = "Halaman 1 dari 1"
            val pageNumWidth = subheaderPaint.measureText(pageNum)
            canvas.drawText(pageNum, 595f - margin - pageNumWidth, footerY, subheaderPaint)

            pdfDocument.finishPage(page)

            // Save to Cache Directory
            val reportsDir = File(context.cacheDir, "reports")
            if (!reportsDir.exists()) reportsDir.mkdirs()

            val pdfFile = File(reportsDir, "Laporan_CatatUsaha_${System.currentTimeMillis()}.pdf")
            val outputStream = FileOutputStream(pdfFile)
            pdfDocument.writeTo(outputStream)
            pdfDocument.close()
            outputStream.flush()
            outputStream.close()

            return pdfFile
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun shareFile(context: Context, file: File, mimeType: String = "application/pdf") {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, file.name)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(shareIntent, "Bagikan Laporan"))
    }
}
