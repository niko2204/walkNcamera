package edu.mnu.walkncamera;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.provider.Settings;

import androidx.core.app.NotificationCompat;

public class BootService extends Service {
    public BootService() {
    }

    private String myChannelId = "mChannelId";
    private String channelName = "mChannelName";
    private String notificationTitle = "자동실행서비스";
    private String notificationText = "자동실행 서비스 실행 중";
    private int id = 1000;


    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(id, createNotification());
        startApp();

    }

    private void createNotificationChannel(){
        NotificationChannel notificationChannel = new NotificationChannel(myChannelId, channelName, NotificationManager.IMPORTANCE_HIGH); // Android 8.0 이상 NotificationChannel 생성 필수
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.createNotificationChannel(notificationChannel); // NotificationManager 를 통해서 Notification Channel 생성
    }

    private Notification createNotification(){ // Notification 만들기
        return new NotificationCompat.Builder(this, myChannelId)
                .setContentTitle(notificationTitle)
                .setContentText(notificationText)
                .build();
    }

    private void startApp(){ // 앱 실행하기
        if(Settings.canDrawOverlays(this)){ // 권한이 있을 때
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            this.startActivity(intent);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopForeground(true);
    }
}