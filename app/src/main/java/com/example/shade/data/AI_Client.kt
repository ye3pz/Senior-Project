package com.example.shade.data

import android.util.Log
import  okhttp3.*
import okhttp3.OkHttpClient
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import android.webkit.MimeTypeMap


object AI_Client {
    const val tag = "AI_Client"
    val   client =  OkHttpClient()
    val url = "http://192.168.105.59:5000/api/scan"

    fun UploadAndScanFile(filePath: String){
        val file = File(filePath)
         if(!file.exists()) {
            Log.e(tag, "File does not exist at $filePath")
             return
        }

        val mediaType = getMimeType(file)?.toMediaTypeOrNull()
        val fileRequestBody = file.asRequestBody(mediaType)

        val requestBody = MultipartBody.Builder()
            .setType (MultipartBody.FORM)
            .addFormDataPart("Uploading file",file.name, fileRequestBody)
            .build()

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        try {
            Log.i(tag, "making a request to $url")
            client.newCall(request).execute().use(){response ->
                if(!response.isSuccessful){
                    Log.e(tag, "Failed to upload file to server")
                    println("${response.code} : ${response.message} ")
                }else {
                    Log.i(tag, "Successfully uplaoded file to Server")
                    print(response.body.toString())
                }
            }
        } catch( e: Exception){
            Log.e(tag, "Failed Request",e)
        }
    }

}

fun getMimeType(file: File): String? {
    val extension = MimeTypeMap.getFileExtensionFromUrl(file.path)
    return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase())
}