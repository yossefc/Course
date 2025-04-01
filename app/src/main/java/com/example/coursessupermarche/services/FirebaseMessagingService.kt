package com.example.coursessupermarche.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.coursessupermarche.MainActivity
import com.example.coursessupermarche.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FirebaseMessagingService : FirebaseMessagingService() {

    private val TAG = "FirebaseMessaging"

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "From: ${remoteMessage.from}")

        // Vérifier si le message contient des données
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: ${remoteMessage.data}")
            handleNow(remoteMessage.data)
        }

        // Vérifier si le message contient une notification
        remoteMessage.notification?.let {
            Log.d(TAG, "Message Notification Body: ${it.body}")
            sendNotification(it.title, it.body)
        }
    }

    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")

        // Envoyer le token au serveur pour associer ce dispositif à l'utilisateur
        sendRegistrationToServer(token)
    }

    private fun handleNow(data: Map<String, String>) {
        // Traiter les données du message
        val title = data["title"] ?: getString(R.string.app_name)
        val message = data["message"] ?: "Nouvelle notification"

        // Afficher une notification
        sendNotification(title, message)
    }

    private fun sendRegistrationToServer(token: String) {
        // TODO: Implémenter l'envoi du token au serveur
        // Dans une application réelle, vous enverriez ce token à votre serveur
        // pour pouvoir envoyer des notifications à ce dispositif spécifique
    }

    private fun sendNotification(title: String?, messageBody: String?) {
        // Créer une intention pour ouvrir l'application lorsque l'utilisateur
        // clique sur la notification
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_ONE_SHOT
        )

        val channelId = "courses_supermarche_channel"
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title ?: getString(R.string.app_name))
            .setContentText(messageBody ?: "")
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Pour Android Oreo et versions ultérieures, les canaux de notification sont obligatoires
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Notifications liste de courses",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        // Afficher la notification
        val notificationId = System.currentTimeMillis().toInt()
        notificationManager.notify(notificationId, notificationBuilder.build())
    }
}