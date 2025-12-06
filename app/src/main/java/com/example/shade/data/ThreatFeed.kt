package com.example.shade.data

import kotlinx.coroutines.Dispatchers // Used for background thread
import kotlinx.coroutines.withContext // needed to run network call without disrupting Main Activity
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import okhttp3.ResponseBody



import android.util.Log // Logging errors
import retrofit2.Retrofit // making request
import retrofit2.http.GET // request type
import okhttp3.MediaType.Companion.toMediaType // Convert type to a Media Type
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory // converter factory tells Retrofit how to turn the HTTP response body into a Kotlin object.

//  properties of JSON Object we are parsing
@Serializable //
data class Threat(
    val url: String = "",
    val url_status: String = "",
    val threat: String = "",
    val urlhaus_link: String = ""
)


// ja3_md5,Firstseen,Lastseen,Listingreason
@Serializable
data class JA3Fingerprint(
    val md5: String,
    val Firstseen: String,
    val Lastseen: String,
    val Listingreason: String
)

@Serializable
data class MalwareSample(
    val sha256: String,
    val fileType: String,
    val signature: String,
    val firstSeen: String
)



public interface UrlHausApi {
    @GET("downloads/json_online/")
    suspend fun getThreats():  okhttp3.ResponseBody
}

public interface JAFingerprintsAPI {
    @GET("blacklist/ja3_fingerprints.csv")
    suspend fun getFingerprints(): okhttp3.ResponseBody
}

public interface  MalwareBazaarApi {
    @GET("export/csv/recent/")
    suspend fun getHashes(): ResponseBody
}



object UrlHausFeed {
    private val baseUrl: String = "https://urlhaus.abuse.ch/"
    const val tag = "URLHausFeed"

    // JSON parser with lenient settings
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .build()
    }

    private val api by lazy { retrofit.create(UrlHausApi::class.java) } // retrofit dynamically geenrates api url with value defined in interface


    // returning Returns a List of Threat objects
    suspend fun fetchThreats(): List<Threat> = withContext(Dispatchers.IO) {

        // 1️⃣ Network call
        val rawJson: String = try {
            api.getThreats().string()
        } catch (e: Exception) {
            Log.e(tag, "Network call to URLhaus failed", e)
            return@withContext emptyList()   // exit early if the HTTP call itself failed
        }

        Log.d(tag, "Network call succeeded, JSON size = ${rawJson.length}")

        // 2️⃣ JSON decoding
        try {
            val jsonObj = json.parseToJsonElement(rawJson).jsonObject

            // Flatten all nested arrays into a single List<Threat>
            val allThreats = jsonObj.values.flatMap { jsonArray ->
                jsonArray.jsonArray.map { element ->
                    json.decodeFromJsonElement<Threat>(element)
                }
            }

            Log.d(tag, "Fetched ${allThreats.size} threats")
            allThreats       // Flatten to a single List<Threat>
        } catch (e: Exception) {
            Log.e(tag, "JSON parsing failed", e)
            emptyList()
        }
    }


        // breaks down url and extracts ip
    fun extractIpFromUrl(url: String): String? {
        return try {
            val uri = android.net.Uri.parse(url)
            val ip = uri.host // gives the  IP
            return ip
        } catch (e: Exception) {
            Log.e(tag, "Failed to extract IP from $url", e)
            null
        }
    }

}

object  JA3Feed {
    const val tag = "JA3"
    const val baseURL = "https://sslbl.abuse.ch/"

    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(baseURL)
            .build()
    }

    private val api by lazy {retrofit.create(JAFingerprintsAPI::class.java)}

    suspend fun fetchFingerprints(): List<JA3Fingerprint> =  withContext(Dispatchers.IO) {
        try {
            val fingerprintCSV = api.getFingerprints().string()
                .lineSequence()
                .filter { line -> line.isNotBlank() && !line.trimStart().startsWith("#") }
                .map { line ->
                    val cols = line.split(',')
                    // defensive check in case of short lines
                    JA3Fingerprint(
                        md5 = cols.getOrElse(0) { "" },
                        Firstseen = cols.getOrElse(1) { "" },
                        Lastseen = cols.getOrElse(2) { "" },
                        Listingreason = cols.getOrElse(3) { "" }
                    )
                }
                .toList()
            Log.i(tag, "Fetched ${fingerprintCSV.size} signatures ")
            fingerprintCSV
        }
        catch( e: Exception){
            Log.e(tag, "Failed to Fetch JA3 fingerprints", e)
            e.printStackTrace()
            emptyList()
        }
    }
}

object MalwareBazaarFeed {

    const val tag = "MalwareBazaar"
    private const val baseURL = "https://bazaar.abuse.ch/"

    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(baseURL)
            .build()
    }

    private val api by lazy { retrofit.create(MalwareBazaarApi::class.java) }


    suspend fun fetchApkHashes(): List<MalwareSample> = withContext(Dispatchers.IO) {
        try {
            val rawCsv = api.getHashes().string()


            rawCsv
                .lineSequence()
                .filter { it.isNotBlank() && !it.trimStart().startsWith("#") }
                .mapNotNull { line ->
                    val cols = parseCsvLine(line)
                    if (cols.size < 8) return@mapNotNull null
                    val fileType = cols[6].replace("\"", "").lowercase()
                    if (fileType != "apk" && fileType != "xapk") return@mapNotNull null

                    Log.i(tag, "Adding APK: ${cols.getOrElse(5){ "unknown" }}, fileType=$fileType")
                    MalwareSample(
                        sha256 = cols[1],
                        fileType = fileType,
                        signature = cols.getOrElse(7) { "" },
                        firstSeen = cols[0]
                    )
                }
                .toList()

        } catch (e: Exception) {
            Log.e(tag, "Failed to fetch CSV", e)
            emptyList()
        }
    }
}

private fun parseCsvLine(line: String): List<String> {
    val result = mutableListOf<String>()
    var inQuotes = false
    val sb = StringBuilder()
    var i = 0

    while (i < line.length) {
        val c = line[i]
        when {
            c == '"' -> {
                if (i + 1 < line.length && line[i + 1] == '"') { // escaped quote
                    sb.append('"')
                    i++ // skip next quote
                } else {
                    inQuotes = !inQuotes
                }
            }
            c == ',' && !inQuotes -> {
                result.add(sb.toString().trim())
                sb.clear()
            }
            else -> sb.append(c)
        }
        i++
    }
    result.add(sb.toString().trim())
    return result
}
