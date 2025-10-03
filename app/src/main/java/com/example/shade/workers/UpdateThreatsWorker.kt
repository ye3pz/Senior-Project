package com.example.shade.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.shade.data.FirebaseClient
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlin.system.measureTimeMillis

class UpdateThreatsWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    private val tag = "UpdateThreatsWorker"

    override suspend fun doWork(): Result = coroutineScope {
        try {
            // Launch the three updates in parallel
            val jobs = listOf(
                async {
                    val time = measureTimeMillis {
                        Log.i(tag, "Starting updateFirebaseWithThreats()")
                        FirebaseClient.updateFirebaseWithThreats()
                    }
                        Log.i(tag, "Finished updateFirebaseWithThreats() in ${time}ms")

                },
                async {
                    val time = measureTimeMillis {
                        Log.i(tag, "Starting updateFirebaseWithUntrustedSignatures()")
                        FirebaseClient.updateFirebaseWithUntrustedSignatures()
                    }
                        Log.i(tag, "Finished updateFirebaseWithUntrustedSignatures() in ${time}ms")

                },
                async {
                    val time  = measureTimeMillis {
                        Log.i(tag, "Starting updateFirebaseWithHashes()")
                        FirebaseClient.updateFirebaseWithHashes()
                    }
                    Log.i(tag, "Finished updateFirebaseWithHashes() in ${time}ms")
                }
            )

            // Wait for all to finish
            jobs.awaitAll()

            Log.i(tag, "All updates finished successfully")
            Result.success()

        } catch (e: Exception) {
            Log.e(tag, "Failed to update threats", e)
            Result.retry()
        }
    }
}
