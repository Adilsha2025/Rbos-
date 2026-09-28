package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.model.DayEntry
import java.io.File
import java.io.FileOutputStream
import java.net.URLEncoder

object ShareUtils {

    /**
     * Creates and shares a PDF report for the month's diary entries
     */
    fun shareMonthPdf(context: Context, entries: List<DayEntry>, monthTitle: String) {
        if (entries.isEmpty()) {
            Toast.makeText(context, "શેર કરવા માટે આ મહિનાનો કોઈ ડેટા નથી", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val pdfDoc = PdfDocument()
            val pageWidth = 595 // A4 standard pt
            val pageHeight = 842

            val titlePaint = Paint().apply {
                color = Color.rgb(21, 128, 61) // #15803d
                textSize = 22f
                isFakeBoldText = true
                isAntiAlias = true
            }

            val subtitlePaint = Paint().apply {
                color = Color.DKGRAY
                textSize = 13f
                isAntiAlias = true
            }

            val headerPaint = Paint().apply {
                color = Color.rgb(22, 101, 52)
                textSize = 12f
                isFakeBoldText = true
                isAntiAlias = true
            }

            val bodyPaint = Paint().apply {
                color = Color.BLACK
                textSize = 11f
                isAntiAlias = true
            }

            val totalPaint = Paint().apply {
                color = Color.rgb(21, 128, 61)
                textSize = 11f
                isFakeBoldText = true
                isAntiAlias = true
            }

            val linePaint = Paint().apply {
                color = Color.LTGRAY
                strokeWidth = 1f
            }

            var pageNumber = 1
            var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            var page = pdfDoc.startPage(pageInfo)
            var canvas: Canvas = page.canvas

            var yPos = 45f

            // Title Header
            canvas.drawText("રોજની ડાયરી - માસિક અહેવાલ", 40f, yPos, titlePaint)
            yPos += 22f
            canvas.drawText("મહિનો: $monthTitle | કુલ સાચવેલા દિવસો: ${entries.size}", 40f, yPos, subtitlePaint)
            yPos += 15f
            canvas.drawLine(40f, yPos, pageWidth - 40f, yPos, linePaint)
            yPos += 25f

            val sorted = entries.sortedBy { it.date }

            for (entry in sorted) {
                // Check if page overflow
                if (yPos > pageHeight - 80f) {
                    pdfDoc.finishPage(page)
                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                    page = pdfDoc.startPage(pageInfo)
                    canvas = page.canvas
                    yPos = 45f
                }

                // Day Box header
                canvas.drawText("${entry.dateDisplay} (${entry.date})", 45f, yPos, headerPaint)
                canvas.drawText(
                    "કુલ: ₹ ${IndianNumberFormatter.formatIndian(entry.total)}",
                    pageWidth - 180f,
                    yPos,
                    totalPaint
                )
                yPos += 16f

                // Items list
                for (item in entry.items) {
                    if (yPos > pageHeight - 50f) {
                        pdfDoc.finishPage(page)
                        pageNumber++
                        pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                        page = pdfDoc.startPage(pageInfo)
                        canvas = page.canvas
                        yPos = 45f
                    }

                    canvas.drawText("• ${item.name}", 55f, yPos, bodyPaint)
                    canvas.drawText(
                        "₹ ${IndianNumberFormatter.formatIndian(item.amount)}",
                        pageWidth - 160f,
                        yPos,
                        bodyPaint
                    )
                    yPos += 14f
                }

                yPos += 6f
                canvas.drawLine(45f, yPos, pageWidth - 45f, yPos, linePaint)
                yPos += 16f
            }

            pdfDoc.finishPage(page)

            // Save PDF to cache
            val cacheFile = File(context.cacheDir, "Rojni_Dairy_${System.currentTimeMillis()}.pdf")
            val outputStream = FileOutputStream(cacheFile)
            pdfDoc.writeTo(outputStream)
            outputStream.flush()
            outputStream.close()
            pdfDoc.close()

            // Share via FileProvider
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                cacheFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, "રોજની ડાયરી - માસિક અહેવાલ ($monthTitle)")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "PDF શેર કરો"))

        } catch (e: Exception) {
            Toast.makeText(context, "PDF બનાવવામાં ભૂલ: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Opens a WhatsApp share link (wa.me) or WhatsApp intent with the selected day's summary
     */
    fun shareDayViaWhatsApp(context: Context, entry: DayEntry) {
        val sb = StringBuilder()
        sb.append("📋 *રોજની ડાયરી*\n")
        sb.append("📅 *તારીખ:* ${entry.dateDisplay}\n")
        sb.append("━━━━━━━━━━━━━━━━━━━━\n")

        for (item in entry.items) {
            sb.append("▫️ ${item.name}: *₹ ${IndianNumberFormatter.formatIndian(item.amount)}*\n")
        }

        val idAmt = entry.items.firstOrNull { it.name.trim().contains("ID") }?.amount ?: 0L
        val cashAmt = entry.items.firstOrNull { it.name.trim().contains("રોકડ") }?.amount ?: 0L
        val floatTotal = idAmt + cashAmt
        sb.append("▫️ ID માં + રોકડ (Float): *₹ ${IndianNumberFormatter.formatIndian(floatTotal)}*\n")

        if (entry.bankDepositAmount > 0) {
            sb.append("🏦 બેંક જમા: *₹ ${IndianNumberFormatter.formatIndian(entry.bankDepositAmount)}*\n")
        }

        sb.append("━━━━━━━━━━━━━━━━━━━━\n")
        sb.append("💰 *આજનો કુલ:* *₹ ${IndianNumberFormatter.formatIndian(entry.total)}*\n")

        if (entry.notes.isNotBlank()) {
            sb.append("📝 *દિવસની નોંધ:* ${entry.notes}\n")
        }

        if (entry.locked) {
            sb.append("🔒 *સ્થિતિ:* લોક થયેલું\n")
        }

        if (entry.edits.isNotEmpty()) {
            sb.append("\n📝 *ફેરફાર ઇતિહાસ:*\n")
            entry.edits.forEach { edit ->
                sb.append("• ${edit.field}: ₹${IndianNumberFormatter.formatIndian(edit.oldValue)} ➔ ₹${IndianNumberFormatter.formatIndian(edit.newValue)} (${edit.time})\n")
            }
        }

        val shareText = sb.toString()

        try {
            val encoded = URLEncoder.encode(shareText, "UTF-8")
            val waUri = Uri.parse("https://wa.me/?text=$encoded")
            val intent = Intent(Intent.ACTION_VIEW, waUri)
            context.startActivity(intent)
        } catch (_: Exception) {
            // Fallback to generic share intent
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, shareText)
                type = "text/plain"
            }
            context.startActivity(Intent.createChooser(sendIntent, "WhatsApp પર મોકલો"))
        }
    }

    /**
     * Shares JSON backup data
     */
    fun shareJsonBackup(context: Context, jsonString: String) {
        try {
            val cacheFile = File(context.cacheDir, "Rojni_Dairy_Backup_${System.currentTimeMillis()}.json")
            cacheFile.writeText(jsonString)

            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                cacheFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, "રોજની ડાયરી બેકઅપ ડેટા")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "બેકઅપ ફાઈલ સેવ અથવા શેર કરો"))
        } catch (e: Exception) {
            Toast.makeText(context, "બેકઅપ શેરિંગમાં ક્ષતિ: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
