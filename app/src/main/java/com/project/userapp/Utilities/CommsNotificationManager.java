package com.project.userapp.Utilities;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import com.project.userapp.R;
import com.project.userapp.ViewMadeRequests;

public class CommsNotificationManager {

    private static Context context;
    private static CommsNotificationManager instance;
    private static NotificationChannel channel;
    private static NotificationManager notificationManager;
    private CommsNotificationManager(Context context){
        this.context = context;
    }


    public static synchronized CommsNotificationManager getInstance(Context context){
        if (instance == null){
            instance = new CommsNotificationManager(context);
        }
        if (Build.VERSION.SDK_INT >= 26) {
            channel = new NotificationChannel(Constants.channel_id, Constants.channel_name, NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription(Constants.channel_desc);
            channel.enableLights(true);
            channel.setLightColor(Color.RED);
            channel.enableVibration(true);
            notificationManager =(NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(channel);
        }else{
            notificationManager =(NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        }
        return instance;
    }

    public void display(String Title, String body){
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context,Constants.channel_id);
        builder.setSmallIcon(R.drawable.ic_launcher_foreground);
        builder.setContentTitle(Title).setContentText(body);
        Intent intent = new Intent(context, ViewMadeRequests.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(pendingIntent);
        builder.setAutoCancel(true);
        if (notificationManager != null){
            notificationManager.notify(1,builder.build());
        }
    }


}
