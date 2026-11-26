package com.tatkalx.model;

/**
 * Represents a train aligned with the Trains table.
 */
public class Train {

    private int trainId;
    private String trainNumber;
    private String trainName;
    private String trainType;
    private int totalCoaches;
    private boolean tatkalEnabled;
    private String status;

    public Train() {
        this.tatkalEnabled = true;
        this.status = "ACTIVE";
    }

    public Train(int trainId, String trainNumber, String trainName, String trainType,
                 int totalCoaches, boolean tatkalEnabled, String status) {
        this.trainId = trainId;
        this.trainNumber = trainNumber;
        this.trainName = trainName;
        this.trainType = trainType;
        this.totalCoaches = totalCoaches;
        this.tatkalEnabled = tatkalEnabled;
        this.status = status;
    }

    public int getTrainId() {
        return trainId;
    }

    public void setTrainId(int trainId) {
        this.trainId = trainId;
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

    public String getTrainType() {
        return trainType;
    }

    public void setTrainType(String trainType) {
        this.trainType = trainType;
    }

    public int getTotalCoaches() {
        return totalCoaches;
    }

    public void setTotalCoaches(int totalCoaches) {
        this.totalCoaches = totalCoaches;
    }

    public boolean isTatkalEnabled() {
        return tatkalEnabled;
    }

    public void setTatkalEnabled(boolean tatkalEnabled) {
        this.tatkalEnabled = tatkalEnabled;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return trainNumber + " " + trainName + " [" + trainType + "]";
    }
}
