package com.tatkalx.investigation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Structured report object returned by each Failure Lab scenario.
 */
public class FailureLabResult {

    private final String scenarioCode;
    private final String scenarioTitle;
    private final List<String> affectedBookingIds = new ArrayList<>();
    private final String threadInfo;
    private final List<String> errorMessages = new ArrayList<>();
    private final String recoveryAction;
    private final String finalInventoryStatus;
    private final boolean inventoryConsistent;

    public FailureLabResult(String scenarioCode, String scenarioTitle, String threadInfo,
                            String recoveryAction, String finalInventoryStatus, boolean inventoryConsistent) {
        this.scenarioCode = scenarioCode;
        this.scenarioTitle = scenarioTitle;
        this.threadInfo = threadInfo;
        this.recoveryAction = recoveryAction;
        this.finalInventoryStatus = finalInventoryStatus;
        this.inventoryConsistent = inventoryConsistent;
    }

    public void addAffectedBookingId(String bookingId) {
        if (bookingId != null) {
            affectedBookingIds.add(bookingId);
        }
    }

    public void addErrorMessage(String message) {
        if (message != null) {
            errorMessages.add(message);
        }
    }

    public String getScenarioCode() {
        return scenarioCode;
    }

    public String getScenarioTitle() {
        return scenarioTitle;
    }

    public List<String> getAffectedBookingIds() {
        return Collections.unmodifiableList(affectedBookingIds);
    }

    public String getThreadInfo() {
        return threadInfo;
    }

    public List<String> getErrorMessages() {
        return Collections.unmodifiableList(errorMessages);
    }

    public String getRecoveryAction() {
        return recoveryAction;
    }

    public String getFinalInventoryStatus() {
        return finalInventoryStatus;
    }

    public boolean isInventoryConsistent() {
        return inventoryConsistent;
    }
}
