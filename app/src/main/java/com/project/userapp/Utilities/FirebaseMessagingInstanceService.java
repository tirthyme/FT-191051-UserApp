package com.project.userapp.Utilities;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;


public class FirebaseMessagingInstanceService extends FirebaseMessagingService {

    private static final String TAG = "MessagingInstance";

    @Override
    public void onNewToken(@NonNull String s) {
        super.onNewToken(s);
        Log.d(TAG, "onNewToken: "+s);
        sendRegistrationToServer(s);
    }

    private void sendRegistrationToServer(String s){
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            FirebaseFirestore.getInstance().collection("user_master").document(FirebaseAuth.getInstance().getUid()).update("FirebaseCloudMessagingID", s);
        }
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Log.d("kkkk", "onMessageReceived: Message Recieved");
        String title = remoteMessage.getNotification().getTitle();
        String body = remoteMessage.getNotification().getBody();
        CommsNotificationManager.getInstance(getApplicationContext()).display(title,body);

    }
}
