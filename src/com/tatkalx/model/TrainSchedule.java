package com.tatkalx.model;

/**
 * Represents a scheduled run of a train on a specific journey date.
 */
public class TrainSchedule {

    private long scheduleId;
    private int trainId;
    private int routeId;
    private String trainNumber;
    private String trainName;
    private int totalDistanceKm;
    private String journeyDate;
    private String departureTime;
    private String arrivalTime;
    private String tatkalOpenTime;
    private String tatkalCloseTime;
    private String scheduleStatus;

    public TrainSchedule() {
        this.tatkalOpenTime = "10:00:00";
        this.tatkalCloseTime = "11:00:00";
        this.scheduleStatus = "SCHEDULED";
    }

    public TrainSchedule(long scheduleId, int trainId, int routeId, String trainNumber,
                         String trainName, int totalDistanceKm, String journeyDate,
                         String departureTime, String arrivalTime,
                         String tatkalOpenTime, String tatkalCloseTime, String scheduleStatus) {
        this.scheduleId = scheduleId;
        this.trainId = trainId;
        this.routeId = routeId;
        this.trainNumber = trainNumber;
        this.trainName = trainName;
        this.totalDistanceKm = totalDistanceKm;
        this.journeyDate = journeyDate;
        this.departureTime = departureTime;
        this.arrivalTime = arrivalTime;
        this.tatkalOpenTime = tatkalOpenTime;
        this.tatkalCloseTime = tatkalCloseTime;
        this.scheduleStatus = scheduleStatus;
    }

    public long getScheduleId() {
        return scheduleId;
    }

    public void setScheduleId(long scheduleId) {
        this.scheduleId = scheduleId;
    }

    public int getTrainId() {
        return trainId;
    }

    public void setTrainId(int trainId) {
        this.trainId = trainId;
    }

    public int getRouteId() {
        return routeId;
    }

    public void setRouteId(int routeId) {
        this.routeId = routeId;
    }

    public String getTrainNumber() {
        return trainNumber;
    }

    public void setTrainNumber(String trainNumber) {
        this.trainNumber = trainNumber;
    }

    public String getTrainName() {
        return trainName;
    }

    public void setTrainName(String trainName) {
        this.trainName = trainName;
    }

    public int getTotalDistanceKm() {
        return totalDistanceKm;
    }

    public void setTotalDistanceKm(int totalDistanceKm) {
        this.totalDistanceKm = totalDistanceKm;
    }

    public String getJourneyDate() {
        return journeyDate;
    }

    public void setJourneyDate(String journeyDate) {
        this.journeyDate = journeyDate;
    }

    public String getDepartureTime() {
        return departureTime;
    }

    public void setDepartureTime(String departureTime) {
        this.departureTime = departureTime;
    }

    public String getArrivalTime() {
        return arrivalTime;
    }

    public void setArrivalTime(String arrivalTime) {
        this.arrivalTime = arrivalTime;
    }

    public String getTatkalOpenTime() {
        return tatkalOpenTime;
    }

    public void setTatkalOpenTime(String tatkalOpenTime) {
        this.tatkalOpenTime = tatkalOpenTime;
    }

    public String getTatkalCloseTime() {
        return tatkalCloseTime;
    }

    public void setTatkalCloseTime(String tatkalCloseTime) {
        this.tatkalCloseTime = tatkalCloseTime;
    }

    public String getScheduleStatus() {
        return scheduleStatus;
    }

    public void setScheduleStatus(String scheduleStatus) {
        this.scheduleStatus = scheduleStatus;
    }

    @Override
    public String toString() {
        return String.format("%s %s | Date: %s | %s -> %s",
                trainNumber, trainName, journeyDate, departureTime, arrivalTime);
    }
}
