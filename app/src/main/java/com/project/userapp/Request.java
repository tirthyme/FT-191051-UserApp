package com.project.userapp;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;


import java.util.Map;

public class Request {

    private String accepted,clientID,driverID,client_onWay,driver_onWay, meetingpointID;
    private Timestamp request_time;
    private Map<String,String> client_info,driver_info,meetingpoint_info;
    @DocumentId
    private String id;

    public Request(){}

    public Request(String accepted, String clientID, String driverID, String client_onWay, String driver_onWay, String meetingpointID, Map<String, String> client_info, Map<String, String> driver_info, Map<String, String> meetingpoint_info, Timestamp request_time, String id) {
        this.id = id;
        this.accepted = accepted;
        this.clientID = clientID;
        this.driverID = driverID;
        this.request_time = request_time;
        this.client_onWay = client_onWay;
        this.driver_onWay = driver_onWay;
        this.meetingpointID = meetingpointID;
        this.client_info = client_info;
        this.driver_info = driver_info;
        this.meetingpoint_info = meetingpoint_info;
    }

    public String getId() {
        return id;
    }

    public String getAccepted() {
        return accepted;
    }

    public String getClientID() {
        return clientID;
    }

    public String getDriverID() {
        return driverID;
    }

    public String getClient_onWay() {
        return client_onWay;
    }

    public String getDriver_onWay() {
        return driver_onWay;
    }

    public String getMeetingpointID() {
        return meetingpointID;
    }

    public Timestamp getRequest_time() {
        return request_time;
    }

    public Map<String, String> getClient_info() {
        return client_info;
    }

    public Map<String, String> getDriver_info() {
        return driver_info;
    }

    public Map<String, String> getMeetingpoint_info() {
        return meetingpoint_info;
    }
}
