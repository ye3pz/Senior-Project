package com.example.shade.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.shade.data.FirebaseClient

class UpdateThreatsWorker (appContext: Context, workerParams: WorkerParameters): CoroutineWorker(appContext, workerParams) {

    val tag = "UpdateThreatsWorker"
    override suspend fun doWork(): Result {
        return try {
            FirebaseClient.updateFirebaseWithThreats()
            Result.success()
        } catch( e: Exception){
            Log.e(tag, "Failed to update threats", e)
            Result.retry()
        }
    }
}
