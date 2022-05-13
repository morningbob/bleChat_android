package com.bitpunchlab.android.blechat_android.chatNotification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.navigation.NavDeepLinkBuilder
import com.bitpunchlab.android.blechat_android.*
import com.bitpunchlab.android.blechat_android.chat.ChatFragment

private const val TAG = "MessageAlertService"
private const val CHANNEL_ID = "BLEChat_Notice"



// this service is to show notification whenever there is a new message
// receive.  Whenever user tap on the service, I want to start the chat view.
// But if the user doesn't open the app, that means the app may not be connected
// to the device which sent the message.  so, in this case, I want to start the
// main fragment, that is to show the device list for user to choose to connect to.
class MessageAlertService : Service() {

    private var message = ""
    //private lateinit var notificationManager: NotificationManagerCompat
    var mBinder: IBinder = LocalBinder()

    override fun onCreate() {
        // we create the notification channel as early as possible
        // here, if the service is not running, onCreate will be called
        createNotificationChannel()

        super.onCreate()

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // we create the notification itself here, every time we start the service.
        super.onStartCommand(intent, flags, startId)
        message = intent?.extras?.getString("message").toString()
        Log.i(TAG, "onStartCommand: message: $message")
        // here, the channel, just as a precaution
        createNotificationChannel()
        createNotification()

        // I don't want the app to be too aggressive
        return START_NOT_STICKY
    }

    override fun onBind(p0: Intent?): IBinder? {
        return mBinder
    }

    private fun createNotification() {

        val intent = Intent(this, ChatFragment::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val bundle = Bundle()
        bundle.putBoolean("startFromNotification", true)

        var pendingIntent : PendingIntent = NavDeepLinkBuilder(applicationContext)
            .setComponentName(MainActivity::class.java)
            .setGraph(R.navigation.nav_graph)
            .setDestination(R.id.MainFragment)
            .setArguments(bundle)
            .createPendingIntent()

        //val pendingIntent: PendingIntent = PendingIntent
        //    .getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        var builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(androidx.core.R.drawable.notification_icon_background)
            .setContentTitle("BLE Chat message")
            .setContentText(message)
            //.setStyle(NotificationCompat.BigTextStyle())
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        with(NotificationManagerCompat.from(this)) {
            notify(NOTIFICATION_ID, builder.build())
        }
    }

    private fun createNotificationChannel() {
        // notification channel doesn't exist before api 26
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val name = getString(R.string.channel_name)
            val descriptionText = getString(R.string.notification_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }

            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    // we cancel the notification that's already sent
    fun cancelNotificationSent() {
        NotificationManagerCompat.from(applicationContext).cancel(NOTIFICATION_ID)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "the service is destroyed.")
    }

    inner class LocalBinder : Binder() {
        fun getMessageServiceInstance() : MessageAlertService = this@MessageAlertService
    }
}

