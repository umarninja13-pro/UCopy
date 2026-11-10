package com.umkt.ucopy.utils

import android.content.Context
import com.umkt.ucopy.data.UserPreferences
import android.net.Uri
import com.umkt.ucopy.model.Clip
import com.umkt.ucopy.R
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import java.io.BufferedInputStream
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object BackupHelper {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Serializable
    data class BackupData(
        val settings: Map<String, String> = emptyMap(),
        val clips: List<Clip> = emptyList()
    )

    fun exportBackupAsJson(
        context: Context,
        uri: Uri,
        clips: List<Clip>,
        prefs: UserPreferences
    ): String {
        return try {
            context.contentResolver.openOutputStream(uri, "wt")?.use { outputStream ->
                val settingsMap = prefs.getAll().mapValues { it.value.toString() }
                val backupData = BackupData(settings = settingsMap, clips = clips)
                val jsonString = json.encodeToString(backupData)
                outputStream.bufferedWriter(Charsets.UTF_8).use { writer ->
                    writer.write(jsonString)
                    writer.flush()
                }
                context.getString(R.string.backup_success_json)
            } ?: context.getString(R.string.backup_failed_no_stream)
        } catch (e: Exception) {
            context.getString(R.string.backup_failed, e.localizedMessage ?: "")
        }
    }

    fun exportBackupAsZip(
        context: Context,
        uri: Uri,
        clips: List<Clip>,
        prefs: UserPreferences
    ): String {
        return try {
            context.contentResolver.openOutputStream(uri, "wt")?.use { outputStream ->
                ZipOutputStream(outputStream).use { zipOut ->
                    val settingsMap = prefs.getAll().mapValues { it.value.toString() }
                    val backupData = BackupData(settings = settingsMap, clips = clips)
                    val jsonString = json.encodeToString(backupData)

                    val entry = ZipEntry("ucopy_backup.json")
                    zipOut.putNextEntry(entry)
                    zipOut.write(jsonString.toByteArray(Charsets.UTF_8))
                    zipOut.closeEntry()

                    val versionEntry = ZipEntry("version.txt")
                    zipOut.putNextEntry(versionEntry)
                    zipOut.write("1.0".toByteArray())
                    zipOut.closeEntry()
                }
                context.getString(R.string.backup_success_zip)
            } ?: context.getString(R.string.backup_failed_no_stream)
        } catch (e: Exception) {
            context.getString(R.string.backup_failed, e.localizedMessage ?: "")
        }
    }

    fun importBackup(context: Context, uri: Uri): ImportResult {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedInputStream(inputStream).use { buffered ->
                    buffered.mark(4)
                    val magic = ByteArray(4)
                    buffered.read(magic)
                    buffered.reset()

                    // Check for ZIP magic bytes (PK..)
                    val isZip = magic[0] == 0x50.toByte() &&
                            magic[1] == 0x4B.toByte() &&
                            magic[2] == 0x03.toByte() &&
                            magic[3] == 0x04.toByte()

                    if (isZip) {
                        importFromZip(context, buffered)
                    } else {
                        importFromJson(context, buffered)
                    }
                }
            } ?: ImportResult.Error(context.getString(R.string.import_could_not_read))
        } catch (e: Exception) {
            ImportResult.Error(context.getString(R.string.import_failed, e.localizedMessage ?: ""))
        }
    }

    private fun importFromJson(context: Context, inputStream: InputStream): ImportResult {
        return try {
            val content = InputStreamReader(inputStream, Charsets.UTF_8).readText()
            if (content.isBlank()) return ImportResult.Error(context.getString(R.string.import_file_empty))
            
            val backupData = json.decodeFromString<BackupData>(content)
            if (backupData.clips.isEmpty() && backupData.settings.isEmpty()) {
                return ImportResult.Error(context.getString(R.string.import_no_data))
            }
            ImportResult.Success(backupData)
        } catch (e: Exception) {
            ImportResult.Error(context.getString(R.string.import_invalid_backup))
        }
    }

    private fun importFromZip(context: Context, inputStream: InputStream): ImportResult {
        return try {
            ZipInputStream(inputStream).use { zipIn ->
                var entry = zipIn.nextEntry
                var jsonContent: String? = null

                while (entry != null) {
                    if (entry.name.endsWith(".json")) {
                        jsonContent = zipIn.readBytes().toString(Charsets.UTF_8)
                        break
                    }
                    entry = zipIn.nextEntry
                }

                if (jsonContent == null) {
                    return ImportResult.Error(context.getString(R.string.import_zip_missing_json))
                }

                val backupData = json.decodeFromString<BackupData>(jsonContent)
                if (backupData.clips.isEmpty() && backupData.settings.isEmpty()) {
                    return ImportResult.Error("This backup file does not contain any data.")
                }
                ImportResult.Success(backupData)
            }
        } catch (e: Exception) {
            ImportResult.Error(context.getString(R.string.import_invalid_zip))
        }
    }

    sealed class ImportResult {
        data class Success(val data: BackupData) : ImportResult()
        data class Error(val message: String) : ImportResult()
    }
}
