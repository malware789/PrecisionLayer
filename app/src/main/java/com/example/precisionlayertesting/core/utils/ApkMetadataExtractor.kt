package com.example.precisionlayertesting.core.utils

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileOutputStream

data class ApkMetadata(
    val packageName: String,
    val versionName: String,
    val versionCode: Int
)

class ApkMetadataExtractor(private val context: Context) {

    companion object {
        private const val TAG = "ApkMetadataExtractor"
    }

    /**
     * Extracts metadata from an APK file.
     * @param uri The Uri of the selected APK file.
     * @return ApkMetadata if successful, null otherwise.
     */

    fun extract(uri: Uri): ApkMetadata? {
        var tempFile: File? = null
        return try {
            // PackageManager needs a file path, so we copy the Uri content to a temp file
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            tempFile = File(context.cacheDir, "temp_apk_extraction.apk")
            val outputStream = FileOutputStream(tempFile)
            inputStream.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }

            val packageInfo = context.packageManager.getPackageArchiveInfo(
                tempFile.absolutePath,
                0
            ) ?: return null

            ApkMetadata(
                packageName = packageInfo.packageName,
                versionName = packageInfo.versionName ?: "unknown",
                versionCode = packageInfo.versionCode
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract metadata from APK: ${e.message}", e)
            null
        } finally {
            tempFile?.delete()
        }
    }
}
