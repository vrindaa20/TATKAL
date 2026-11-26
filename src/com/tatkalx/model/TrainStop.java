package com.tatkalx.model;

/**
 * Represents an intermediate stop along a Train Route.
 * Natural order sorts stops strictly by stopOrder.
 */
public class TrainStop implements Comparable<TrainStop> {

    private int stopId;
    private int routeId;
    private int stationId;
    private String stationCode;
    private String stationName;
    private int stopOrder;
    private int distanceFromOriginKm;
    private int arrivalOffsetMin;
    private int departureOffsetMin;

    public TrainStop() {
    }

    public TrainStop(int stopId, int routeId, int stationId, String stationCode,
                     String stationName, int stopOrder, int distanceFromOriginKm,
                     int arrivalOffsetMin, int departureOffsetMin) {
        this.stopId = stopId;
        this.routeId = routeId;
        this.stationId = stationId;
        this.stationCode = stationCode;
        this.stationName = stationName;
        this.stopOrder = stopOrder;
        this.distanceFromOriginKm = distanceFromOriginKm;
        this.arrivalOffsetMin = arrivalOffsetMin;
        this.departureOffsetMin = departureOffsetMin;
    }

    @Override
    public int compareTo(TrainStop other) {
        return Integer.compare(this.stopOrder, other.stopOrder);
    }

    public int getStopId() {
        return stopId;
    }

    public void setStopId(int stopId) {
        this.stopId = stopId;
    }

    public int getRouteId() {
        return routeId;
    }

    public void setRouteId(int routeId) {
        this.routeId = routeId;
    }

    public int getStationId() {
        return stationId;
    }

    public void setStationId(int stationId) {
        this.stationId = stationId;
    }

    public String getStationCode() {
        return stationCode;
    }

    public void setStationCode(String stationCode) {
        this.stationCode = stationCode;
    }

    public String getStationName() {
        return stationName;
    }

    public void setStationName(String stationName) {
        this.stationName = stationName;
    }

    public int getStopOrder() {
        return stopOrder;
    }

    public void setStopOrder(int stopOrder) {
        this.stopOrder = stopOrder;
    }

    public int getDistanceFromOriginKm() {
        return distanceFromOriginKm;
    }

    public void setDistanceFromOriginKm(int distanceFromOriginKm) {
        this.distanceFromOriginKm = distanceFromOriginKm;
    }

    public int getArrivalOffsetMin() {
        return arrivalOffsetMin;
    }

    public void setArrivalOffsetMin(int arrivalOffsetMin) {
        this.arrivalOffsetMin = arrivalOffsetMin;
    }

    public int getDepartureOffsetMin() {
        return departureOffsetMin;
    }

    public void setDepartureOffsetMin(int departureOffsetMin) {
        this.departureOffsetMin = departureOffsetMin;
    }

    @Override
    public String toString() {
        return String.format("#%d %s (%s) - %d km", stopOrder, stationCode, stationName, distanceFromOriginKm);
    }
}
