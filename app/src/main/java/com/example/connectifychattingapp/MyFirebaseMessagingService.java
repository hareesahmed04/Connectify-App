package com.example.connectifychattingapp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context; // Added missing import
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull; // Added missing import
import androidx.core.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.messaging.FirebaseMessagingService; // Required
import com.google.firebase.messaging.RemoteMessage;           // Required

import org.json.JSONObject;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        if (remoteMessage.getData().size() > 0) {
            String type = remoteMessage.getData().get("type");
            String callerName = remoteMessage.getData().get("callerName");
            String callerId = remoteMessage.getData().get("callerId");
            String channelId = remoteMessage.getData().get("channelId");
            String callerPic = remoteMessage.getData().get("callerPic");

            showIncomingCall(type, callerName, callerId, channelId, callerPic);
        }
    }

    private void showIncomingCall(String type, String name, String id, String channel, String pic) {
        Intent intent;
        // Correcting the activity names based on your provided files
        if ("video".equals(type)) {
            intent = new Intent(this, VideoCallIncomingActivity.class);
        } else {
            intent = new Intent(this, AudioIncomingActivity.class);
        }

        intent.putExtra("callerName", name);
        intent.putExtra("callerId", id);
        intent.putExtra("channelId", channel);
        intent.putExtra("callerPic", pic);

        // Use FLAG_ACTIVITY_CLEAR_TOP to ensure a clean launch
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_CLEAR_TOP |
                Intent.FLAG_ACTIVITY_SINGLE_TOP);

        String notificationChannelId = "call_channel";
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel callChannel = new NotificationChannel(notificationChannelId, "Incoming Calls", NotificationManager.IMPORTANCE_HIGH);
            callChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            notificationManager.createNotificationChannel(callChannel);
        }

        // PendingIntent.FLAG_IMMUTABLE is required for Android 12+
        PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, notificationChannelId)
                        .setSmallIcon(android.R.drawable.ic_menu_call) // Using system icon if your R.drawable.ic_call isn't ready
                        .setContentTitle("Incoming Call")
                        .setContentText(name + " is calling...")
                        .setPriority(NotificationCompat.PRIORITY_MAX)
                        .setCategory(NotificationCompat.CATEGORY_CALL)
                        .setFullScreenIntent(fullScreenPendingIntent, true)
                        .setAutoCancel(true)
                        .setOngoing(true); // Keeps notification visible during ring

        notificationManager.notify(1, notificationBuilder.build());
    }

    private void sendCallNotification(String receiverToken, String type) {
        new Thread(() -> {
            try {
                JSONObject data = new JSONObject();
                data.put("type", type); // "audio" or "video"
                data.put("callerName", "Your Name");
                data.put("callerId", FirebaseAuth.getInstance().getUid());
                data.put("channelId", "some_unique_channel_id");
                data.put("callerPic", "your_profile_url");

                JSONObject payload = new JSONObject();
                payload.put("to", receiverToken);
                payload.put("priority", "high"); // CRITICAL: Makes it pop up even if app is closed
                payload.put("data", data);

                RequestBody body = RequestBody.create(
                        payload.toString(),
                        MediaType.parse("application/json; charset=utf-8")
                );

                Request request = new Request.Builder()
                        .url("https://fcm.googleapis.com/fcm/send")
                        .addHeader("Authorization", "BAO1aL2oUIUFHSL4QfCgt2702JKUNx--DsC6KI4CnLDcmpQCokclB599MJhGUHKYz8IUVPIZk_qdBXBGKVUJmtM") // Found in Firebase Console > Project Settings > Cloud Messaging
                        .post(body)
                        .build();

                new OkHttpClient().newCall(request).execute();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}