package com.example.shade.data

import android.content.Context
import android.util.Log
import com.example.shade.data.ThreatFeed.extractIpFromUrl
import com.google.firebase.FirebaseApp
import com.google.firebase.database.FirebaseDatabase

object FirebaseClient {
   const val tag = "Firebase"
    private var initialized = false
    // Lazy initialization of Realtime Database reference
    val db: FirebaseDatabase by lazy {
        FirebaseDatabase.getInstance().apply {
            //  keep local data in sync with server
            setPersistenceEnabled(true)
        }
    }

    /**
     * Initialize Firebase if it hasn’t been initialized yet.
     * Call this from Application class or MainActivity.onCreate()
     */
    fun init(context: Context) {
        if (!initialized) {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
                Log.i(tag, "FirebaseApp initialized manually")
            } else {
                Log.i(tag, "FirebaseApp already initialized")
            }

            createSchema() // create schema only if missing
        }
    }
    private fun createSchema(){
        // creating structure of database similar to tables in MYSQL or SQlite
        val schema = mapOf(
            "signatures" to mapOf (
                "trusted" to mapOf<String, Any?>(),
                "untrusted " to mapOf<String, Any?>(),
         ),
            "ips" to mapOf(
                "trusted" to mapOf<String, Any?>(),
                "untrusted" to mapOf<String, Any?>(),
            ),
            "apps" to mapOf(
                "trusted" to mapOf<String, Any?>(),
                "untrusted" to mapOf<String, Any?>(),
            ),
            "scanResults" to mapOf<String, Any?>()
            )

        // Only write schema if database is empty
        db.reference.get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.exists()) {
                    db.reference.updateChildren(schema)
                        .addOnSuccessListener {
                            initialized = true
                            Log.i(tag, "✅ Schema created in Firebase")
                        }
                        .addOnFailureListener { e ->
                            Log.e(tag, "❌ Failed to create schema", e)
                        }
                } else {
                    Log.i(tag, "Schema already exists, skipping creation")
                }
            }
            .addOnFailureListener { e ->
                Log.e(tag, "Failed to check that schema exists", e)
            }
    }


    fun addSignature(){

    }
    fun addIp(ipAddress: String, trusted: Boolean = false) {
        val ipKey = ipAddress.replace(".", "_").trim()
        val category = if (trusted) "trusted" else "untrusted"

        val ref = db.getReference("ips").child(category).child(ipKey)
        ref.setValue(true)
            .addOnSuccessListener {
                Log.i(tag, "IP $ipAddress added to $category list")
            }
            .addOnFailureListener { e ->
                Log.e(tag, "Failed to add IP $ipAddress to $category list: $e")
            }
    }

    fun checkIpUntrusted(ipAddress: String) {
        val ipKey = ipAddress.replace(".", "_").trim()
        val ref = db.getReference("ips/untrusted").child(ipKey)

        ref.get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    Log.i("FirebaseClient", "✅IP $ipAddress is in the untrusted list")
                } else {
                    Log.w("FirebaseClient", "⚠IP $ipAddress not found in untrusted list")
                }
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseClient", "❌ Failed to check IP $ipAddress", e)
            }
    }
    fun getUntrustedIps(onResult: (List<String>) -> Unit){
        // returning Unit, UNit i similar to void
        val ref = db.getReference("ips/untrusted")
        ref.get()
            .addOnSuccessListener { snapshot ->
                val ips = snapshot.children.mapNotNull { it.key?.replace("_", ".") }
                onResult(ips) // call back function
            }
            .addOnFailureListener { e ->
                Log.e(tag, "Failed to fetch untrusted IPs", e)
                onResult(emptyList())
            }
    }
    suspend fun updateFirebaseWithThreats() {
        try {
            // 1. Fetch all threats from URLhaus (this returns List<Threat>)
            val threats: List<Threat> = ThreatFeed.fetchThreats()

            // 2. Loop through each threat, extract an IP, and upload details
            for (threat in threats) {
                // Use your helper function to pull an IP from the URL
                val ip: String? = extractIpFromUrl(threat.url)
                if (ip.isNullOrEmpty()) {
                    Log.w(tag, "No IP found for ${threat.url}")
                    continue
                }

                val ipKey = ip.replace(".", "_") // Firebase-safe key

                // Metadata map for this IP
                val ipDetails = mapOf(
                    "url" to threat.url,
                    "url_status" to threat.url_status,
                    "threat" to threat.threat,
                    "urlhaus_link" to threat.urlhaus_link
                )

                // Write to Firebase under ips/untrusted
                db.getReference("ips/untrusted")
                    .child(ipKey)
                    .setValue(ipDetails)
                    .addOnSuccessListener {
                        Log.i(tag, "Added IP $ip to Firebase")
                    }
                    .addOnFailureListener { e ->
                        Log.e(tag, "Failed to add IP $ip", e)
                    }
            }
        } catch(e: Exception ){
            Log.e(tag, "failed to update  Firebase with theats" , e)
            e.printStackTrace()

        }
    }

    suspend fun updateFirebaseWithUntrustedSignatures() {
        try{
            val signatures = ThreatFeed.JA3.fetchFingerprints()
            if(signatures.isNullOrEmpty()){
                Log.e(tag, "Get Request returned an EmptyList")
                return
            }
            for (signature in signatures) {

                val sigKey = signature.md5

                val signatureDetails = {
                    "Firstseen" to signature.Lastseen
                    "Lastseen" to signature.Firstseen
                    "Listingreason" to signature.Listingreason

                }
                db.getReference("signatures/untrusted")
                    .child(sigKey)
                    .setValue(signatureDetails)
                    .addOnSuccessListener {
                        Log.i(tag, "added signature $sigKey to Firebase")
                    }
                    .addOnFailureListener {
                        Log.e(tag, "Failed to add $sigKey to Firebase")
                    }

            }
        } catch(e: Exception){
            Log.e(tag, "Faield to update Firebase with Signatures, e")
        }
    }


    fun addApp(){

    }
}

