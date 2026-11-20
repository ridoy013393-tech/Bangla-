package com.example.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.BuildConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okio.BufferedSink
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit

data class UploadedMedia(
    val downloadUrl: String,
    val fileName: String,
    val sizeBytes: Long,
    val mimeType: String
)

class StorageRepository(context: Context) {
    private val appContext = context.applicationContext

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    companion object {
        const val MAX_IMAGE_BYTES = 5L * 1024L * 1024L      // 5 MB
        const val MAX_DOCUMENT_BYTES = 10L * 1024L * 1024L  // 10 MB

        private const val PLACEHOLDER_CLOUD_NAME = "YOUR_CLOUDINARY_CLOUD_NAME"
        private const val PLACEHOLDER_UPLOAD_PRESET = "YOUR_CLOUDINARY_UPLOAD_PRESET"
        private const val FIRESTORE_URL_MAX_LENGTH = 500

        private val ALLOWED_DOC_MIMES = setOf(
            "application/pdf",
            "text/plain",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        )
    }

    suspend fun uploadProfilePhoto(
        uid: String,
        uri: Uri,
        onProgress: (Float) -> Unit
    ): Result<UploadedMedia> = uploadToCloudinary(
        uri = uri,
        folder = "bangla_chat/profile_photos/$uid",
        maxBytes = MAX_IMAGE_BYTES,
        imageOnly = true,
        onProgress = onProgress
    )

    suspend fun uploadGroupPhoto(
        creatorUid: String,
        uri: Uri,
        onProgress: (Float) -> Unit
    ): Result<UploadedMedia> = uploadToCloudinary(
        uri = uri,
        folder = "bangla_chat/group_photos/$creatorUid",
        maxBytes = MAX_IMAGE_BYTES,
        imageOnly = true,
        onProgress = onProgress
    )

    suspend fun uploadStatusImage(
        uid: String,
        uri: Uri,
        onProgress: (Float) -> Unit
    ): Result<UploadedMedia> = uploadToCloudinary(
        uri = uri,
        folder = "bangla_chat/statuses/$uid",
        maxBytes = MAX_IMAGE_BYTES,
        imageOnly = true,
        onProgress = onProgress
    )

    suspend fun uploadChatMedia(
        chatId: String,
        senderUid: String,
        uri: Uri,
        isImage: Boolean,
        onProgress: (Float) -> Unit
    ): Result<UploadedMedia> = uploadToCloudinary(
        uri = uri,
        folder = "bangla_chat/chat_media/$chatId/$senderUid",
        maxBytes = if (isImage) MAX_IMAGE_BYTES else MAX_DOCUMENT_BYTES,
        imageOnly = isImage,
        onProgress = onProgress
    )

    suspend fun uploadVoiceNote(
        chatId: String,
        senderUid: String,
        audioFile: File,
        onProgress: (Float) -> Unit
    ): Result<UploadedMedia> = withContext(Dispatchers.IO) {
        try {
            val cloudName = BuildConfig.CLOUDINARY_CLOUD_NAME.trim()
            val uploadPreset = BuildConfig.CLOUDINARY_UPLOAD_PRESET.trim()
            if (cloudName.isBlank() || cloudName == PLACEHOLDER_CLOUD_NAME ||
                uploadPreset.isBlank() || uploadPreset == PLACEHOLDER_UPLOAD_PRESET
            ) {
                return@withContext Result.failure(IllegalStateException("Cloudinary upload preset is not configured"))
            }
            val size = audioFile.length()
            if (!audioFile.exists() || size <= 0L) {
                return@withContext Result.failure(IOException("Recorded audio file is empty"))
            }
            if (size > MAX_DOCUMENT_BYTES) {
                return@withContext Result.failure(IllegalArgumentException("Voice note exceeds 10MB limit"))
            }

            val mediaType = "audio/mp4".toMediaTypeOrNull()
            val progressBody = ProgressFileRequestBody(
                file = audioFile,
                mediaType = mediaType,
                onProgress = onProgress
            )
            val fileName = "voice_${System.currentTimeMillis()}.m4a"
            val multipartBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("upload_preset", uploadPreset)
                .addFormDataPart("folder", "bangla_chat/voice_notes/$chatId/$senderUid")
                .addFormDataPart("file", fileName, progressBody)
                .build()

            // Cloudinary handles audio uploads under the "video" resource endpoint
            val endpoint = "https://api.cloudinary.com/v1_1/$cloudName/video/upload"
            val request = Request.Builder()
                .url(endpoint)
                .post(multipartBody)
                .build()

            httpClient.newCall(request).execute().use { response ->
                val rawBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val cloudError = runCatching {
                        JSONObject(rawBody).optJSONObject("error")?.optString("message")
                    }.getOrNull()?.takeIf { it.isNotBlank() }
                    return@withContext Result.failure(
                        IOException(cloudError ?: "Voice upload failed (HTTP ${response.code})")
                    )
                }
                val secureUrl = JSONObject(rawBody).optString("secure_url").trim()
                if (!secureUrl.startsWith("https://")) {
                    return@withContext Result.failure(IOException("Invalid upload response from Cloudinary"))
                }
                onProgress(1f)
                Result.success(
                    UploadedMedia(
                        downloadUrl = secureUrl,
                        fileName = fileName,
                        sizeBytes = size,
                        mimeType = "audio/mp4"
                    )
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            runCatching { audioFile.delete() }
        }
    }

    private suspend fun uploadToCloudinary(
        uri: Uri,
        folder: String,
        maxBytes: Long,
        imageOnly: Boolean,
        onProgress: (Float) -> Unit
    ): Result<UploadedMedia> = withContext(Dispatchers.IO) {
        var tempFile: File? = null
        try {
            val cloudName = BuildConfig.CLOUDINARY_CLOUD_NAME.trim()
            val uploadPreset = BuildConfig.CLOUDINARY_UPLOAD_PRESET.trim()

            if (cloudName.isBlank() || cloudName == PLACEHOLDER_CLOUD_NAME ||
                uploadPreset.isBlank() || uploadPreset == PLACEHOLDER_UPLOAD_PRESET
            ) {
                return@withContext Result.failure(
                    IllegalStateException(
                        "Cloudinary সেটআপ করা হয়নি। AI Studio-র Secrets প্যানেলে CLOUDINARY_CLOUD_NAME এবং CLOUDINARY_UPLOAD_PRESET যোগ করুন।"
                    )
                )
            }

            val resolver = appContext.contentResolver
            val mimeType = resolver.getType(uri)
                ?: if (imageOnly) "image/jpeg" else "application/pdf"

            if (imageOnly && !mimeType.startsWith("image/")) {
                return@withContext Result.failure(IllegalArgumentException("Only image files are supported"))
            }
            if (!imageOnly && !mimeType.startsWith("image/") && mimeType !in ALLOWED_DOC_MIMES) {
                return@withContext Result.failure(IllegalArgumentException("Unsupported file type: $mimeType"))
            }

            val (fileName, queriedSize) = queryFileMeta(uri, imageOnly)
            if (queriedSize > maxBytes) {
                val limitMb = maxBytes / (1024 * 1024)
                return@withContext Result.failure(IllegalArgumentException("File exceeds ${limitMb}MB limit"))
            }

            // Copy URI content to a size-capped temp file for accurate progress and OOM safety
            val staged = File(appContext.cacheDir, "upload_${UUID.randomUUID()}.tmp")
            tempFile = staged
            val actualSize = copyUriToFileWithLimit(uri, staged, maxBytes)
            if (actualSize <= 0L) {
                return@withContext Result.failure(IOException("Selected file is empty or unreadable"))
            }

            val mediaType = mimeType.toMediaTypeOrNull() ?: "application/octet-stream".toMediaTypeOrNull()
            val progressBody = ProgressFileRequestBody(
                file = staged,
                mediaType = mediaType,
                onProgress = onProgress
            )

            val multipartBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("upload_preset", uploadPreset)
                .addFormDataPart("folder", folder)
                .addFormDataPart("file", fileName, progressBody)
                .build()

            val resourceType = if (mimeType.startsWith("image/")) "image" else "auto"
            val endpoint = "https://api.cloudinary.com/v1_1/$cloudName/$resourceType/upload"

            val request = Request.Builder()
                .url(endpoint)
                .post(multipartBody)
                .build()

            httpClient.newCall(request).execute().use { response ->
                val rawBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val cloudError = runCatching {
                        JSONObject(rawBody).optJSONObject("error")?.optString("message")
                    }.getOrNull()?.takeIf { it.isNotBlank() }
                    return@withContext Result.failure(
                        IOException(cloudError ?: "Cloudinary upload failed (HTTP ${response.code})")
                    )
                }

                val json = JSONObject(rawBody)
                val secureUrl = json.optString("secure_url").trim()
                if (!secureUrl.startsWith("https://")) {
                    return@withContext Result.failure(IOException("Invalid upload response from Cloudinary"))
                }
                if (secureUrl.length > FIRESTORE_URL_MAX_LENGTH) {
                    return@withContext Result.failure(IOException("Uploaded media URL exceeds supported length"))
                }

                onProgress(1f)
                Result.success(
                    UploadedMedia(
                        downloadUrl = secureUrl,
                        fileName = fileName,
                        sizeBytes = actualSize,
                        mimeType = mimeType
                    )
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            runCatching { tempFile?.delete() }
        }
    }

    private fun copyUriToFileWithLimit(uri: Uri, destination: File, maxBytes: Long): Long {
        val input = appContext.contentResolver.openInputStream(uri)
            ?: throw IOException("Unable to open selected file")
        var total = 0L
        input.use { stream ->
            FileOutputStream(destination).use { out ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val read = stream.read(buffer)
                    if (read == -1) break
                    total += read
                    if (total > maxBytes) {
                        val limitMb = maxBytes / (1024 * 1024)
                        throw IllegalArgumentException("File exceeds ${limitMb}MB limit")
                    }
                    out.write(buffer, 0, read)
                }
                out.flush()
            }
        }
        return total
    }

    private fun queryFileMeta(uri: Uri, isImage: Boolean): Pair<String, Long> {
        var name = if (isImage) "photo_${System.currentTimeMillis()}.jpg" else "document.pdf"
        var size = 0L
        appContext.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst()) {
                if (nameIdx >= 0) {
                    cursor.getString(nameIdx)?.takeIf { it.isNotBlank() }?.let { name = it }
                }
                if (sizeIdx >= 0) {
                    size = cursor.getLong(sizeIdx)
                }
            }
        }
        return name.take(180) to size
    }

    private class ProgressFileRequestBody(
        private val file: File,
        private val mediaType: MediaType?,
        private val onProgress: (Float) -> Unit
    ) : RequestBody() {
        override fun contentType(): MediaType? = mediaType
        override fun contentLength(): Long = file.length()

        override fun writeTo(sink: BufferedSink) {
            val totalBytes = file.length().coerceAtLeast(1L)
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var uploaded = 0L
            var lastReported = -1f
            FileInputStream(file).use { input ->
                while (true) {
                    val read = input.read(buffer)
                    if (read == -1) break
                    sink.write(buffer, 0, read)
                    uploaded += read
                    val fraction = (uploaded.toFloat() / totalBytes.toFloat()).coerceIn(0f, 0.98f)
                    if (fraction - lastReported >= 0.02f) {
                        lastReported = fraction
                        onProgress(fraction)
                    }
                }
            }
        }
    }
}
