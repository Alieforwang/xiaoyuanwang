package com.example.xiaoyuanwang.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import com.example.xiaoyuanwang.MainActivity;
import com.example.xiaoyuanwang.R;
import com.example.xiaoyuanwang.api.SrunPortal;
import com.example.xiaoyuanwang.utils.ConfigManager;

public class KeepAliveService extends Service {
    private static final String CHANNEL_ID = "KeepAliveChannel";
    private static final String TAG = "KeepAliveService";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("校园网客户端正在运行")
                .setContentText("点击返回应用")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent)
                .build();

        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(1, notification);
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "KeepAliveService onDestroy");
        
        ConfigManager configManager = new ConfigManager(this);
        if (configManager.isAutoLogout()) {
            Log.d(TAG, "Triggering Auto Logout from Service");
            String username = configManager.getUsername();
            String domain = configManager.getDomain();
            String serverUrl = configManager.getServerUrl();
            String userAgent = configManager.getUserAgent();
            
            if (!username.isEmpty()) {
                new Thread(() -> {
                    new SrunPortal(serverUrl, userAgent).logout(username, "1", "", domain);
                }).start();
            }
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Keep Alive Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }
}
