package com.whatever.caro.core.ui.notification

import android.Manifest
import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@Composable
actual fun rememberNotificationPermissionController(): NotificationPermissionController {
    val context = LocalContext.current
    val pending = remember { PendingNotificationPermission() }
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
            pending.result?.complete(notificationsAllowed(context))
            pending.result = null
        }
    return remember(context, launcher) {
        object : NotificationPermissionController {
            private val mutex = Mutex()

            override suspend fun isGranted(): Boolean = notificationsAllowed(context)

            override suspend fun request(): Boolean =
                withContext(Dispatchers.Main.immediate) {
                    mutex.withLock {
                        if (
                            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                        ) {
                            return@withLock notificationsAllowed(context)
                        }
                        val result =
                            pending.result ?: CompletableDeferred<Boolean>().also {
                                pending.result = it
                                try {
                                    launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } catch (exception: Exception) {
                                    pending.result = null
                                    throw exception
                                }
                            }
                        result.await()
                    }
                }

            override fun openSettings() {
                try {
                    context.startActivity(
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                } catch (_: ActivityNotFoundException) {
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                }
            }
        }
    }
}

private class PendingNotificationPermission {
    var result: CompletableDeferred<Boolean>? = null
}

private fun notificationsAllowed(context: Context): Boolean {
    if (
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
    ) {
        return false
    }
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val channel = manager.getNotificationChannel("caro_learning")
    return manager.areNotificationsEnabled() && channel?.importance != NotificationManager.IMPORTANCE_NONE
}
