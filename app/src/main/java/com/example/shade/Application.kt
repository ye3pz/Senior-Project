package com.example.shade

import android.app.Application
import com.example.shade.data.FirebaseClient
import android.util.Log
import com.example.shade.data.UrlHausFeed
import androidx.work.*
import com.example.shade.workers.UpdateThreatsWorker
import java.util.concurrent.TimeUnit


class Shade: Application(){
    val tag = "ApplicationWorker "
    override fun onCreate() {
        super.onCreate()


        //init Firebase
        try {
            FirebaseClient.init(applicationContext)
        } catch (e: Exception) {
            Log.e(FirebaseClient.tag, "Firebase initialization error", e)
        }

        try {

           val threatUpdateRequest =
                PeriodicWorkRequestBuilder<UpdateThreatsWorker>(
                    24, TimeUnit.HOURS)
                    .setConstraints(
                        Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .build()
                    )
                    .build()

            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "ThreatUpdate",
                ExistingPeriodicWorkPolicy.KEEP,   // keep the old one if it’s already scheduled
                threatUpdateRequest
            )



                 /*
                //For testing

            val testRequest = OneTimeWorkRequestBuilder<UpdateThreatsWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)

                        .build()
                )
                .build()

            WorkManager.getInstance(this)
                .enqueueUniqueWork(
                    "ThreatUpdateTest",
                    ExistingWorkPolicy.REPLACE,
                    testRequest

            )
            */
                        //For testing


        } catch(e: Exception){
            Log.e(tag, "Failed to schedule ThreatUpdateWorker", e)
        }
    }
}