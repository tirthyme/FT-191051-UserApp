package com.project.userapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TimePicker;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.ServerTimestamp;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.location.modes.RenderMode;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.plugins.markerview.MarkerView;
import com.mapbox.mapboxsdk.plugins.markerview.MarkerViewManager;
import com.project.userapp.Utilities.CommsNotificationManager;
import com.project.userapp.Utilities.ScheduledRequestWork;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import timber.log.Timber;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, PermissionsListener {
    private static FirebaseUser firebaseUser;
    MapView mapView;
    MapboxMap mapboxMap;
    LocationComponent locationComponent;
    String workID = "ScheduleWork";
    PermissionsManager permissionsManager;
    MarkerViewManager markerViewManager;
    ProgressBar progressBar;
    MarkerView markerView;
    Button requester_button;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        Mapbox.getInstance(MainActivity.this,getResources().getString(R.string.access_token));
        setContentView(R.layout.activity_main);
        LinearLayout signin = findViewById(R.id.reglog);
        ConstraintLayout m = findViewById(R.id.maplog);
        progressBar = findViewById(R.id.progress_meetingPoint);
        requester_button = findViewById(R.id.requester_button);
        requester_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this).setPositiveButton("Request Now.", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        request_meeting_point();
                    }
                }).setNegativeButton("Schedule Request", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final View dialogView = View.inflate(MainActivity.this, R.layout.date_time_picker, null);
                        final AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
                        final DatePicker datePicker = dialogView.findViewById(R.id.date_picker);
                        final TimePicker timePicker = dialogView.findViewById(R.id.time_picker);
                        datePicker.setMinDate(System.currentTimeMillis());
                        final long maxtime = System.currentTimeMillis()+(1000*60*60*24*7);
                        datePicker.setMaxDate(maxtime);
                        dialogView.findViewById(R.id.date_time_set).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                Calendar calendar = new GregorianCalendar(datePicker.getYear(),
                                        datePicker.getMonth(),
                                        datePicker.getDayOfMonth(),
                                        timePicker.getCurrentHour(),
                                        timePicker.getCurrentMinute());
                                long currentTime= System.currentTimeMillis();
                                long specificTimeToTrigger = calendar.getTimeInMillis();
                                long delayToPass = specificTimeToTrigger - currentTime;
                                Log.d(TAG, "onClick: "+ delayToPass + "==" + currentTime);
                                if(delayToPass > 0 && delayToPass<(1000*60*60*24*7)) {
                                    Constraints constraints = new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED)
                                            .build();
                                    OneTimeWorkRequest oneTimeWorkRequest = new OneTimeWorkRequest.Builder(ScheduledRequestWork.class)
                                            .setConstraints(constraints)
                                            .setInitialDelay(delayToPass, TimeUnit.MILLISECONDS)
                                            .build();
                                    WorkManager.getInstance(getApplicationContext()).enqueue(oneTimeWorkRequest);
                                    alertDialog.dismiss();
                                }else{
                                    Toast.makeText(MainActivity.this, "Select Proper Time", Toast.LENGTH_SHORT).show();
                                    alertDialog.dismiss();
                                }
                            }});
                        alertDialog.setView(dialogView);
                        dialog.dismiss();
                        alertDialog.show();
                    }
                }).setNeutralButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                builder.show();
            }
        });
        mapView = findViewById(R.id.mapView);
        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if(firebaseUser == null){
            signin.setVisibility(View.VISIBLE);
        }else{
            FirebaseInstanceId.getInstance().getInstanceId().addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                @Override
                public void onComplete(@NonNull Task<InstanceIdResult> task) {
                    InstanceIdResult res= task.getResult();
                    FirebaseFirestore.getInstance().collection("user_master").document(firebaseUser.getUid()).update("FirebaseCloudMessagingID",res.getToken());
                }
            });
            m.setVisibility(View.VISIBLE);
            mapView.onCreate(savedInstanceState);
            mapView.getMapAsync(this);
        }
    }



    public void signOut(View view){
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(MainActivity.this, MainActivity.class));
            finish();
        }
    }

    public void registrationActivity(View view){
        startActivity(new Intent(MainActivity.this,RegistrationActivity.class));
        finish();
    }

    public void loginActivity(View view){
        startActivity(new Intent(MainActivity.this,LoginActivity.class));
        finish();
    }

    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {

    }

    public void viewReq(View view){
        startActivity(new Intent(this,ViewMadeRequests.class));
    }

    @Override
    public void onPermissionResult(boolean granted) {
        if (granted && mapboxMap != null) {
            Style style = mapboxMap.getStyle();
            if (style != null) {
                enableLocationPlugin(style);
            }
        } else {
            Toast.makeText(this, "Failed", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onMapReady(@NonNull MapboxMap mapboxMap) {
        MainActivity.this.mapboxMap = mapboxMap;
        markerViewManager = new MarkerViewManager(mapView, mapboxMap);
        mapboxMap.setStyle(Style.MAPBOX_STREETS, new Style.OnStyleLoaded() {
            @Override
            public void onStyleLoaded(@NonNull Style style) {
                enableLocationPlugin(style);
            }
        });

    }
    FusedLocationProviderClient fusedLocationProviderClient;
    private void enableLocationPlugin(@NonNull Style loadedMapStyle) {
// Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(this)) {

            // Get an instance of the component. Adding in LocationComponentOptions is also an optional
            // parameter
            locationComponent = mapboxMap.getLocationComponent();
            locationComponent.activateLocationComponent(LocationComponentActivationOptions.builder(
                    this, loadedMapStyle).build());
            locationComponent.setLocationComponentEnabled(true);
            fusedLocationProviderClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
                @Override
                public void onComplete(@NonNull Task<Location> task) {
                    getNearbyDrivers(task.getResult());
                }
            });

            // Set the component's camera mode
            locationComponent.setCameraMode(CameraMode.TRACKING);
            locationComponent.setRenderMode(RenderMode.NORMAL);
        } else {
            permissionsManager = new PermissionsManager(this);
            permissionsManager.requestLocationPermissions(this);
        }
    }

    public void updateCameraLocation(Double latitude ,Double longitude){
        CameraPosition position = new CameraPosition.Builder()
                .target(new LatLng(latitude, longitude)) // Sets the new camera position
                .zoom(13) // Sets the zoom
                .build(); // Creates a CameraPosition from the builder

        mapboxMap.animateCamera(CameraUpdateFactory
                .newCameraPosition(position), 2000);

    }


    private static final String TAG = "MainActivity";
    public void getNearbyDrivers(Location location){
        Double latitude = location.getLatitude();
        final Double longitude = location.getLongitude();
        final Double lessThanLatPoint = latitude  - (20.0 / 111.045);
        final Double gtThanLatPoint = latitude  + (20.0 / 111.045);
        final Double lessThanLonPoint = longitude - (20.0 / 111.045);
        final Double gtThanLonPoint = longitude + (20.0 / 111.045);
        updateCameraLocation(latitude,longitude);
        Query q = FirebaseFirestore.getInstance().collection("user_master").whereEqualTo("type","Driver").whereLessThanOrEqualTo("current_latitude",gtThanLatPoint).whereGreaterThanOrEqualTo("current_latitude",lessThanLatPoint);
        q.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (!task.getResult().isEmpty()){
                    QuerySnapshot snapshots = task.getResult();
                    View customView;
                    MarkerView marker;
                    /*ImageView imageView;*/
                    for (final DocumentSnapshot snapshot : snapshots){
                        /*Log.d(TAG, "onComplete: "+snapshot.toString());*/
                        if ((Double)snapshot.get("current_longitude") <= gtThanLonPoint ||(Double) snapshot.get("current_longitude") >= lessThanLonPoint){
                            customView = LayoutInflater.from(MainActivity.this).inflate(
                                    R.layout.pointerview, null);
                            marker = new MarkerView(new LatLng((Double)snapshot.get("current_latitude"),(Double)snapshot.get("current_longitude")),customView);
                            markerViewManager.addMarker(marker);
                        }
                    }
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Timber.tag("Error").e(e);
            }
        });
    }

    public void request_meeting_point(){
        progressBar.setVisibility(View.VISIBLE);
        fusedLocationProviderClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
            @Override
            public void onComplete(@NonNull Task<Location> task) {
                if (task.isSuccessful()){
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
                            double meters=0;double temp_dist = 0;
                            double mp_lat=0;double mp_lon=0;
                            QuerySnapshot snapshots = task.getResult();

                            Log.d(TAG, "Total MEETING POINTs onComplete: "+snapshots.size());

                            for (DocumentSnapshot snapshot: snapshots) {
                                Log.d(TAG, "onComplete: snapshot data " + snapshot.toString());
                                double temp_mp_lon = (Double) snapshot.get("longitude");
                                double temp_mp_lat = (Double) snapshot.get("latitude");
                                if (temp_mp_lon <= gtThanLonPoint || temp_mp_lon >= lessThanLonPoint) {
                                    temp_dist = distance_from_user.distanceTo(new LatLng(temp_mp_lat,temp_mp_lon));
                                    if (temp_dist <= meters || meters == 0){
                                        meters = temp_dist;
                                        idealMeetingPoint = snapshot;
                                        mp_lat = temp_mp_lat;
                                        mp_lon = temp_mp_lon;
                                    }
                                }
                            }
                            if (idealMeetingPoint != null) {
                                Log.d(TAG, "onComplete: Final meeting Point -- " + meters + " = " + idealMeetingPoint.getData());
                                request_driver(new LatLng(mp_lat,mp_lon),idealMeetingPoint);
                            }else {
                                Log.d(TAG, "onComplete: Final meeting Point -- " + meters + " = " + "null");
                                AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this).setTitle("Ooops..")
                                        .setMessage("There's a problem.. We couldn't find a driver or meeting point. Try Again Later");
                                dialog.setPositiveButton("Okay!", null);
                                dialog.show();
                            }
                        }
                    });
                }
            }
        });
    }

    public void request_driver(LatLng meeting_pointLocation, final DocumentSnapshot idealMeetingPoint){
        final LatLng distance_from_mpLoc = meeting_pointLocation;
        final Double latitude = meeting_pointLocation.getLatitude();
        final Double longitude = meeting_pointLocation.getLongitude();
        final Double lessThanLatPoint = latitude - (20.0 / 111.045);
        final Double gtThanLatPoint = latitude + (20.0 / 111.045);
        final Double lessThanLonPoint = longitude - (20.0 / 111.045);
        final Double gtThanLonPoint = longitude + (20.0 / 111.045);

        Query q = FirebaseFirestore.getInstance().collection("user_master").whereEqualTo("type","Driver").whereLessThanOrEqualTo("current_latitude",gtThanLatPoint).whereGreaterThanOrEqualTo("current_latitude",lessThanLatPoint).whereEqualTo("allocated","no");
        q.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()){
                    DocumentSnapshot idealDriver = null;
                    double meters=0;
                    double temp_dist = 0;
                    double d_lat=0;double d_lon=0;
                    QuerySnapshot snapshots = task.getResult();
                    Log.d(TAG, "onComplete: snapshot data " + snapshots.size());
                    for (DocumentSnapshot snapshot:snapshots) {
                        Log.d(TAG, "onComplete: snapshot data " + snapshot.toString());
                        double temp_mp_lon = (Double) snapshot.get("current_longitude");
                        double temp_mp_lat = (Double) snapshot.get("current_latitude");
                        if (temp_mp_lon <= gtThanLonPoint || temp_mp_lon >= lessThanLonPoint) {
                            temp_dist = distance_from_mpLoc.distanceTo(new LatLng(temp_mp_lat,temp_mp_lon));
                            if (temp_dist <= meters || meters == 0){
                                meters = temp_dist;
                                idealDriver = snapshot;
                                d_lat = temp_mp_lat;
                                d_lon = temp_mp_lon;
                            }
                        }
                    }
                    if (idealDriver != null) {
                        Log.d(TAG, "onComplete: Final Driver -- " + meters + " = " + idealDriver.getData());
                        Map<String,Object> request_data = new HashMap<>();
                        Map<String,String> user_data = new HashMap<>();
                        Map<String,String> driver_data = new HashMap<>();
                        Map<String,String> mp_data = new HashMap<>();
                        user_data.put("client_name",FirebaseAuth.getInstance().getCurrentUser().getDisplayName());
                        user_data.put("client_email",FirebaseAuth.getInstance().getCurrentUser().getEmail());
                        driver_data.put("driver_name",(String)idealDriver.get("user_name"));
                        driver_data.put("driver_email",(String)idealDriver.get("user_email"));
                        driver_data.put("driver_phone",(String)idealDriver.get("user_phone"));
                        request_data.put("accepted","no");
                        request_data.put("clientID",FirebaseAuth.getInstance().getCurrentUser().getUid());
                        request_data.put("driverID",idealDriver.getId());
                        request_data.put("client_onWay","no");
                        request_data.put("driver_onWay","no");
                        request_data.put("request_time", FieldValue.serverTimestamp());
                        request_data.put("client_info",user_data);
                        request_data.put("driver_info",driver_data);
                        mp_data.put("MeetingPointName",(String) idealMeetingPoint.get("LocationName"));
                        mp_data.put("MeetingPointAddress",(String) idealMeetingPoint.get("LocationAddress"));
                        mp_data.put("MeetingPointLat",String.valueOf(idealMeetingPoint.get("latitude")));
                        mp_data.put("MeetingPointLon",String.valueOf(idealMeetingPoint.get("longitude")));
                        request_data.put("meetingpointID",idealMeetingPoint.getId());
                        request_data.put("meetingpoint_info",mp_data);

                        FirebaseFirestore.getInstance().collection("requests_master").add(request_data).addOnCompleteListener(new OnCompleteListener<DocumentReference>() {
                            @Override
                            public void onComplete(@NonNull Task<DocumentReference> task) {
                                if (task.isSuccessful()){
                                    Log.d(TAG, "onComplete: Request Added " + task.getResult().getId());
                                    CommsNotificationManager.getInstance(getApplicationContext()).display("Request Success", "Request has been added. Please View");
                                    Toast.makeText(MainActivity.this, "Request Added", Toast.LENGTH_SHORT).show();
                                    progressBar.setVisibility(View.GONE);
                                }
                                else{
                                    Log.d(TAG, "onComplete: ERROR on request add" + task.getException());
                                }
                            }
                        });
                    }else {
                        Log.d(TAG, "onComplete: Final meeting Point -- " + meters + " = " + "null");
                        AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this).setTitle("Ooops..")
                                .setMessage("There's a problem.. We couldn't find a driver or meeting point. Try Again Later");
                        dialog.setPositiveButton("Okay!", null);
                        dialog.show();
                        progressBar.setVisibility(View.GONE);
                    }
                }
                else{
                    Log.e(TAG, "onComplete: " + task.getException());
                }
            }
        });
    }

    /*public void request_driver(LatLng location){
        final Double latitude = location.getLatitude();
        final Double longitude = location.getLongitude();
        final Double lessThanLatPoint = latitude  - (20.0 / 111.045);
        final Double gtThanLatPoint = latitude  + (20.0 / 111.045);
        final Double lessThanLonPoint = longitude - (20.0 / 111.045);
        final Double gtThanLonPoint = longitude + (20.0 / 111.045);
        Query q = FirebaseFirestore.getInstance().collection("user_master").whereEqualTo("type","Driver").whereLessThanOrEqualTo("current_latitude",gtThanLatPoint).whereGreaterThanOrEqualTo("current_latitude",lessThanLatPoint).whereEqualTo("allocated","no");
        q.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()){

                    String idealDriverID = "No Driver There";
                    double meters=0;
                    QuerySnapshot snapshots = task.getResult();
                    Log.d(TAG, "onComplete: "+snapshots.size());
                    for (final DocumentSnapshot snapshot : snapshots){
                        float[] results = new float[1];
                        LatLng user_location = new LatLng((Double) snapshot.get("current_longitude"),(Double)snapshot.get("current_longitude"));
                        if ((Double)snapshot.get("current_longitude") <= gtThanLonPoint ||(Double) snapshot.get("current_longitude") >= lessThanLonPoint){
                            double m = user_location.distanceTo(new LatLng(latitude,longitude));
                            Log.d(TAG, "onComplete: Distance"+user_location.distanceTo(new LatLng(latitude,longitude)));
                            if (m <= meters || meters == 0){
                                idealDriverID = snapshot.getId();
                                meters = m;
                            }
                        }
                    }
                    Log.d(TAG, "onComplete: Found ideal driver--" + idealDriverID + "which is" + meters + "far from meeting point");
                    progressBar.setVisibility(View.GONE);
                }
            }
        });
    }*/
}
