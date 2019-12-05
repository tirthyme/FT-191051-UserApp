package com.project.userapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;

import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.mapbox.mapboxsdk.Mapbox;

public class ViewMadeRequests extends AppCompatActivity {

    RecyclerView recyclerView;
    private RequestsAdapter adapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(this,getResources().getString(R.string.access_token));
        setContentView(R.layout.activity_view_made_requests);
        recyclerView = findViewById(R.id.recyclerView);
        setRecyclerView();
    }

    void setRecyclerView(){
        Query query = FirebaseFirestore.getInstance().collection("requests_master").
                whereEqualTo("clientID", FirebaseAuth.getInstance().getCurrentUser().getUid())
                .orderBy("request_time", Query.Direction.DESCENDING);

        FirestoreRecyclerOptions<Request> options = new FirestoreRecyclerOptions.Builder<Request>()
                .setQuery(query,Request.class).build();
        adapter = new RequestsAdapter(options);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        adapter.startListening();
    }

    @Override
    protected void onStop() {
        super.onStop();
        adapter.stopListening();
    }
}
