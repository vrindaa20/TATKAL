package com.tatkalx.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a train route and its ordered intermediate stops.
 */
public class Route {

    private int routeId;
    private String routeCode;
    private int originStationId;
    private int destinationStationId;
    private int totalDistanceKm;
    private final List<TrainStop> stops = new ArrayList<>();

    public Route() {
    }

    public Route(int routeId, String routeCode, int originStationId,
                 int destinationStationId, int totalDistanceKm) {
        this.routeId = routeId;
        this.routeCode = routeCode;
        this.originStationId = originStationId;
        this.destinationStationId = destinationStationId;
        this.totalDistanceKm = totalDistanceKm;
    }

    public void addStop(TrainStop stop) {
        stops.add(stop);
        Collections.sort(stops);
    }

    public List<TrainStop> getStops() {
        return Collections.unmodifiableList(stops);
    }

    public int getRouteId() {
        return routeId;
    }

    public void setRouteId(int routeId) {
        this.routeId = routeId;
    }

    public String getRouteCode() {
        return routeCode;
    }

    public void setRouteCode(String routeCode) {
        this.routeCode = routeCode;
    }

    public int getOriginStationId() {
        return originStationId;
    }

    public void setOriginStationId(int originStationId) {
        this.originStationId = originStationId;
    }

    public int getDestinationStationId() {
        return destinationStationId;
    }

    public void setDestinationStationId(int destinationStationId) {
        this.destinationStationId = destinationStationId;
    }

    public int getTotalDistanceKm() {
        return totalDistanceKm;
    }

    public void setTotalDistanceKm(int totalDistanceKm) {
        this.totalDistanceKm = totalDistanceKm;
    }
}
