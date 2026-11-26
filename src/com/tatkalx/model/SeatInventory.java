package com.tatkalx.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Isolated per-session seat inventory for one (sessionId, scheduleId, coachId).
 * Holds an intrinsic monitor lock object so critical sections can protect check-then-act allocation.
 */
public class SeatInventory {

    private long inventoryId;
    private long sessionId;
    private long scheduleId;
    private int coachId;
    private String coachCode;
    private String coachClass;

    private int totalTatkalSeats;
    private int availableTatkalSeats;
    private int confirmedCount;

    private int racCapacity;
    private int racAllocated;

    private int wlCapacity;
    private int wlAllocated;

    private int versionNum = 1;

    private final Object monitorLock = new Object();
    private final LinkedList<Berth> availableBerthPool = new LinkedList<>();
    private final Map<Long, List<Berth>> allocatedBerthsByTicket = new HashMap<>();
    private final Set<Integer> occupiedBerthIds = new HashSet<>();

    public SeatInventory() {
    }

    public SeatInventory(long sessionId, long scheduleId, Coach coach, List<Berth> initialBerths) {
        this.sessionId = sessionId;
        this.scheduleId = scheduleId;
        this.coachId = coach.getCoachId();
        this.coachCode = coach.getCoachCode();
        this.coachClass = coach.getCoachClass();
        this.totalTatkalSeats = coach.getTatkalQuotaBerths();
        this.availableTatkalSeats = coach.getTatkalQuotaBerths();
        this.confirmedCount = 0;
        this.racCapacity = coach.getRacQuota();
        this.racAllocated = 0;
        this.wlCapacity = coach.getWlQuota();
        this.wlAllocated = 0;

        if (initialBerths != null) {
            int limit = Math.min(totalTatkalSeats, initialBerths.size());
            for (int i = 0; i < limit; i++) {
                availableBerthPool.addLast(initialBerths.get(i));
            }
        }
    }

    /**
     * Clones this inventory cleanly for a new simulation session so experiments never interfere.
     */
    public SeatInventory cloneForSession(long targetSessionId) {
        Coach templateCoach = new Coach(coachId, 0, coachCode, coachClass,
                totalTatkalSeats, totalTatkalSeats, racCapacity, wlCapacity, 1.5);
        List<Berth> freshBerths = new ArrayList<>();
        for (Berth b : availableBerthPool) {
            freshBerths.add(new Berth(b.getBerthId(), b.getCoachId(), b.getCoachCode(),
                    b.getBerthNumber(), b.getBerthType(), b.isTatkalReserved(), b.isRacShared()));
        }
        return new SeatInventory(targetSessionId, scheduleId, templateCoach, freshBerths);
    }

    public Object getMonitorLock() {
        return monitorLock;
    }

    /**
     * Checks inventory conservation invariants:
     * 1) available + confirmed == total
     * 2) availableBerthPool.size() == availableTatkalSeats
     * 3) occupiedBerthIds.size() == confirmedCount
     */
    public boolean isConsistent() {
        synchronized (monitorLock) {
            return availableTatkalSeats >= 0
                    && confirmedCount >= 0
                    && (availableTatkalSeats + confirmedCount == totalTatkalSeats)
                    && availableBerthPool.size() == availableTatkalSeats
                    && occupiedBerthIds.size() == confirmedCount
                    && racAllocated >= 0 && racAllocated <= racCapacity
                    && wlAllocated >= 0 && wlAllocated <= wlCapacity;
        }
    }

    public LinkedList<Berth> getAvailableBerthPool() {
        return availableBerthPool;
    }

    public Map<Long, List<Berth>> getAllocatedBerthsByTicket() {
        return allocatedBerthsByTicket;
    }

    public Set<Integer> getOccupiedBerthIds() {
        return occupiedBerthIds;
    }

    public long getInventoryId() {
        return inventoryId;
    }

    public void setInventoryId(long inventoryId) {
        this.inventoryId = inventoryId;
    }

    public long getSessionId() {
        return sessionId;
    }

    public void setSessionId(long sessionId) {
        this.sessionId = sessionId;
    }

    public long getScheduleId() {
        return scheduleId;
    }

    public void setScheduleId(long scheduleId) {
        this.scheduleId = scheduleId;
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

    public String getCoachClass() {
        return coachClass;
    }

    public void setCoachClass(String coachClass) {
        this.coachClass = coachClass;
    }

    public int getTotalTatkalSeats() {
        return totalTatkalSeats;
    }

    public void setTotalTatkalSeats(int totalTatkalSeats) {
        this.totalTatkalSeats = totalTatkalSeats;
    }

    public int getAvailableTatkalSeats() {
        return availableTatkalSeats;
    }

    public void setAvailableTatkalSeats(int availableTatkalSeats) {
        this.availableTatkalSeats = availableTatkalSeats;
    }

    public int getConfirmedCount() {
        return confirmedCount;
    }

    public void setConfirmedCount(int confirmedCount) {
        this.confirmedCount = confirmedCount;
    }

    public int getRacCapacity() {
        return racCapacity;
    }

    public void setRacCapacity(int racCapacity) {
        this.racCapacity = racCapacity;
    }

    public int getRacAllocated() {
        return racAllocated;
    }

    public void setRacAllocated(int racAllocated) {
        this.racAllocated = racAllocated;
    }

    public int getWlCapacity() {
        return wlCapacity;
    }

    public void setWlCapacity(int wlCapacity) {
        this.wlCapacity = wlCapacity;
    }

    public int getWlAllocated() {
        return wlAllocated;
    }

    public void setWlAllocated(int wlAllocated) {
        this.wlAllocated = wlAllocated;
    }

    public int getVersionNum() {
        return versionNum;
    }

    public void incrementVersion() {
        this.versionNum++;
    }

    @Override
    public String toString() {
        return String.format("Inventory[session=%d, coach=%s(%s), avail=%d/%d, conf=%d, rac=%d/%d, wl=%d/%d, consistent=%s]",
                sessionId, coachCode, coachClass, availableTatkalSeats, totalTatkalSeats,
                confirmedCount, racAllocated, racCapacity, wlAllocated, wlCapacity, isConsistent());
    }
}
