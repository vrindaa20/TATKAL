package com.tatkalx.model;

/**
 * Represents a Passenger user aligned with the Users + Passengers SQLite tables.
 * Implements Comparable so passengers have a natural ordering by passengerId.
 */
public class Passenger extends User implements Comparable<Passenger> {

    private int passengerId;
    private int age;
    private String gender;
    private String idProofType;
    private String idProofNumber;
    private boolean verified;
    private String priorityCategory;
    private String berthPreference;
    private double walletBalance;

    public Passenger() {
        super();
        this.role = "PASSENGER";
        this.priorityCategory = "NORMAL";
        this.berthPreference = "ANY";
        this.walletBalance = 10000.0;
    }

    public Passenger(int userId, int passengerId, String username, String fullName,
                     String email, String phone, int age, String gender,
                     String idProofType, String idProofNumber, boolean verified,
                     String priorityCategory, String berthPreference, double walletBalance) {
        super(userId, username, "", fullName, email, phone, "PASSENGER", true, "");
        this.passengerId = passengerId;
        this.age = age;
        this.gender = gender;
        this.idProofType = idProofType;
        this.idProofNumber = idProofNumber;
        this.verified = verified;
        this.priorityCategory = priorityCategory;
        this.berthPreference = berthPreference;
        this.walletBalance = walletBalance;
    }

    @Override
    public String getRoleDescription() {
        return "Passenger (" + priorityCategory + ", verified=" + verified + ")";
    }

    @Override
    public int compareTo(Passenger other) {
        return Integer.compare(this.passengerId, other.passengerId);
    }

    public int getPassengerId() {
        return passengerId;
    }

    public void setPassengerId(int passengerId) {
        this.passengerId = passengerId;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getIdProofType() {
        return idProofType;
    }

    public void setIdProofType(String idProofType) {
        this.idProofType = idProofType;
    }

    public String getIdProofNumber() {
        return idProofNumber;
    }

    public void setIdProofNumber(String idProofNumber) {
        this.idProofNumber = idProofNumber;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public String getPriorityCategory() {
        return priorityCategory;
    }

    public void setPriorityCategory(String priorityCategory) {
        this.priorityCategory = priorityCategory;
    }

    public String getBerthPreference() {
        return berthPreference;
    }

    public void setBerthPreference(String berthPreference) {
        this.berthPreference = berthPreference;
    }

    public double getWalletBalance() {
        return walletBalance;
    }

    public void setWalletBalance(double walletBalance) {
        this.walletBalance = walletBalance;
    }

    @Override
    public String toString() {
        return String.format("Passenger[id=%d, name=%s, age=%d, cat=%s, verified=%s, idProof=%s]",
                passengerId, fullName, age, priorityCategory, verified, idProofNumber);
    }
}
