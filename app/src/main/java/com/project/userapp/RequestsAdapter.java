package com.project.userapp;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mapbox.api.directions.v5.DirectionsCriteria;
import com.mapbox.api.directions.v5.MapboxDirections;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.style.layers.LineLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.mapbox.core.constants.Constants.PRECISION_6;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineDasharray;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineTranslate;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineWidth;

public class RequestsAdapter extends FirestoreRecyclerAdapter<Request,RequestsAdapter.RequestsHolder>{

    FusedLocationProviderClient fusedLocationProviderClient;

    public RequestsAdapter(@NonNull FirestoreRecyclerOptions<Request> options) {
        super(options);

    }

    @Override
    protected void onBindViewHolder(@NonNull final RequestsHolder holder, int position, @NonNull final Request model) {
        holder.title.setText(model.getMeetingpoint_info().get("MeetingPointName"));
        SimpleDateFormat sfd = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss a");
        holder.timestamp.setText(sfd.format(model.getRequest_time().toDate()));
        if (model.getDriver_onWay().equals("no")){
            holder.status.setText("Status: Waiting for Accepted");
        }else{
            holder.status.setText("Accepted");
        }
        holder.del_name.setText("Driver Name: " + model.getDriver_info().get("driver_name"));
        holder.del_phone.setText("Driver Phone: " + model.getDriver_info().get("driver_phone"));
        holder.address.setText("Meeting Point Address: " + model.getMeetingpoint_info().get("MeetingPointAddress"));
        holder.cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (model.getAccepted().equals("yes")) {
                    AlertDialog.Builder dialog = new AlertDialog.Builder(holder.context).setTitle("Confirmation..").setMessage("Are you sure you want to start your navigation?")
                            .setPositiveButton("Okaay", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    FirebaseFirestore.getInstance().collection("requests_master").document(model.getId()).update("client_onWay", "yes").addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if(task.isSuccessful()){
                                                Intent intent = new Intent(holder.context, NavigationActivity.class);
                                                Map<String, Object> map = new HashMap<>();
                                                map.put("holder", holder);
                                                intent.putExtra("meetingpointID", model.getMeetingpointID());
                                                intent.putExtra("requestID",model.getId());
                                                intent.putExtra("meetingpointLat", String.valueOf(model.getMeetingpoint_info().get("MeetingPointLat")));
                                                intent.putExtra("meetingpointLon", String.valueOf(model.getMeetingpoint_info().get("MeetingPointLon")));
                                                intent.putExtra("driverID", model.getDriverID());
                                                holder.context.startActivity(intent);
                                            }
                                        }
                                    });
                                }
                            });
                    dialog.show();
                }else{
                    AlertDialog.Builder dialog = new AlertDialog.Builder(holder.context).setTitle("Wait a moment..").setMessage("Driver hasn't accepted your request yet.")
                            .setPositiveButton("Okaay",null);
                    dialog.show();
                }
            }
        });
        holder.mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(@NonNull MapboxMap mapboxMap) {
                Log.d("TAG", "onMapReady: Called");
                holder.mapboxMap = mapboxMap;
                mapboxMap.setStyle(Style.LIGHT, new Style.OnStyleLoaded() {
                    @Override
                    public void onStyleLoaded(@NonNull Style style) {
                        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(holder.context);
                        initDottedLineSourceAndLayer(style,holder);
                        getRoute(model, holder);
                    }
                });
            }
        });
    }
    @NonNull
    @Override
    public RequestsHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.view_item,parent,false);
        return new RequestsHolder(view);
    }

    class RequestsHolder extends RecyclerView.ViewHolder{
        TextView title, timestamp, address, del_name, del_phone, status;
        CardView cardView;
        MapView mapView;
        MapboxMap mapboxMap;
        FeatureCollection dashedLineDirectionsFeatureCollection;
        final Context context;
        public RequestsHolder(@NonNull View itemView) {
            super(itemView);
            context = itemView.getContext();
            cardView = itemView.findViewById(R.id.card_layout);
            title = itemView.findViewById(R.id.title);
            address = itemView.findViewById(R.id.address);
            timestamp = itemView.findViewById(R.id.timestamp);
            del_name = itemView.findViewById(R.id.del_name);
            del_phone = itemView.findViewById(R.id.del_phone);
            status = itemView.findViewById(R.id.status);
            mapView = itemView.findViewById(R.id.mapView);
        }
    }


    private void initDottedLineSourceAndLayer(@NonNull Style loadedMapStyle, RequestsHolder holder) {
        holder.dashedLineDirectionsFeatureCollection = FeatureCollection.fromFeatures(new Feature[] {});
        loadedMapStyle.addSource(new GeoJsonSource("SOURCE_ID", holder.dashedLineDirectionsFeatureCollection));
        loadedMapStyle.addLayerBelow(
                new LineLayer(
                        "DIRECTIONS_LAYER_ID", "SOURCE_ID").withProperties(
                        lineWidth(2.5f),
                        lineColor(Color.BLACK)
                ), "road-label-small");
    }



    public void updateCameraLocation(Double latitude ,Double longitude, RequestsHolder holder){
        CameraPosition position = new CameraPosition.Builder()
                .target(new LatLng(latitude, longitude)) // Sets the new camera position
                .zoom(12) // Sets the zoom
                .build(); // Creates a CameraPosition from the builder

        holder.mapboxMap.animateCamera(CameraUpdateFactory
                .newCameraPosition(position), 1000);



    }


    @SuppressWarnings( {"MissingPermission"})
    private void getRoute(final Request request, final RequestsHolder holder) {
        fusedLocationProviderClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
            @Override
            public void onComplete(@NonNull Task<Location> task) {

                Point directionsOriginPoint = Point.fromLngLat(task.getResult().getLongitude(), task.getResult().getLatitude());
                updateCameraLocation(task.getResult().getLatitude(),task.getResult().getLongitude(),holder);
                Point directionsDestPoint = Point.fromLngLat(Double.valueOf(request.getMeetingpoint_info().get("MeetingPointLon")),Double.valueOf(request.getMeetingpoint_info().get("MeetingPointLat")));
                MapboxDirections client = MapboxDirections.builder()
                        .origin(directionsOriginPoint)
                        .destination(directionsDestPoint)
                        .overview(DirectionsCriteria.OVERVIEW_FULL)
                        .profile(DirectionsCriteria.PROFILE_DRIVING)
                        .accessToken("pk.eyJ1IjoidGlydGh5bWUiLCJhIjoiY2syYWtscXVzMGh2ZjNwcDJqYzZ6NG94bSJ9.UnKLn7w-k_gFKdfpGpxvLQ")
                        .build();

                client.enqueueCall(new Callback<DirectionsResponse>() {
                    @Override
                    public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
                        if (response.body() == null) {
                            Log.d("TAG", "No routes found, make sure you set the right user and access token.");
                            return;
                        } else if (response.body().routes().size() < 1) {
                            Log.d("TAG", "No routes found");
                            return;
                        }
                        drawNavigationPolylineRoute(response.body().routes().get(0),holder);
                    }

                    @Override
                    public void onFailure(Call<DirectionsResponse> call, Throwable t) {
                        Log.d("TAG",t.getMessage());
                    }
                });
            }
        });

    }

    private void drawNavigationPolylineRoute(final DirectionsRoute route, final RequestsHolder request) {
        if (request.mapboxMap != null) {
            request.mapboxMap.getStyle(new Style.OnStyleLoaded() {
                @Override
                public void onStyleLoaded(@NonNull Style style) {
                    List<Feature> directionsRouteFeatureList = new ArrayList<>();
                    LineString lineString = LineString.fromPolyline(route.geometry(), PRECISION_6);
                    List<Point> coordinates = lineString.coordinates();
                    for (int i = 0; i < coordinates.size(); i++) {
                        directionsRouteFeatureList.add(Feature.fromGeometry(LineString.fromLngLats(coordinates)));
                    }
                    request.dashedLineDirectionsFeatureCollection = FeatureCollection.fromFeatures(directionsRouteFeatureList);
                    GeoJsonSource source = style.getSourceAs("SOURCE_ID");
                    if (source != null) {
                        source.setGeoJson(request.dashedLineDirectionsFeatureCollection);
                    }
                }
            });
        }
    }


}
