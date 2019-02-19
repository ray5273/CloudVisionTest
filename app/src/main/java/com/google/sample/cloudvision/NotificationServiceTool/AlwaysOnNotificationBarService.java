package com.google.sample.cloudvision.NotificationServiceTool;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.google.sample.cloudvision.MainActivity;
import com.google.sample.cloudvision.MainService.TextRecognition_screenshot;
import com.google.sample.cloudvision.R;

public class AlwaysOnNotificationBarService extends Service {
    private static final int NOTIFICATION_ID = 1304;
    private static NotificationManagement notiman = TextRecognition_screenshot.notiman;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        //Communication between Service Object and Activity
        return null;
    }

    @Override
    public void onCreate(){
        super.onCreate();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder tempBuilder = notiman.getNotification(getString(R.string.notification_title), getString(R.string.notification_content));
            if(tempBuilder!=null) {
                startForeground(NOTIFICATION_ID, tempBuilder.build());
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
    }
}
