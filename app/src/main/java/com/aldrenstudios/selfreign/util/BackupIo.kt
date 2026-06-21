package com.aldrenstudios.selfreign.util

import android.content.Context
import android.net.Uri

/**
 * Robust file I/O for backup export/import over Storage Access Framework URIs.
 * Every operation is wrapped so a failure (permission revoked, file gone, bad
 * encoding) returns a safe value instead of crashing the app.
 */
object BackupIo {

    /** Writes [content] to [uri]. Returns true on success. */
    fun writeToUri(context: Context, uri: Uri, content: String): Boolean = try {
        context.contentResolver.openOutputStream(uri)?.use { out ->
            out.write(content.toByteArray(Charsets.UTF_8))
            out.flush()
        } != null
    } catch (e: Exception) {
        false
    }

    /** Reads the full text content of [uri]. Returns null on any failure. */
    fun readFromUri(context: Context, uri: Uri): String? = try {
        context.contentResolver.openInputStream(uri)?.use { input ->
            input.readBytes().toString(Charsets.UTF_8)
        }
    } catch (e: Exception) {
        null
    }
}
