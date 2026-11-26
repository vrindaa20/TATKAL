package com.tatkalx.model;

/**
 * Represents a Coach belonging to a Train (2A, 3A, SL, CC).
 */
public class Coach {

    private int coachId;
    private int trainId;
    private String coachCode;
    private String coachClass;
    private int totalBerths;
    private int tatkalQuotaBerths;
    private int racQuota;
    private int wlQuota;
    private double baseFarePerKm;

    public Coach() {
    }

    public Coach(int coachId, int trainId, String coachCode, String coachClass,
                 int totalBerths, int tatkalQuotaBerths, int racQuota, int wlQuota,
                 double baseFarePerKm) {
        this.coachId = coachId;
        this.trainId = trainId;
        this.coachCode = coachCode;
        this.coachClass = coachClass;
        this.totalBerths = totalBerths;
        this.tatkalQuotaBerths = tatkalQuotaBerths;
        this.racQuota = racQuota;
        this.wlQuota = wlQuota;
        this.baseFarePerKm = baseFarePerKm;
    }

    public int getCoachId() {
        return coachId;
    }

    public void setCoachId(int coachId) {
        this.coachId = coachId;
    }

    public int getTrainId() {
        return trainId;
    }

    public void setTrainId(int trainId) {
        this.trainId = trainId;
    }

    public String getCoachCode() {
        return coachCode;
    }

    public void setCoachCode(String coachCode) {
        this.coachCode = coachCode;
    }

    public String getCoachClass() {
        return coachClass;
    }

    public void setCoachClass(String coachClass) {
        this.coachClass = coachClass;
    }

    public int getTotalBerths() {
        return totalBerths;
    }

    public void setTotalBerths(int totalBerths) {
        this.totalBerths = totalBerths;
    }

    public int getTatkalQuotaBerths() {
        return tatkalQuotaBerths;
    }

    public void setTatkalQuotaBerths(int tatkalQuotaBerths) {
        this.tatkalQuotaBerths = tatkalQuotaBerths;
    }

    public int getRacQuota() {
        return racQuota;
    }

    public void setRacQuota(int racQuota) {
        this.racQuota = racQuota;
    }

    public int getWlQuota() {
        return wlQuota;
    }

    public void setWlQuota(int wlQuota) {
        this.wlQuota = wlQuota;
    }

    public double getBaseFarePerKm() {
        return baseFarePerKm;
    }

    public void setBaseFarePerKm(double baseFarePerKm) {
        this.baseFarePerKm = baseFarePerKm;
    }

    @Override
    public String toString() {
        return coachCode + " (" + coachClass + ") Tatkal=" + tatkalQuotaBerths
                + ", RAC=" + racQuota + ", WL=" + wlQuota;
    }
}
