package com.example.shade.utils

import android.util.Log
import java.io.File

import java.io.FileOutputStream
import android.net.Uri
import android.content.Context
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import com.example.shade.data.AI_Client.UploadFile

class UploadHelper(private val context: Context) {

    suspend fun processFileUri(uri: Uri) {
        val localFilePath = withContext(Dispatchers.IO) { convertURI(uri) }
        if (localFilePath != null) {
            withContext(Dispatchers.IO) {
                UploadFile(localFilePath) // your existing function
                File(localFilePath).delete() // cleanup
            }
        } else {
            Log.e("UploadHelper", "Failed to create local file for upload")
        }
    }

    private suspend fun convertURI(uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val fileName = getFileName(uri) ?: "upload_${System.currentTimeMillis()}.tmp"
            val tempFile = File(context.cacheDir, fileName)
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }
            tempFile.absolutePath
        } catch (e: Exception) {
            Log.e("UploadHelper", "Error converting URI to file", e)
            null
        }
    }

    private fun getFileName(uri: Uri): String? {
        var fileName: String? = null
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) fileName = it.getString(nameIndex)
            }
        }
        return fileName
    }
}
