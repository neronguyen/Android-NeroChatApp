package io.github.neronguyen.astrocommander.worker

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.github.neronguyen.astrocommander.AscomApplication
import io.github.neronguyen.astrocommander.R
import io.github.neronguyen.astrocommander.core.data.repository.PlaceholderRepository

@HiltWorker
class RandomIdWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val placeholderRepository: PlaceholderRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val result = placeholderRepository.syncPlaceholderList()
        return result.fold(
            ifLeft = { Result.retry() },
            ifRight = {
                showSuccessFetchNotification()
                Result.retry()
            }
        )
    }

    private fun showSuccessFetchNotification() {
        val builder = NotificationCompat.Builder(context, AscomApplication.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("New Data Fetched")
            .setContentText("New Data Fetched")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        with(NotificationManagerCompat.from(context)) {
            if (androidx.core.app.ActivityCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                notify(1, builder.build())
            }
        }
    }
}
