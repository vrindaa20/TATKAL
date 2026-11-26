package com.tatkalx.filehandling;

import com.tatkalx.model.Passenger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Holds valid imported Passenger records alongside invalid CSV rows rejected during validation.
 */
public class CsvImportResult {

    private final List<Passenger> validPassengers = new ArrayList<>();
    private final List<String> invalidRows = new ArrayList<>();

    public void addValid(Passenger p) {
        validPassengers.add(p);
    }

    public void addInvalid(int lineNumber, String rawLine, String reason) {
        invalidRows.add("Line " + lineNumber + " [" + rawLine + "] -> " + reason);
    }

    public List<Passenger> getValidPassengers() {
        return Collections.unmodifiableList(validPassengers);
    }

    public List<String> getInvalidRows() {
        return Collections.unmodifiableList(invalidRows);
    }
}
