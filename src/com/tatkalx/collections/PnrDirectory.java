package com.tatkalx.collections;

import com.tatkalx.model.PnrRecord;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.NavigableSet;
import java.util.TreeSet;

/**
 * Maintains generated PNR records using:
 * - TreeSet<PnrRecord> for automatically sorted PNRs and range queries (O(log n))
 * - HashMap<String, PnrRecord> for O(1) instant lookup by pnrNumber
 */
public class PnrDirectory {

    private final TreeSet<PnrRecord> sortedPnrTree = new TreeSet<>();
    private final HashMap<String, PnrRecord> pnrLookupMap = new HashMap<>();

    public synchronized boolean registerPnr(PnrRecord record) {
        if (record == null || record.getPnrNumber() == null) {
            return false;
        }
        if (pnrLookupMap.containsKey(record.getPnrNumber())) {
            return false;
        }
        sortedPnrTree.add(record);
        pnrLookupMap.put(record.getPnrNumber(), record);
        return true;
    }

    public synchronized PnrRecord lookupPnr(String pnrNumber) {
        return pnrLookupMap.get(pnrNumber);
    }

    /**
     * Updates status in-place (pnrNumber is unchanged, so TreeSet ordering remains valid).
     */
    public synchronized boolean updateStatus(String pnrNumber, String newStatus) {
        PnrRecord record = pnrLookupMap.get(pnrNumber);
        if (record == null) {
            return false;
        }
        record.setCurrentStatus(newStatus);
        return true;
    }

    /**
     * Returns all PNRs in natural ascending PNR order.
     */
    public synchronized List<PnrRecord> getAllSortedPnrs() {
        return Collections.unmodifiableList(new ArrayList<>(sortedPnrTree));
    }

    /**
     * Demonstrates TreeSet NavigableSet range query between two PNR prefixes.
     */
    public synchronized List<PnrRecord> getPnrsInRange(String fromPnrInclusive, String toPnrInclusive) {
        PnrRecord lower = new PnrRecord(0, fromPnrInclusive, 0, 0, "", false, "");
        PnrRecord upper = new PnrRecord(0, toPnrInclusive, 0, 0, "", false, "");
        NavigableSet<PnrRecord> subset = sortedPnrTree.subSet(lower, true, upper, true);
        return new ArrayList<>(subset);
    }

    public synchronized PnrRecord getLowestPnr() {
        return sortedPnrTree.isEmpty() ? null : sortedPnrTree.first();
    }

    public synchronized PnrRecord getHighestPnr() {
        return sortedPnrTree.isEmpty() ? null : sortedPnrTree.last();
    }
}
