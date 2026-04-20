package com.example.precisionlayertesting.data.repository

import com.example.precisionlayertesting.core.utils.Result
import com.example.precisionlayertesting.data.models.bug.*
import com.example.precisionlayertesting.data.remote.BugApiService
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType
import okhttp3.RequestBody
import com.example.precisionlayertesting.data.remote.R2ApiService
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class BugRepository(
    private val apiService: BugApiService,
    private val r2ApiService: R2ApiService
) {

    companion object {
        private const val TAG = "BugRepository"
        private const val MAX_IMAGE_SIZE = 2 * 1024 * 1024 // 2MB
        private const val COMPRESSION_QUALITY = 80
    }

    // ... existing get/create methods ...

    suspend fun prepareScreenshotUploadBatch(request: ScreenshotBatchPrepareRequest): Result<ScreenshotBatchPrepareResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.prepareScreenshotUploadBatch(request)
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string() ?: "Batch preparation failed"
                Result.Error(Exception(errorBody))
            }
        } catch (e: Exception) {
            Log.e(TAG, "prepareScreenshotUploadBatch exception: ${e.message}", e)
            Result.Error(e)
        }
    }

    suspend fun deleteScreenshot(workspaceId: String, filePath: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.deleteR2File(DeleteFileRequest(workspaceId, filePath))
            if (response.isSuccessful) {
                Result.Success(Unit)
            } else {
                val errorBody = response.errorBody()?.string() ?: "Deletion failed"
                Result.Error(Exception(errorBody))
            }
        } catch (e: Exception) {
            Log.e(TAG, "deleteScreenshot exception: ${e.message}", e)
            Result.Error(e)
        }
    }

    /**
     * Copies URI to cache, compresses it, and returns the cached file Uri.
     */
    suspend fun cacheAndCompressImage(context: Context, sourceUri: Uri): Result<Pair<Uri, String>> = withContext(Dispatchers.IO) {
        try {
            val cacheDir = File(context.cacheDir, "bug_drafts").apply { if (!exists()) mkdirs() }
            
            val inputStream = context.contentResolver.openInputStream(sourceUri) ?: throw Exception("Failed to open input stream")
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            if (bitmap == null) throw Exception("Failed to decode bitmap")

            // Prefer JPEG for screenshots
            val outStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, COMPRESSION_QUALITY, outStream)
            var byteArray = outStream.toByteArray()

            // Optional: Downscale if still too large
            if (byteArray.size > MAX_IMAGE_SIZE) {
                // Scaling logic could go here if needed, but quality 80 is usually enough for 2MB
                Log.w(TAG, "Image still large after compression: ${byteArray.size} bytes")
            }

            val fileName = "screenshot_${UUID.randomUUID()}.jpg"
            val cachedFile = File(cacheDir, fileName)
            FileOutputStream(cachedFile).use { it.write(byteArray) }

            Result.Success(Pair(Uri.fromFile(cachedFile), "image/jpeg"))
        } catch (e: Exception) {
            Log.e(TAG, "cacheAndCompressImage exception: ${e.message}", e)
            Result.Error(e)
        }
    }

    suspend fun uploadToR2(
        url: String, 
        data: ByteArray, 
        mimeType: String,
        onProgress: (Long, Long) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val mediaType = MediaType.parse(mimeType)
            val requestBody = ProgressRequestBody(mediaType, data, onProgress)
            
            val response = r2ApiService.uploadFile(
                uploadUrl = url, 
                contentType = mimeType, 
                file = requestBody
            )
            
            if (response.isSuccessful) {
                Result.Success(Unit)
            } else {
                val errorBody = response.errorBody()?.string() ?: "Upload failed"
                Result.Error(Exception(errorBody))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun uploadToR2Stream(
        url: String,
        context: Context,
        uri: Uri,
        mimeType: String,
        onProgress: (Long, Long) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val totalBytes = context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: -1L
            
            val mediaType = MediaType.parse(mimeType)
            val requestBody = StreamingProgressRequestBody(
                context = context,
                uri = uri,
                contentType = mediaType,
                totalBytes = totalBytes,
                onProgress = onProgress
            )
            
            val response = r2ApiService.uploadFile(
                uploadUrl = url,
                contentType = mimeType,
                file = requestBody
            )

            if (response.isSuccessful) {
                Result.Success(Unit)
            } else {
                val errorBody = response.errorBody()?.string() ?: "Streaming upload failed"
                Result.Error(Exception(errorBody))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    // ... existing createTestingSession, submitBugReports, confirmUpload methods ...

    suspend fun getModules(workspaceId: String): List<Module> = withContext(Dispatchers.IO) {
        apiService.getModules("eq.$workspaceId")
    }

    suspend fun getVersions(moduleId: String, workspaceId: String): List<AppVersion> = withContext(Dispatchers.IO) {
        apiService.getVersions("eq.$moduleId", "eq.$workspaceId")
    }

    suspend fun getBugGroups(versionId: String, workspaceId: String): List<TestingSession> = withContext(Dispatchers.IO) {
        apiService.getBugGroups("eq.$versionId", "eq.$workspaceId")
    }

    suspend fun getBugReports(sessionId: String, workspaceId: String, page: Int = 0): List<BugReport> = withContext(Dispatchers.IO) {
        val limit = 20
        val offset = page * limit
        apiService.getBugReports(
            sessionId = "eq.$sessionId",
            workspaceId = "eq.$workspaceId",
            limit = limit,
            offset = offset
        )
    }

    suspend fun getBugDetail(bugId: String): BugReport? = withContext(Dispatchers.IO) {
        apiService.getBugDetail("eq.$bugId").firstOrNull()
    }

    suspend fun createModule(request: ModuleCreateRequest): Result<Module> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.createModule(request = request)
            if (response.isSuccessful) {
                val module = response.body()?.firstOrNull()
                if (module != null) {
                    Result.Success(module)
                } else {
                    Result.Error(Exception("No data returned from server"))
                }
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                Log.e(TAG, "createModule failed: ${response.code()} ${response.message()} - $errorBody")
                Result.Error(Exception("Server error: ${response.code()} $errorBody"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "createModule exception: ${e.message}", e)
            Result.Error(e)
        }
    }

    suspend fun createVersion(request: AppVersionCreateRequest): Result<AppVersion> = withContext(Dispatchers.IO) {
        try {
            // 🔥 IMPORTANT LOGIC: Fetch existing versions to calculate build_number and version_code
            val existingVersions = apiService.getVersions("eq.${request.moduleId}", "eq.${request.workspaceId}", select = "version_name,version_code,build_number")
            
            val sameVersionEntries = existingVersions.filter { it.versionName == request.versionName }
            val isNewVersionName = sameVersionEntries.isEmpty()
            
            val maxBuildNumber = sameVersionEntries.maxOfOrNull { it.buildNumber } ?: 0
            val lastVersionCode = existingVersions.maxOfOrNull { it.versionCode } ?: 0
            
            val finalBuildNumber = if (isNewVersionName) 1 else maxBuildNumber + 1
            val finalVersionCode = if (isNewVersionName) lastVersionCode + 1 else {
                // Same version name, keep the version code from previous entries of this version
                sameVersionEntries.first().versionCode
            }

            val finalRequest = request.copy(
                buildNumber = finalBuildNumber,
                versionCode = finalVersionCode
            )

            Log.d(TAG, "Creating version: ${finalRequest.versionName} (b$finalBuildNumber), Code: $finalVersionCode")

            val response = apiService.createVersion(request = finalRequest)
            if (response.isSuccessful) {
                val version = response.body()?.firstOrNull()
                if (version != null) {
                    Result.Success(version)
                } else {
                    Result.Error(Exception("No data returned from server"))
                }
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                Log.e(TAG, "createVersion failed: ${response.code()} ${response.message()} - $errorBody")
                Result.Error(Exception("Server error: ${response.code()} $errorBody"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "createVersion exception: ${e.message}", e)
            Result.Error(e)
        }
    }

    suspend fun validateApk(request: ApkValidationRequest): Result<ApkValidationResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.validateApk(request)
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string() ?: "Validation failed"
                Result.Error(Exception(errorBody))
            }
        } catch (e: Exception) {
            Log.e(TAG, "validateApk exception: ${e.message}", e)
            Result.Error(e)
        }
    }

    suspend fun uploadToR2(
        url: String, 
        data: ByteArray, 
        onProgress: (Long, Long) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        var lastError: Exception? = null
        val maxRetries = 1
        
        for (attempt in 0..maxRetries) {
            try {
                if (attempt > 0) Log.d(TAG, "Retrying upload... Attempt $attempt")
                
                val mediaType = MediaType.parse("application/vnd.android.package-archive")
                val requestBody = ProgressRequestBody(mediaType, data, onProgress)
                
                // Use a direct PUT request without explicit header if it might conflict with signature
                val response = r2ApiService.uploadFile(
                    uploadUrl = url, 
                    contentType = "application/vnd.android.package-archive", 
                    file = requestBody
                )
                
                if (response.isSuccessful) {
                    Log.d(TAG, "R2 Upload successful")
                    return@withContext Result.Success(Unit)
                } else {
                    val errorCode = response.code()
                    val errorMsg = response.message()
                    val errorBody = response.errorBody()?.string() ?: "No error body"
                    Log.e(TAG, "R2 Upload failed - Code: $errorCode, Msg: $errorMsg, Body: $errorBody")
                    lastError = Exception("Upload failed ($errorCode): $errorMsg - $errorBody")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Upload attempt $attempt failed: ${e.message}", e)
                lastError = e
            }
            
            if (attempt < maxRetries) {
                kotlinx.coroutines.delay(1000L * (attempt + 1))
            }
        }
        
        Result.Error(lastError ?: Exception("Unknown upload error"))
    }

    private class ProgressRequestBody(
        private val contentType: MediaType?,
        private val data: ByteArray,
        private val onProgress: (Long, Long) -> Unit
    ) : RequestBody() {
        override fun contentType(): MediaType? = contentType
        override fun contentLength(): Long = data.size.toLong()

        override fun writeTo(sink: okio.BufferedSink) {
            val total = contentLength()
            var uploaded = 0L
            val bufferSize = 8192
            var offset = 0
            
            while (offset < data.size) {
                val toRead = minOf(bufferSize, data.size - offset)
                sink.write(data, offset, toRead)
                offset += toRead
                uploaded += toRead
                onProgress(uploaded, total)
            }
        }
    }

    private class StreamingProgressRequestBody(
        private val context: Context,
        private val uri: Uri,
        private val contentType: MediaType?,
        private val totalBytes: Long,
        private val onProgress: (Long, Long) -> Unit
    ) : RequestBody() {
        override fun contentType(): MediaType? = contentType
        override fun contentLength(): Long = totalBytes

        override fun writeTo(sink: okio.BufferedSink) {
            val buffer = ByteArray(8192)
            var bytesRead: Int
            var uploaded = 0L
            
            context.contentResolver.openInputStream(uri)?.use { input ->
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    sink.write(buffer, 0, bytesRead)
                    uploaded += bytesRead
                    onProgress(uploaded, totalBytes)
                }
            } ?: throw java.io.IOException("Failed to open input stream for URI: $uri")
        }
    }

    suspend fun createTestingSession(request: TestingSessionCreateRequest): Result<TestingSession> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.createTestingSession(request = request)
            if (response.isSuccessful) {
                val session = response.body()?.firstOrNull()
                if (session != null) {
                    Result.Success(session)
                } else {
                    Result.Error(Exception("No session data returned"))
                }
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                Result.Error(Exception("Session creation failed: $errorBody"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun submitBugReports(reports: List<BugReportCreateRequest>): Result<List<BugReport>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.createBugReports(request = reports)
            if (response.isSuccessful) {
                val data = response.body()
                if (data != null) {
                    Result.Success(data)
                } else {
                    Result.Error(Exception("No reporting data returned"))
                }
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                Result.Error(Exception("Bug submission failed: $errorBody"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun confirmUpload(request: ConfirmUploadRequest): Result<AppVersion> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.confirmUpload(request)
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string() ?: "Confirmation failed"
                Result.Error(Exception(errorBody))
            }
        } catch (e: Exception) {
            Log.e(TAG, "confirmUpload exception: ${e.message}", e)
            Result.Error(e)
        }
    }
}
