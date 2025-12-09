package com.example.shade.data

import android.util.Log
import  okhttp3.*
import okhttp3.OkHttpClient
import okhttp3.MultipartBody
import okhttp3.Request
import org.json.JSONObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import android.webkit.MimeTypeMap
import okhttp3.RequestBody.Companion.toRequestBody
import com.example.shade.utils.MalwareScanResponse
import com.example.shade.utils.Indicators
import com.example.shade.utils.toThreatItem
import com.example.shade.utils.ThreatItem
import com.example.shade.ui.theme.ScanCallbacks
import com.example.shade.ThreatsList.activeThreats
import com.example.shade.BuildConfig


const val tag = "AI_Client"
const val ip = BuildConfig.SERVER_IP
object AI_Client {
    val client = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(180, java.util.concurrent.TimeUnit.SECONDS) // <-- important
        .callTimeout(0, java.util.concurrent.TimeUnit.SECONDS)  // unlimited
        .build()
    val url = "http://$ip:5000//analyze/upload"
    val hashUrl = "http://$ip:5000/check_hash"

    fun UploadAndScanFile(filePath: String): ThreatItem? {
        val file = File(filePath)
        if (!file.exists()) {
            Log.e(tag, "File does not exist at $filePath")
            return null
        }

        val mediaType = getMimeType(file)?.toMediaType()
        val fileRequestBody = file.asRequestBody(mediaType)

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", file.name, fileRequestBody)
            .build()

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        try {
            Log.i(tag, "making a request to $url")
            client.newCall(request).execute().use() { response ->
                if (!response.isSuccessful) {
                    Log.e(tag, "Failed to upload file to server")
                    Log.i(tag, ("${response.code} : ${response.message} "))
                    return null
                } else {
                    Log.i(tag, "Successfully uplaoded file to Server")
                    val report = response.body.string()
                    val parsed_report = parseMalwareReport(report)

                    val result = parsed_report.toThreatItem(file.name)
                    activeThreats.add(result)
                    ScanCallbacks.onScanCompleted?.invoke(result)
                    return result
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed Request", e)
             return null
        }
    }

    fun UploadFile(filePath: String) {
        Log.i(tag, "Hash Upload starting")
        val file = File(filePath)
        if(!file.exists()) {
            Log.e(tag, "File does not exist at $filePath")
         return
        }
        val sha256 = getSha256(file)

        val json = """
        {"hash": "$sha256"}
    """.trimIndent()

        Log.i(tag, "sending request body $json")
        val requestBody = json.toRequestBody("application/json".toMediaType())




        val request = Request.Builder()
            .url(hashUrl)
            .post(requestBody)
            .build()

         try {
            Log.i(tag, "making a request to $hashUrl")

            client.newCall(request).execute().use() { response ->
                val responseText = response.body.string()
                if (!response.isSuccessful) {
                    Log.e(tag, "Failed to upload file to server")
                    Log.i(tag, ("${response.code} : ${response.message} "))
                }
                Log.i(tag, responseText)
                try {
                val json = JSONObject(responseText)
                val status = json.getString("status")

                when (status) {
                    "KNOWN_SAFE" -> {
                        Log.i(tag, "SAFE: No need to upload file.")
                    }

                    "KNOWN_MALWARE" -> {
                        Log.i(tag, "MALWARE: No need to upload file.")
                    }

                    "UNKNOWN" -> {
                        Log.i(tag, "Hash not recognized. Uploading file...")
                       UploadAndScanFile(filePath)
                    }
                }
            } catch (e: Exception) {
                Log.e(tag, "Failed to parse JSON hash response", e)
        }
        }
    } catch( e: Exception) {
        Log.e(tag, "Failed Request", e)
    }
}

}

fun getMimeType(file: File): String? {
    val extension = MimeTypeMap.getFileExtensionFromUrl(file.path)
    return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase())
}

fun getSha256(file: File): String {
    Log.i(tag, "getting sha-256")
    val messageDigest = MessageDigest.getInstance("SHA-256") //message digest object
    val buffer = ByteArray(1024) //array to read into
    val fileInputStream = FileInputStream(file)  // FileInputStream reads raw bytes from the file

    try {
        var bytesRead = fileInputStream.read(buffer)
        while (bytesRead != -1) {
            messageDigest.update(buffer, 0, bytesRead) // Feed the chunk into the digest
            bytesRead = fileInputStream.read(buffer) //read next chunk
        }
    } catch (e: Exception) {
        Log.e("sha256", "failed to get sha256 of file", e)
    } finally {
        fileInputStream.close()
    }
    Log.i(tag, "message digest finished")
    val hashBytes = messageDigest.digest()
    Log.i(tag, "hash function finished")
    return hashBytes.joinToString("") { "%02x".format(it) } // converting hash bytes to string
}

fun parseMalwareReport(jsonText: String): MalwareScanResponse {
    val json = JSONObject(jsonText)

    val indicatorsObj = json.getJSONObject("indicators")

    return MalwareScanResponse(
        malware_confidence = json.getDouble("malware_confidence"),
        verdict = json.getString("verdict"),
        decision_threshold = json.getInt("decision_threshold"),
        summary = json.getString("summary"),
        analysis_log = json.getJSONArray("analysis_log")
            .let { arr ->
                List(arr.length()) { i -> arr.getString(i) }
            },
        indicators = Indicators(
            ips = indicatorsObj.getJSONArray("ips")
                .let { arr -> List(arr.length()) { i -> arr.getString(i) } },
            urls = indicatorsObj.getJSONArray("urls")
                .let { arr -> List(arr.length()) { i -> arr.getString(i) } }
        ),
        key_risks = json.getJSONArray("key_risks")
            .let { arr -> List(arr.length()) { i -> arr.getString(i) } },
        full_report_text = json.getString("full_report_text")
    )
}
