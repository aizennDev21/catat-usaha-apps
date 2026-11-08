package com.example.util

import android.content.Context
import com.example.data.local.entity.TransactionWithItems
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets

object CsvExporter {

    fun generateAndShareCsv(
        context: Context,
        businessName: String,
        periodText: String,
        transactions: List<TransactionWithItems>,
        detailedPerItem: Boolean = true
    ): File? {
        try {
            val reportsDir = File(context.cacheDir, "reports")
            if (!reportsDir.exists()) reportsDir.mkdirs()

            val csvFile = File(reportsDir, "Laporan_${businessName.replace(" ", "_")}_${System.currentTimeMillis()}.csv")
            val outputStream = FileOutputStream(csvFile)
            val writer = OutputStreamWriter(outputStream, StandardCharsets.UTF_8)

            // UTF-8 BOM so Excel opens with correct encoding
            writer.write("\uFEFF")

            // Metadata Headers
            writer.write("LAPORAN TRANSAKSI - $businessName\n")
            writer.write("Periode: $periodText\n")
            writer.write("Dibuat Pada: ${DateUtils.formatFullDate(System.currentTimeMillis())}\n\n")

            if (detailedPerItem) {
                writer.write("ID Transaksi;Tanggal;Jam;Jenis;Kategori;Pihak / Toko;Metode Bayar;Nama Barang;Qty;Satuan;Harga Satuan;Subtotal;Catatan\n")
                for (txWithItems in transactions) {
                    val tx = txWithItems.transaction
                    val date = DateUtils.formatShortDate(tx.occurredAt)
                    val time = DateUtils.formatTime(tx.occurredAt)
                    val type = if (tx.type == "income") "Pemasukan" else "Pengeluaran"
                    val party = (tx.counterparty ?: "").replace(";", ",")
                    val method = tx.paymentMethod.uppercase()
                    val note = (tx.note ?: "").replace(";", ",")

                    if (txWithItems.items.isNotEmpty()) {
                        for (item in txWithItems.items) {
                            val itemName = item.name.replace(";", ",")
                            writer.write("${tx.id};$date;$time;$type;${tx.categoryId ?: ""};$party;$method;$itemName;${item.qty};${item.unit};${item.unitPrice};${item.subtotal};$note\n")
                        }
                    } else {
                        writer.write("${tx.id};$date;$time;$type;${tx.categoryId ?: ""};$party;$method;-;0;-;0;${tx.totalAmount};$note\n")
                    }
                }
            } else {
                writer.write("ID Transaksi;Tanggal;Jam;Jenis;Kategori;Pihak / Toko;Metode Bayar;Total Nominal;Jumlah Item;Catatan\n")
                for (txWithItems in transactions) {
                    val tx = txWithItems.transaction
                    val date = DateUtils.formatShortDate(tx.occurredAt)
                    val time = DateUtils.formatTime(tx.occurredAt)
                    val type = if (tx.type == "income") "Pemasukan" else "Pengeluaran"
                    val party = (tx.counterparty ?: "").replace(";", ",")
                    val method = tx.paymentMethod.uppercase()
                    val note = (tx.note ?: "").replace(";", ",")
                    val itemCount = txWithItems.items.size

                    writer.write("${tx.id};$date;$time;$type;${tx.categoryId ?: ""};$party;$method;${tx.totalAmount};$itemCount;$note\n")
                }
            }

            writer.flush()
            writer.close()

            return csvFile
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}
