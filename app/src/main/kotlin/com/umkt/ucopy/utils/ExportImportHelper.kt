package com.umkt.ucopy.utils

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Html
import android.text.Spanned
import io.noties.markwon.Markwon
import android.text.TextPaint
import android.text.Layout
import android.text.StaticLayout
import android.graphics.Canvas
import android.graphics.Bitmap
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.umkt.ucopy.model.Clip
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonArray
import com.umkt.ucopy.model.Task
import com.umkt.ucopy.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.InputStreamReader
import java.io.OutputStream
import java.util.UUID

object ExportImportHelper {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    fun exportItems(context: Context, uri: Uri, items: List<Clip>): String {
        return try {
            // ✅ "wt" mode to truncate existing file
            context.contentResolver.openOutputStream(uri, "wt")?.use { outputStream ->
                val jsonString = json.encodeToString(items)
                outputStream.write(jsonString.toByteArray())
                context.getString(R.string.export_success_json)
            } ?: context.getString(R.string.export_failed_no_stream)
        } catch (e: Exception) {
            context.getString(R.string.export_failed, e.message ?: "")
        }
    }

    fun exportToTxt(context: Context, uri: Uri, items: List<Clip>): String {
        return try {
            // ✅ "wt" mode
            context.contentResolver.openOutputStream(uri, "wt")?.use { outputStream ->
                val sb = StringBuilder()
                items.forEach { item ->
                    sb.appendLine("Title: ${item.title}")
                    sb.appendLine("Content:")
                    sb.appendLine(item.content)
                    sb.appendLine("---")
                    sb.appendLine()
                }
                outputStream.write(sb.toString().toByteArray())
                context.getString(R.string.export_success_txt)
            } ?: context.getString(R.string.export_failed_no_stream)
        } catch (e: Exception) {
            context.getString(R.string.export_failed, e.message ?: "")
        }
    }

    fun exportToHtml(context: Context, uri: Uri, items: List<Clip>): String {
        return try {
            // ✅ "wt" mode
            context.contentResolver.openOutputStream(uri, "wt")?.use { outputStream ->
                val sb = StringBuilder()
                sb.appendLine("<!DOCTYPE html>")
                sb.appendLine("<html><head><meta charset='utf-8'><title>U Copy Export</title></head><body>")
                items.forEach { item ->
                    sb.appendLine("<div style='border:1px solid #ccc; padding:10px; margin:10px;'>")
                    sb.appendLine("<h3>${item.title}</h3>")
                    sb.appendLine("<p>${item.content.replace("\n", "<br>")}</p>")
                    sb.appendLine("</div>")
                }
                sb.appendLine("</body></html>")
                outputStream.write(sb.toString().toByteArray())
                context.getString(R.string.export_success_html)
            } ?: context.getString(R.string.export_failed_no_stream)
        } catch (e: Exception) {
            context.getString(R.string.export_failed, e.message ?: "")
        }
    }

    fun exportToMarkdown(context: Context, uri: Uri, items: List<Clip>): String {
        return try {
            context.contentResolver.openOutputStream(uri, "wt")?.use { outputStream ->
                val sb = StringBuilder()
                items.forEach { item ->
                    sb.appendLine("# ${item.title.ifEmpty { "Untitled" }}")
                    sb.appendLine()
                    if (item.isTodo) {
                        item.todoItems.forEach { todo ->
                            sb.appendLine("- [${if (todo.isCompleted) "x" else " "}] ${todo.title}")
                        }
                    } else {
                        sb.appendLine(item.content)
                    }
                    sb.appendLine()
                    sb.appendLine("---")
                    sb.appendLine()
                }
                outputStream.write(sb.toString().toByteArray())
                context.getString(R.string.export_success_md)
            } ?: context.getString(R.string.export_failed_no_stream)
        } catch (e: Exception) {
            context.getString(R.string.export_failed, e.message ?: "")
        }
    }

    fun exportToCsv(context: Context, uri: Uri, items: List<Clip>): String {
        return try {
            context.contentResolver.openOutputStream(uri, "wt")?.use { outputStream ->
                val sb = StringBuilder()
                sb.appendLine("Title,Content,Category,Timestamp")
                items.forEach { item ->
                    val title = "\"" + item.title.replace("\"", "\"\"") + "\""
                    val content = "\"" + item.content.replace("\"", "\"\"") + "\""
                    val category = "\"" + (item.category ?: "").replace("\"", "\"\"") + "\""
                    sb.appendLine("$title,$content,$category,${item.timestamp}")
                }
                outputStream.write(sb.toString().toByteArray(Charsets.UTF_8))
                context.getString(R.string.export_success_csv)
            } ?: context.getString(R.string.export_failed_no_stream)
        } catch (e: Exception) {
            context.getString(R.string.export_failed, e.message ?: "")
        }
    }

    suspend fun exportToImage(context: Context, uri: Uri, items: List<Clip>): String {
        return withContext(Dispatchers.IO) {
            try {
                val width = 1080
                val padding = 80f
                val textWidth = width - (padding * 2)
                val exportItems = items.take(20)

                val markwon = Markwon.create(context)

                val titlePaint = TextPaint().apply {
                    color = android.graphics.Color.BLACK
                    textSize = 64f
                    isAntiAlias = true
                    typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                }
                val bodyPaint = TextPaint().apply {
                    color = android.graphics.Color.DKGRAY
                    textSize = 44f
                    isAntiAlias = true
                }
                val headerPaint = TextPaint().apply {
                    color = android.graphics.Color.GRAY
                    textSize = 48f
                    isAntiAlias = true
                    typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.ITALIC)
                }

                val blocks = mutableListOf<Pair<android.text.StaticLayout, Float>>()
                var totalHeight = padding * 2

                val headerText = "U Copy Export"
                val headerLayout = android.text.StaticLayout.Builder.obtain(headerText, 0, headerText.length, headerPaint, textWidth.toInt()).build()
                blocks.add(headerLayout to padding)
                totalHeight += headerLayout.height + padding

                for (item in exportItems) {
                    val t = item.title.ifEmpty { "Untitled" }
                    val titleLayout = android.text.StaticLayout.Builder.obtain(t, 0, t.length, titlePaint, textWidth.toInt()).build()
                    blocks.add(titleLayout to 16f)
                    totalHeight += titleLayout.height + 16f

                    val bodySpanned: Spanned = if (item.isTodo) {
                        val sb = StringBuilder()
                        for (todo in item.todoItems) {
                            if (todo.isCompleted) sb.append("[x] ") else sb.append("[ ] ")
                            sb.append(todo.title)
                            sb.appendLine()
                        }
                        android.text.SpannableString(sb.toString())
                    } else {
                        if (item.content.contains("<html", ignoreCase = true)) {
                            Html.fromHtml(item.content, Html.FROM_HTML_MODE_COMPACT)
                        } else {
                            markwon.toMarkdown(item.content)
                        }
                    }

                    val bodyLayout = android.text.StaticLayout.Builder.obtain(bodySpanned, 0, bodySpanned.length, bodyPaint, textWidth.toInt()).build()
                    blocks.add(bodyLayout to padding)
                    totalHeight += bodyLayout.height + padding
                }

                if (items.size > 20) {
                    val moreText = "... and " + (items.size - 20).toString() + " more notes"
                    val moreLayout = android.text.StaticLayout.Builder.obtain(moreText, 0, moreText.length, bodyPaint, textWidth.toInt()).build()
                    blocks.add(moreLayout to padding)
                    totalHeight += moreLayout.height + padding
                }

                val bitmap = Bitmap.createBitmap(width, totalHeight.toInt(), Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawColor(android.graphics.Color.WHITE)

                var y = padding
                for ((layout, spacing) in blocks) {
                    canvas.save()
                    canvas.translate(padding, y)
                    layout.draw(canvas)
                    canvas.restore()
                    y += layout.height + spacing
                }

                context.contentResolver.openOutputStream(uri, "wt")?.use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                    context.getString(R.string.export_success_png)
                } ?: context.getString(R.string.export_failed_no_stream)
            } catch (e: Exception) {
                context.getString(R.string.export_failed, e.message ?: "")
            }
        }
    }

    suspend fun exportToPdf(context: Context, uri: Uri, items: List<Clip>): String {
        return withContext(Dispatchers.IO) {
            try {
                // ✅ "wt" mode for PDF
                context.contentResolver.openOutputStream(uri, "wt")?.use { outputStream ->
                    createPdf(context, items, outputStream)
                    context.getString(R.string.export_success_pdf)
                } ?: context.getString(R.string.export_failed_no_stream)
            } catch (e: Exception) {
                context.getString(R.string.export_failed, e.message ?: "")
            }
        }
    }

    private fun createPdf(context: Context, items: List<Clip>, outputStream: OutputStream) {
        val document = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val margin = 50f
        val maxY = pageHeight - margin
        var pageNumber = 1
        val textWidth = (pageWidth - margin * 2).toInt()

        val markwon = Markwon.create(context)

        val titlePaint = TextPaint().apply {
            color = android.graphics.Color.BLACK
            textSize = 18f
            isAntiAlias = true
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        val contentPaint = TextPaint().apply {
            color = android.graphics.Color.DKGRAY
            textSize = 12f
            isAntiAlias = true
        }
        val dividerPaint = Paint().apply {
            color = android.graphics.Color.LTGRAY
            strokeWidth = 2f
        }

        fun startNewPage(): PdfDocument.Page {
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            val page = document.startPage(pageInfo)
            pageNumber++
            return page
        }

        var currentPage = startNewPage()
        var canvas = currentPage.canvas
        var y = margin

        fun drawBlock(spanned: Spanned, paint: TextPaint, spacingAfter: Float) {
            val layout = android.text.StaticLayout.Builder.obtain(spanned, 0, spanned.length, paint, textWidth).build()
            if (y + layout.height > maxY) {
                document.finishPage(currentPage)
                currentPage = startNewPage()
                canvas = currentPage.canvas
                y = margin
            }
            canvas.save()
            canvas.translate(margin, y)
            layout.draw(canvas)
            canvas.restore()
            y += layout.height + spacingAfter
        }

        items.forEachIndexed { index, item ->
            val titleSpanned = android.text.SpannableString(item.title.ifEmpty { "Untitled" })
            drawBlock(titleSpanned, titlePaint, 10f)

            val bodySpanned: Spanned = if (item.isTodo) {
                val sb = StringBuilder()
                for (todo in item.todoItems) {
                    if (todo.isCompleted) sb.append("[x] ") else sb.append("[ ] ")
                    sb.append(todo.title)
                    sb.appendLine()
                }
                android.text.SpannableString(sb.toString())
            } else {
                if (item.content.contains("<html", ignoreCase = true)) {
                    Html.fromHtml(item.content, Html.FROM_HTML_MODE_COMPACT)
                } else {
                    markwon.toMarkdown(item.content)
                }
            }
            drawBlock(bodySpanned, contentPaint, 30f)

            if (index < items.size - 1) {
                if (y + 20 > maxY) {
                    document.finishPage(currentPage)
                    currentPage = startNewPage()
                    canvas = currentPage.canvas
                    y = margin
                }
                canvas.drawLine(margin, y, pageWidth - margin, y, dividerPaint)
                y += 20f
            }
        }

        document.finishPage(currentPage)
        document.writeTo(outputStream)
        document.close()
    }

    fun importItems(context: Context, uri: Uri): ImportResult {
        return try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return ImportResult.Error(context.getString(R.string.import_could_not_open))
            if (bytes.isEmpty()) {
                return ImportResult.Error(context.getString(R.string.import_file_empty))
            }

            val isZip = bytes.size >= 4 &&
                bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte() &&
                bytes[2] == 0x03.toByte() && bytes[3] == 0x04.toByte()
            if (isZip) return importFromZip(context, bytes)

            val content = String(bytes, Charsets.UTF_8).trim()
            if (content.isEmpty()) {
                return ImportResult.Error(context.getString(R.string.import_file_empty))
            }

            val head = content.take(2000).lowercase()
            return when {
                content.startsWith("[") || content.startsWith("{") -> parseJsonContent(context, content)
                head.contains("<en-export") -> importFromEnex(context, content)
                head.contains("<opml") -> importFromOpml(context, content)
                head.contains("<html") || head.contains("<body") ||
                    head.contains("<div") || head.contains("<p>") || head.contains("<p ") ||
                    head.contains("<h1") || head.contains("<h2") -> importFromHtml(context, content)
                head.startsWith("<?xml") || head.contains("<note>") || head.contains("<note ") ->
                    importFromXmlGeneric(context, content)
                isCsvContent(content) -> importFromCsv(context, content)
                isMarkdownContent(content) -> importFromMarkdown(context, content)
                else -> importFromTxt(context, content)
            }
        } catch (e: Exception) {
            ImportResult.Error(context.getString(R.string.import_failed, e.message ?: ""))
        }
    }

    private fun importFromTxt(context: Context, content: String): ImportResult {
        try {
            val items = mutableListOf<Clip>()

            val hasTitleMarker = Regex("(?i)^\\s*Title:").containsMatchIn(content)
            val hasContentMarker = Regex("(?i)^\\s*Content:").containsMatchIn(content)

            if (!hasTitleMarker && !hasContentMarker) {
                val title = content.lines().firstOrNull()?.take(60)?.trim() ?: "Untitled"
                items.add(
                    Clip(
                        id = UUID.randomUUID().toString(),
                        title = title.ifEmpty { "Untitled" },
                        content = content,
                        timestamp = System.currentTimeMillis()
                    )
                )
                return ImportResult.Success(items)
            }

            val blocks = content.split("---").filter { it.isNotBlank() }

            if (blocks.isEmpty()) {
                parseSingleBlock(content, items)
            } else {
                blocks.forEach { block ->
                    parseSingleBlock(block, items)
                }
            }

            return if (items.isNotEmpty()) {
                ImportResult.Success(items)
            } else {
                ImportResult.Error(context.getString(R.string.import_no_valid_items_txt))
            }
        } catch (e: Exception) {
            return ImportResult.Error(context.getString(R.string.import_parse_txt_failed, e.message ?: ""))
        }
    }

    private fun parseSingleBlock(block: String, items: MutableList<Clip>) {
        val lines = block.lines().filter { it.isNotBlank() }
        var title = "Untitled"
        var body = ""

        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            when {
                line.lowercase().startsWith("title:") -> {
                    title = line.substringAfter(":").trim()
                    if (title.isEmpty()) title = "Untitled"
                }
                line.lowercase().startsWith("content:") -> {
                    val contentLines = mutableListOf<String>()
                    i++
                    while (i < lines.size) {
                        val nextLine = lines[i]
                        if (nextLine.lowercase().startsWith("title:")) {
                            break
                        }
                        contentLines.add(nextLine)
                        i++
                    }
                    body = contentLines.joinToString("\n")
                    continue
                }
                else -> {
                    if (body.isEmpty()) {
                        body = line
                    } else {
                        body += "\n$line"
                    }
                }
            }
            i++
        }

        if (body.isNotBlank()) {
            items.add(
                Clip(
                    id = UUID.randomUUID().toString(),
                    title = title,
                    content = body,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    private fun importFromHtml(context: Context, content: String): ImportResult {
        try {
            val items = mutableListOf<Clip>()
            val divPattern = Regex("<div[^>]*>(.*?)</div>", RegexOption.DOT_MATCHES_ALL)
            val titlePattern = Regex("<h3[^>]*>(.*?)</h3>", RegexOption.DOT_MATCHES_ALL)
            val contentPattern = Regex("<p[^>]*>(.*?)</p>", RegexOption.DOT_MATCHES_ALL)

            val divs = divPattern.findAll(content)

            if (!divs.any()) {
                val title = titlePattern.find(content)?.groupValues?.get(1)?.trim() ?: "Untitled"
                val body = contentPattern.find(content)?.groupValues?.get(1)?.trim()?.replace("<br>", "\n") ?: content
                if (body.isNotBlank()) {
                    items.add(
                        Clip(
                            id = UUID.randomUUID().toString(),
                            title = title,
                            content = body,
                            timestamp = System.currentTimeMillis()
                        )
                    )
                }
            } else {
                divs.forEach { divMatch ->
                    val div = divMatch.value
                    val title = titlePattern.find(div)?.groupValues?.get(1)?.trim() ?: "Untitled"
                    val body = contentPattern.find(div)?.groupValues?.get(1)?.trim()?.replace("<br>", "\n") ?: ""
                    if (body.isNotBlank()) {
                        items.add(
                            Clip(
                                id = UUID.randomUUID().toString(),
                                title = title,
                                content = body,
                                timestamp = System.currentTimeMillis()
                            )
                        )
                    }
                }
            }

            return if (items.isNotEmpty()) {
                ImportResult.Success(items)
            } else {
                ImportResult.Error(context.getString(R.string.import_no_valid_items_html))
            }
        } catch (e: Exception) {
            return ImportResult.Error(context.getString(R.string.import_parse_html_failed, e.message ?: ""))
        }
    }

    private fun importFromZip(context: Context, bytes: ByteArray): ImportResult {
        return try {
            val jsonItems = mutableListOf<Clip>()
            val textItems = mutableListOf<Clip>()
            java.util.zip.ZipInputStream(java.io.ByteArrayInputStream(bytes)).use { zipIn ->
                var entry = zipIn.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val name = entry.name.lowercase()
                        val entryBytes = try { zipIn.readBytes() } catch (_: Exception) { ByteArray(0) }
                        if (entryBytes.isNotEmpty()) {
                            val entryText = String(entryBytes, Charsets.UTF_8).trim()
                            if (entryText.isNotEmpty()) {
                                if (name.endsWith(".json")) {
                                    val result = parseJsonContent(context, entryText)
                                    if (result is ImportResult.Success) jsonItems.addAll(result.items)
                                } else if (name.endsWith(".md") || name.endsWith(".txt")) {
                                    val result = if (name.endsWith(".md"))
                                        importFromMarkdown(context, entryText)
                                    else importFromTxt(context, entryText)
                                    if (result is ImportResult.Success) textItems.addAll(result.items)
                                }
                            }
                        }
                    }
                    entry = zipIn.nextEntry
                }
            }
            val items = if (jsonItems.isNotEmpty()) jsonItems else textItems
            if (items.isNotEmpty()) ImportResult.Success(items)
            else ImportResult.Error(context.getString(R.string.import_zip_no_clips))
        } catch (e: Exception) {
            ImportResult.Error(context.getString(R.string.import_invalid_zip))
        }
    }

    private fun parseJsonContent(context: Context, content: String): ImportResult {
        try {
            val backupData = json.decodeFromString<BackupHelper.BackupData>(content)
            if (backupData.clips.isNotEmpty()) return ImportResult.Success(backupData.clips)
        } catch (_: Exception) { }
        try {
            val items = json.decodeFromString<List<Clip>>(content)
            if (items.isNotEmpty()) return ImportResult.Success(items)
        } catch (_: Exception) { }
        return parseGoogleKeepJson(context, content)
    }

    private fun parseGoogleKeepJson(context: Context, content: String): ImportResult {
        return try {
            val element = json.parseToJsonElement(content)
            val noteObjects: List<JsonObject> = when (element) {
                is JsonArray -> element.mapNotNull { it as? JsonObject }
                is JsonObject -> listOf(element)
                else -> emptyList()
            }
            val items = mutableListOf<Clip>()
            noteObjects.forEach { obj ->
                val title = obj["title"]?.jsonPrimitive?.contentOrNull ?: ""
                val textContent = obj["textContent"]?.jsonPrimitive?.contentOrNull
                val listContent = obj["listContent"] as? JsonArray
                val timestampUsec = obj["userEditedTimestampUsec"]?.jsonPrimitive?.longOrNull
                    ?: obj["createdTimestampUsec"]?.jsonPrimitive?.longOrNull
                val timestamp = timestampUsec?.let { it / 1000 } ?: System.currentTimeMillis()

                if (listContent != null) {
                    val todoItems = listContent.mapNotNull { itemJson ->
                        val itemObj = itemJson as? JsonObject ?: return@mapNotNull null
                        val text = itemObj["text"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                        val isChecked = itemObj["isChecked"]?.jsonPrimitive?.booleanOrNull ?: false
                        Task(title = text, isCompleted = isChecked)
                    }
                    if (todoItems.isNotEmpty() || title.isNotBlank()) {
                        items.add(Clip(
                            id = UUID.randomUUID().toString(),
                            title = title.ifEmpty { "Untitled" },
                            content = "",
                            timestamp = timestamp,
                            isTodo = true,
                            todoItems = todoItems
                        ))
                    }
                } else if (!textContent.isNullOrBlank() || title.isNotBlank()) {
                    val body = textContent ?: ""
                    items.add(Clip(
                        id = UUID.randomUUID().toString(),
                        title = title.ifEmpty { body.lines().firstOrNull()?.take(60) ?: "Untitled" },
                        content = body,
                        timestamp = timestamp
                    ))
                }
            }
            if (items.isNotEmpty()) ImportResult.Success(items)
            else ImportResult.Error(context.getString(R.string.import_invalid_json, "no valid notes found"))
        } catch (e: Exception) {
            ImportResult.Error(context.getString(R.string.import_invalid_json, e.message ?: ""))
        }
    }

    private fun importFromMarkdown(context: Context, content: String): ImportResult {
        return try {
            val items = mutableListOf<Clip>()
            val lines = content.lines()

            // Smartly detect split level: use H1 if available, otherwise fallback to H2
            var splitLevel = 1
            val hasH1 = lines.any { it.matches(Regex("^#\\s+\\S.*")) }
            if (!hasH1) {
                val hasH2 = lines.any { it.matches(Regex("^##\\s+\\S.*")) }
                if (hasH2) splitLevel = 2
            }

            val prefix = "#".repeat(splitLevel) + " "
            val indices = mutableListOf<Int>()
            lines.forEachIndexed { idx, line ->
                if (line.startsWith(prefix)) {
                    indices.add(idx)
                }
            }

            if (indices.isEmpty()) {
                val title = lines.firstOrNull { it.isNotBlank() }?.trim()?.removePrefix("#")?.trim()?.take(60) ?: "Untitled"
                items.add(Clip(
                    id = UUID.randomUUID().toString(),
                    title = title.ifEmpty { "Untitled" },
                    content = content,
                    timestamp = System.currentTimeMillis()
                ))
            } else {
                for (i in indices.indices) {
                    val startLine = indices[i]
                    val endLine = if (i + 1 < indices.size) indices[i + 1] else lines.size
                    val titleLine = lines[startLine].removePrefix(prefix).trim()
                    val body = lines.subList(startLine + 1, endLine).joinToString("\n").trim()
                    items.add(Clip(
                        id = UUID.randomUUID().toString(),
                        title = titleLine.ifEmpty { "Untitled" },
                        content = body,
                        timestamp = System.currentTimeMillis()
                    ))
                }
            }
            if (items.isNotEmpty()) ImportResult.Success(items)
            else ImportResult.Error(context.getString(R.string.import_no_valid_items_md))
        } catch (e: Exception) {
            ImportResult.Error(context.getString(R.string.import_parse_md_failed, e.message ?: ""))
        }
    }

    private fun isMarkdownContent(content: String): Boolean {
        var signals = 0
        for (line in content.lines().take(80)) {
            val t = line.trimStart()
            if (t.startsWith("# ") || t.startsWith("## ") || t.startsWith("### ") ||
                t.startsWith("- ") || t.startsWith("* ") || t.startsWith("> ") ||
                t.matches(Regex("^\\d+\\.\\s+.*"))) {
                signals++
                if (signals >= 2) return true
            }
        }
        return false
    }

    private fun isCsvContent(content: String): Boolean {
        val rows = splitCsvRows(content).filter { it.isNotBlank() }
        if (rows.size < 2) return false
        val sample = rows.take(10)
        val counts = sample.map { row ->
            var count = 0
            var inQ = false
            for (ch in row) {
                when {
                    ch == '"' -> inQ = !inQ
                    ch == ',' && !inQ -> count++
                }
            }
            count
        }
        return counts.all { it > 0 } && counts.distinct().size <= 2
    }

    private fun splitCsvRows(content: String): List<String> {
        val rows = mutableListOf<String>()
        val cur = StringBuilder()
        var inQ = false
        var i = 0
        while (i < content.length) {
            val ch = content[i]
            if (ch == '"') {
                if (inQ && i + 1 < content.length && content[i + 1] == '"') {
                    cur.append('"'); i += 2; continue
                }
                inQ = !inQ
                cur.append(ch)
            } else if (ch == '\n' && !inQ) {
                rows.add(cur.toString()); cur.clear()
            } else if (ch == '\r') {
                // skip CR
            } else {
                cur.append(ch)
            }
            i++
        }
        if (cur.isNotEmpty()) rows.add(cur.toString())
        return rows
    }

    private fun parseCsvLine(line: String): List<String> {
        val fields = mutableListOf<String>()
        val cur = StringBuilder()
        var inQ = false
        var i = 0
        while (i < line.length) {
            val ch = line[i]
            when {
                ch == '"' && inQ && i + 1 < line.length && line[i + 1] == '"' -> {
                    cur.append('"'); i++
                }
                ch == '"' -> inQ = !inQ
                ch == ',' && !inQ -> { fields.add(cur.toString()); cur.clear() }
                else -> cur.append(ch)
            }
            i++
        }
        fields.add(cur.toString())
        return fields
    }

    private fun importFromCsv(context: Context, content: String): ImportResult {
        return try {
            val rows = splitCsvRows(content).filter { it.isNotBlank() }
            if (rows.isEmpty()) return ImportResult.Error(context.getString(R.string.import_no_valid_items_csv))

            val firstFields = parseCsvLine(rows[0]).map { it.trim().lowercase() }
            val hasHeader = firstFields.any {
                it == "title" || it == "text" || it == "content" || it == "note" || it == "body"
            }
            val header = if (hasHeader) firstFields else emptyList()
            val titleIdx = header.indexOfFirst { it == "title" || it == "name" }
            val contentIdx = header.indexOfFirst { it == "text" || it == "content" || it == "note" || it == "body" }
            val startIdx = if (hasHeader) 1 else 0

            val items = mutableListOf<Clip>()
            for (i in startIdx until rows.size) {
                val fields = parseCsvLine(rows[i])
                if (fields.all { it.isBlank() }) continue

                val title: String
                val body: String
                if (hasHeader && titleIdx >= 0) {
                    title = fields.getOrNull(titleIdx)?.trim() ?: ""
                    body = if (contentIdx >= 0) fields.getOrNull(contentIdx)?.trim() ?: ""
                           else fields.filterIndexed { idx, _ -> idx != titleIdx }.joinToString(" ").trim()
                } else {
                    title = fields.firstOrNull()?.trim() ?: ""
                    body = fields.drop(1).joinToString("\n").trim()
                }
                if (title.isBlank() && body.isBlank()) continue
                items.add(Clip(
                    id = UUID.randomUUID().toString(),
                    title = title.ifEmpty { body.lines().firstOrNull()?.take(60) ?: "Untitled" },
                    content = body,
                    timestamp = System.currentTimeMillis()
                ))
            }
            if (items.isNotEmpty()) ImportResult.Success(items)
            else ImportResult.Error(context.getString(R.string.import_no_valid_items_csv))
        } catch (e: Exception) {
            ImportResult.Error(context.getString(R.string.import_parse_csv_failed, e.message ?: ""))
        }
    }

    private fun stripHtmlTags(html: String): String {
        return html.replace(Regex("<[^>]*>"), "").trim()
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&nbsp;", " ")
    }

    private fun importFromXmlGeneric(context: Context, content: String): ImportResult {
        return try {
            val parser = android.util.Xml.newPullParser()
            parser.setFeature(org.xmlpull.v1.XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(java.io.StringReader(content))

            val items = mutableListOf<Clip>()
            var currentTitle: String? = null
            var currentBody: String? = null
            var currentTag: String? = null
            val buf = StringBuilder()
            var inNote = false

            var ev = parser.eventType
            while (ev != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
                when (ev) {
                    org.xmlpull.v1.XmlPullParser.START_TAG -> {
                        val name = parser.name?.lowercase() ?: ""
                        if (name == "note" || name == "entry" || name == "item" || name == "clip") {
                            inNote = true
                            currentTitle = null; currentBody = null; currentTag = null
                        } else if (inNote && (name == "title" || name == "name")) {
                            currentTag = "title"; buf.clear()
                        } else if (inNote && (name == "content" || name == "text" || name == "body" || name == "description")) {
                            currentTag = "content"; buf.clear()
                        }
                    }
                    org.xmlpull.v1.XmlPullParser.TEXT, org.xmlpull.v1.XmlPullParser.CDSECT -> {
                        if (currentTag != null) buf.append(parser.text ?: "")
                    }
                    org.xmlpull.v1.XmlPullParser.END_TAG -> {
                        val name = parser.name?.lowercase() ?: ""
                        if (currentTag == "title" && (name == "title" || name == "name")) {
                            currentTitle = buf.toString().trim(); currentTag = null
                        } else if (currentTag == "content" &&
                            (name == "content" || name == "text" || name == "body" || name == "description")) {
                            currentBody = buf.toString().trim(); currentTag = null
                        } else if (inNote &&
                            (name == "note" || name == "entry" || name == "item" || name == "clip")) {
                            val t = currentTitle ?: ""
                            val b = currentBody ?: ""
                            if (t.isNotBlank() || b.isNotBlank()) {
                                items.add(Clip(
                                    id = UUID.randomUUID().toString(),
                                    title = t.ifEmpty { b.lines().firstOrNull()?.take(60) ?: "Untitled" },
                                    content = b,
                                    timestamp = System.currentTimeMillis()
                                ))
                            }
                            inNote = false; currentTitle = null; currentBody = null
                        }
                    }
                }
                ev = parser.next()
            }
            if (items.isNotEmpty()) ImportResult.Success(items)
            else ImportResult.Error(context.getString(R.string.import_no_valid_items_xml))
        } catch (e: Exception) {
            ImportResult.Error(context.getString(R.string.import_parse_xml_failed, e.message ?: ""))
        }
    }

    private fun importFromEnex(context: Context, content: String): ImportResult {
        return try {
            val noteRegex = Regex("<note>(.*?)</note>",
                setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
            val titleRegex = Regex("<title>(.*?)</title>",
                setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
            val contentRegex = Regex("<content>(.*?)</content>",
                setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))

            val items = mutableListOf<Clip>()
            noteRegex.findAll(content).forEach { noteMatch ->
                val noteXml = noteMatch.groupValues[1]
                val title = titleRegex.find(noteXml)?.groupValues?.get(1)?.trim()?.let { stripHtmlTags(it) } ?: ""
                var body = contentRegex.find(noteXml)?.groupValues?.get(1) ?: ""
                body = body.trim()
                if (body.startsWith("<![CDATA[")) body = body.removePrefix("<![CDATA[")
                if (body.endsWith("]]>")) body = body.removeSuffix("]]>")
                body = stripHtmlTags(body)

                if (title.isNotBlank() || body.isNotBlank()) {
                    items.add(Clip(
                        id = UUID.randomUUID().toString(),
                        title = title.ifEmpty { body.lines().firstOrNull()?.take(60) ?: "Untitled" },
                        content = body,
                        timestamp = System.currentTimeMillis()
                    ))
                }
            }
            if (items.isNotEmpty()) ImportResult.Success(items)
            else ImportResult.Error(context.getString(R.string.import_no_valid_items_enex))
        } catch (e: Exception) {
            ImportResult.Error(context.getString(R.string.import_parse_enex_failed, e.message ?: ""))
        }
    }

    private fun importFromOpml(context: Context, content: String): ImportResult {
        return try {
            val outlineRegex = Regex("<outline\\s+([^>]*?)/?>", RegexOption.IGNORE_CASE)
            val textAttr = Regex("\\btext\\s*=\\s*\"([^\"]*)\"", RegexOption.IGNORE_CASE)
            val titleAttr = Regex("\\btitle\\s*=\\s*\"([^\"]*)\"", RegexOption.IGNORE_CASE)

            val items = mutableListOf<Clip>()
            outlineRegex.findAll(content).forEach { match ->
                val attrs = match.groupValues[1]
                val rawText = textAttr.find(attrs)?.groupValues?.get(1)
                    ?: titleAttr.find(attrs)?.groupValues?.get(1)
                    ?: return@forEach
                val decoded = stripHtmlTags(rawText).trim()
                if (decoded.isNotBlank()) {
                    items.add(Clip(
                        id = UUID.randomUUID().toString(),
                        title = decoded.take(60),
                        content = decoded,
                        timestamp = System.currentTimeMillis()
                    ))
                }
            }
            if (items.isNotEmpty()) ImportResult.Success(items)
            else ImportResult.Error(context.getString(R.string.import_no_valid_items_opml))
        } catch (e: Exception) {
            ImportResult.Error(context.getString(R.string.import_parse_opml_failed, e.message ?: ""))
        }
    }

    sealed class ImportResult {
        data class Success(val items: List<Clip>) : ImportResult()
        data class Error(val message: String) : ImportResult()
    }
}