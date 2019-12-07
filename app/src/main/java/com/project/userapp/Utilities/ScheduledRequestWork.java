package com.project.userapp.Utilities;

import android.content.Context;
import android.location.Location;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.mapbox.mapboxsdk.geometry.LatLng;

import java.util.HashMap;
import java.util.Map;

public class ScheduledRequestWork extends Worker {

    private Context context;
    private String clientID;
    private FusedLocationProviderClient fusedLocationProviderClient;

    public ScheduledRequestWork(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.context = context;
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context);
        clientID = workerParams.getInputData().getString("userId");
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "doWork: Inside Work");
        fusedLocationProviderClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
            @Override
            public void onComplete(@NonNull Task<Location> task) {
                if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                    request_meeting_point();
                }else{
                    CommsNotificationManager.getInstance(context).displayError("Error", "You've Logged out please try again after logging in.");
                }
            }
        });
        return Result.success();
    }

    public void request_meeting_point() {

        fusedLocationProviderClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
            @Override
            public void onComplete(@NonNull Task<Location> task) {
                if (task.isSuccessful()) {
                    final Location user_location = task.getResult();
                    final LatLng distance_from_user = new LatLng(user_location);
                    final Double latitude = user_location.getLatitude();
                    final Double longitude = user_location.getLongitude();
                    final Double lessThanLatPoint = latitude - (20.0 / 111.045);
                    final Double gtThanLatPoint = latitude + (20.0 / 111.045);
                    final Double lessThanLonPoint = longitude - (20.0 / 111.045);
                    final Double gtThanLonPoint = longitude + (20.0 / 111.045);
                    Query q = FirebaseFirestore.getInstance().collection("meeting_points").whereLessThanOrEqualTo("latitude", gtThanLatPoint).whereGreaterThanOrEqualTo("latitude", lessThanLatPoint);
                    q.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            DocumentSnapshot idealMeetingPoint = null;
                            double meters = 0;
                            double temp_dist = 0;
                            double mp_lat = 0;
                            double mp_lon = 0;
                            QuerySnapshot snapshots = task.getResult();

                            Log.d(TAG, "Total MEETING POINTs onComplete: " + snapshots.size());

                            for (DocumentSnapshot snapshot : snapshots) {
                                Log.d(TAG, "onComplete: snapshot data " + snapshot.toString());
                                double temp_mp_lon = (Double) snapshot.get("longitude");
                                double temp_mp_lat = (Double) snapshot.get("latitude");
                                if (temp_mp_lon <= gtThanLonPoint || temp_mp_lon >= lessThanLonPoint) {
                                    temp_dist = distance_from_user.distanceTo(new LatLng(temp_mp_lat, temp_mp_lon));
                                    if (temp_dist <= meters || meters == 0) {
                                        meters = temp_dist;
                                        idealMeetingPoint = snapshot;
                                        mp_lat = temp_mp_lat;
                                        mp_lon = temp_mp_lon;
                                    }
                                }
                            }
                            if (idealMeetingPoint != null) {
                                Log.d(TAG, "onComplete: Final meeting Point -- " + meters + " = " + idealMeetingPoint.getData());
                                request_driver(new LatLng(mp_lat, mp_lon), idealMeetingPoint);
                            } else {
                                Log.d(TAG, "onComplete: Final meeting Point -- " + meters + " = " + "null");
                                CommsNotificationManager.getInstance(context).displayError("Error", "There's a problem.. We couldn't find a driver or meeting point. Try Again Later");
                            }
                        }
                    });
                }
            }
        });
    }

    private static final String TAG = "ScheduledRequestWork";

    private void request_driver(LatLng meeting_pointLocation, final DocumentSnapshot idealMeetingPoint) {
        final LatLng distance_from_mpLoc = meeting_pointLocation;
        final Double latitude = meeting_pointLocation.getLatitude();
        final Double longitude = meeting_pointLocation.getLongitude();
        final Double lessThanLatPoint = latitude - (20.0 / 111.045);
        final Double gtThanLatPoint = latitude + (20.0 / 111.045);
        final Double lessThanLonPoint = longitude - (20.0 / 111.045);
        final Double gtThanLonPoint = longitude + (20.0 / 111.045);

        Query q = FirebaseFirestore.getInstance().collection("user_master").whereEqualTo("type", "Driver").whereLessThanOrEqualTo("current_latitude", gtThanLatPoint).whereGreaterThanOrEqualTo("current_latitude", lessThanLatPoint).whereEqualTo("allocated", "no");
        q.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot idealDriver = null;
                    double meters = 0;
                    double temp_dist = 0;
                    double d_lat = 0;
                    double d_lon = 0;
                    QuerySnapshot snapshots = task.getResult();
                    Log.d(TAG, "onComplete: snapshot data " + snapshots.size());
                    for (DocumentSnapshot snapshot : snapshots) {
                        Log.d(TAG, "onComplete: snapshot data " + snapshot.toString());
                        double temp_mp_lon = (Double) snapshot.get("current_longitude");
                        double temp_mp_lat = (Double) snapshot.get("current_latitude");
                        if (temp_mp_lon <= gtThanLonPoint || temp_mp_lon >= lessThanLonPoint) {
                            temp_dist = distance_from_mpLoc.distanceTo(new LatLng(temp_mp_lat, temp_mp_lon));
                            if (temp_dist <= meters || meters == 0) {
                                meters = temp_dist;
                                idealDriver = snapshot;
                                d_lat = temp_mp_lat;
                                d_lon = temp_mp_lon;
                            }
                        }
                    }
                    if (idealDriver != null) {
                        Log.d(TAG, "onComplete: Final Driver -- " + meters + " = " + idealDriver.getData());
                        Map<String, Object> request_data = new HashMap<>();
                        Map<String, String> user_data = new HashMap<>();
                        Map<String, String> driver_data = new HashMap<>();
                        Map<String, String> mp_data = new HashMap<>();
                        user_data.put("client_name", FirebaseAuth.getInstance().getCurrentUser().getDisplayName());
                        user_data.put("client_email", FirebaseAuth.getInstance().getCurrentUser().getEmail());
                        driver_data.put("driver_name", (String) idealDriver.get("user_name"));
                        driver_data.put("driver_email", (String) idealDriver.get("user_email"));
                        driver_data.put("driver_phone", (String) idealDriver.get("user_phone"));
                        request_data.put("accepted", "no");
                        request_data.put("clientID", FirebaseAuth.getInstance().getCurrentUser().getUid());
                        request_data.put("driverID", idealDriver.getId());
                        request_data.put("client_onWay", "no");
                        request_data.put("driver_onWay", "no");
                        request_data.put("request_time", FieldValue.serverTimestamp());
                        request_data.put("client_info", user_data);
                        request_data.put("driver_info", driver_data);
                        mp_data.put("MeetingPointName", (String) idealMeetingPoint.get("LocationName"));
                        mp_data.put("MeetingPointAddress", (String) idealMeetingPoint.get("LocationAddress"));
                        mp_data.put("MeetingPointLat", String.valueOf(idealMeetingPoint.get("latitude")));
                        mp_data.put("MeetingPointLon", String.valueOf(idealMeetingPoint.get("longitude")));
                        request_data.put("meetingpointID", idealMeetingPoint.getId());
                        request_data.put("meetingpoint_info", mp_data);

                        FirebaseFirestore.getInstance().collection("requests_master").add(request_data).addOnCompleteListener(new OnCompleteListener<DocumentReference>() {
                            @Override
                            public void onComplete(@NonNull Task<DocumentReference> task) {
                                if (task.isSuccessful()) {
                                    Log.d(TAG, "onComplete: Request Added " + task.getResult().getId());
                                    CommsNotificationManager.getInstance(context).display("Request Success", "Scheduled Request has been added. Please View");
                                } else {
                                    Log.d(TAG, "onComplete: ERROR on request add" + task.getException());
                                }
                            }
                        });
                    } else {
                        Log.d(TAG, "onComplete: Final meeting Point -- " + meters + " = " + "null");
                        CommsNotificationManager.getInstance(context).displayError("Error", "There's a problem.. We couldn't find a driver or meeting point. Try Again Later");
                    }
                } else {
                    Log.e(TAG, "onComplete: " + task.getException());
                }
            }
        });
    }
}