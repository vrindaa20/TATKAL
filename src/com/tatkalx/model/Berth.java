package com.tatkalx.model;

/**
 * Represents an individual physical berth inside a coach.
 */
public class Berth {

    private int berthId;
    private int coachId;
    private String coachCode;
    private int berthNumber;
    private String berthType;
    private boolean tatkalReserved;
    private boolean racShared;

    public Berth() {
    }

    public Berth(int berthId, int coachId, String coachCode, int berthNumber,
                 String berthType, boolean tatkalReserved, boolean racShared) {
        this.berthId = berthId;
        this.coachId = coachId;
        this.coachCode = coachCode;
        this.berthNumber = berthNumber;
        this.berthType = berthType;
        this.tatkalReserved = tatkalReserved;
        this.racShared = racShared;
    }

    public String getFormattedLabel() {
        return coachCode + "-" + berthNumber + " (" + berthType + ")";
    }

    public int getBerthId() {
        return berthId;
    }

    public void setBerthId(int berthId) {
        this.berthId = berthId;
    }

    public int getCoachId() {
        return coachId;
    }

    public void setCoachId(int coachId) {
        this.coachId = coachId;
    }

    public String getCoachCode() {
        return coachCode;
    }

    public void setCoachCode(String coachCode) {
        this.coachCode = coachCode;
    }

    public int getBerthNumber() {
        return berthNumber;
    }

    public void setBerthNumber(int berthNumber) {
        this.berthNumber = berthNumber;
    }

    public String getBerthType() {
        return berthType;
    }

    public void setBerthType(String berthType) {
        this.berthType = berthType;
    }

    public boolean isTatkalReserved() {
        return tatkalReserved;
    }

    public void setTatkalReserved(boolean tatkalReserved) {
        this.tatkalReserved = tatkalReserved;
    }

    public boolean isRacShared() {
        return racShared;
    }

    public void setRacShared(boolean racShared) {
        this.racShared = racShared;
    }
}
