package com.google.sample.cloudvision.NotificationServiceTool;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;

import com.google.sample.cloudvision.MainActivity;
import com.google.sample.cloudvision.MainService.TextRecognition_screenshot;
import com.google.sample.cloudvision.R;

public class NotificationManagement extends ContextWrapper {
    private NotificationManager manager;
    public static final String CHANNEL_ID = "A";
    public static final String CHANNEL_NAME = "B";

    public NotificationManagement(Context context){
        super(context);

        NotificationChannel channel = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW);
            channel.setLightColor(Color.GREEN);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            getManager().createNotificationChannel(channel);
        }
    }

    public Notification.Builder getNotification(String title, String body) {
        Notification.Builder builder = null;
        Intent intent = new Intent(this, TextRecognition_screenshot.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(getApplicationContext(), CHANNEL_ID);
            builder.setContentTitle(title);
            builder.setContentText(body);
            builder.setSmallIcon(getSmallIcon());
            builder.setAutoCancel(false);
            builder.setContentIntent(pendingIntent);
        }
        return builder;
    }

    private int getSmallIcon() {
        return R.mipmap.ic_launcher;
    }

    private NotificationManager getManager(){
        if(manager == null){
            manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        }
        return manager;
    }
}