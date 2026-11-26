package com.tatkalx.filehandling;

import com.tatkalx.exception.BookingValidationException;
import com.tatkalx.model.BookingRequest;
import com.tatkalx.model.Passenger;
import com.tatkalx.model.Refund;
import com.tatkalx.model.SimulationResult;
import com.tatkalx.model.ThreadLog;
import com.tatkalx.utility.InputValidator;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Component #40: CSV Import & Export service using FileReader, FileWriter,
 * StringTokenizer, input validation, and invalid-record handling.
 */
public class CsvImportExportService {

    public CsvImportExportService() {
        ensureDirectories();
    }

    private void ensureDirectories() {
        File dataDir = new File("data");
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }
        File exportsDir = new File("exports");
        if (!exportsDir.exists()) {
            exportsDir.mkdirs();
        }
    }

    /**
     * Generates a sample passenger CSV file containing both valid rows and
     * intentionally invalid rows to demonstrate validation & error isolation.
     */
    public void createSamplePassengersCsv(String path) throws IOException {
        ensureDirectories();
        try (FileWriter fw = new FileWriter(path);
             BufferedWriter bw = new BufferedWriter(fw)) {
            bw.write("username,fullName,email,phone,age,gender,idProofType,idProofNumber,verified,priorityCategory,berthPreference,walletBalance");
            bw.newLine();
            bw.write("ananya_r,Ananya Rao,ananya@mail.in,9822011223,29,F,AADHAAR,AAD-880011,true,NORMAL,LB,14000");
            bw.newLine();
            bw.write("col_vikram,Col Vikram Nair,vikram@mail.in,9822044556,52,M,PAN,PAN-DEF992,true,DEFENCE,SL,22000");
            bw.newLine();
            bw.write("dr_kavita,Dr Kavita Iyer,kavita@mail.in,9822077889,45,F,PASSPORT,PAS-MED101,true,MEDICAL,LB,18500");
            bw.newLine();
            // Deliberately invalid row 1: invalid age (250) & short phone
            bw.write("bad_age,Invalid AgeUser,bad@mail.in,12345,250,M,AADHAAR,AAD-999,true,NORMAL,ANY,1000");
            bw.newLine();
            // Deliberately invalid row 2: missing columns
            bw.write("incomplete_row,OnlyTwoCols");
            bw.newLine();
        }
    }

    /**
     * Imports passengers using FileReader + StringTokenizer with strict validation.
     */
    public CsvImportResult importPassengersFromCsv(String path) throws IOException {
        CsvImportResult result = new CsvImportResult();
        File file = new File(path);
        if (!file.exists()) {
            throw new IOException("CSV file not found: " + path);
        }

        try (FileReader fr = new FileReader(file);
             BufferedReader br = new BufferedReader(fr)) {
            String line;
            int lineNum = 0;
            while ((line = br.readLine()) != null) {
                lineNum++;
                String trimmed = line.trim();
                if (trimmed.isEmpty() || (lineNum == 1 && trimmed.toLowerCase().startsWith("username,"))) {
                    continue;
                }
                StringTokenizer st = new StringTokenizer(trimmed, ",");
                if (st.countTokens() < 12) {
                    result.addInvalid(lineNum, trimmed, "Expected 12 CSV columns, found " + st.countTokens());
                    continue;
                }

                try {
                    String username = st.nextToken().trim();
                    String fullName = st.nextToken().trim();
                    String email = st.nextToken().trim();
                    String phone = st.nextToken().trim();
                    int age = Integer.parseInt(st.nextToken().trim());
                    String gender = st.nextToken().trim().toUpperCase();
                    String idType = st.nextToken().trim().toUpperCase();
                    String idNum = st.nextToken().trim();
                    boolean verified = Boolean.parseBoolean(st.nextToken().trim());
                    String category = st.nextToken().trim().toUpperCase();
                    String berthPref = st.nextToken().trim().toUpperCase();
                    double wallet = Double.parseDouble(st.nextToken().trim());

                    InputValidator.validatePassengerRegistration(username, fullName, email, phone, age, idNum);
                    if (wallet < 0) {
                        throw new BookingValidationException("Wallet balance cannot be negative.");
                    }

                    Passenger p = new Passenger(0, lineNum, username, fullName, email, phone,
                            age, gender, idType, idNum, verified, category, berthPref, wallet);
                    p.setPasswordHash("pass123");
                    result.addValid(p);
                } catch (NumberFormatException | BookingValidationException ex) {
                    result.addInvalid(lineNum, trimmed, ex.getMessage());
                }
            }
        }
        return result;
    }

    public int exportPassengersToCsv(List<Passenger> passengers, String path) throws IOException {
        ensureDirectories();
        try (FileWriter fw = new FileWriter(path);
             BufferedWriter bw = new BufferedWriter(fw)) {
            bw.write("passenger_id,username,full_name,age,gender,id_proof_type,id_proof_number,verified,priority_category,berth_preference,wallet_balance");
            bw.newLine();
            for (Passenger p : passengers) {
                bw.write(String.format("%d,%s,%s,%d,%s,%s,%s,%s,%s,%s,%.2f",
                        p.getPassengerId(), csvSafe(p.getUsername()), csvSafe(p.getFullName()),
                        p.getAge(), csvSafe(p.getGender()), csvSafe(p.getIdProofType()),
                        csvSafe(p.getIdProofNumber()), p.isVerified(),
                        csvSafe(p.getPriorityCategory()), csvSafe(p.getBerthPreference()), p.getWalletBalance()));
                bw.newLine();
            }
        }
        return passengers.size();
    }

    public int exportBookingHistoryToCsv(List<BookingRequest> requests, String path) throws IOException {
        ensureDirectories();
        try (FileWriter fw = new FileWriter(path);
             BufferedWriter bw = new BufferedWriter(fw)) {
            bw.write("booking_id,session_id,passenger_id,passenger_name,schedule_id,coach_class,requested_seats,priority_score,request_timestamp,thread_name,processing_time_ms,status,failure_reason");
            bw.newLine();
            for (BookingRequest r : requests) {
                bw.write(String.format("%s,%d,%d,%s,%d,%s,%d,%d,%s,%s,%d,%s,%s",
                        csvSafe(r.getBookingId()), r.getSessionId(), r.getPassengerId(),
                        csvSafe(r.getPassengerName()), r.getScheduleId(), csvSafe(r.getCoachClass()),
                        r.getRequestedSeats(), r.getPriorityScore(), csvSafe(r.getRequestTimestamp()),
                        csvSafe(r.getThreadName()), r.getProcessingTimeMs(),
                        csvSafe(r.getStatus()), csvSafe(r.getFailureReason())));
                bw.newLine();
            }
        }
        return requests.size();
    }

    public int exportRefundReportToCsv(List<Refund> refunds, String path) throws IOException {
        ensureDirectories();
        try (FileWriter fw = new FileWriter(path);
             BufferedWriter bw = new BufferedWriter(fw)) {
            bw.write("refund_id,cancellation_id,payment_id,original_amount,deduction_amount,refund_amount,refund_status,processed_at");
            bw.newLine();
            for (Refund r : refunds) {
                bw.write(String.format("%d,%d,%d,%.2f,%.2f,%.2f,%s,%s",
                        r.getRefundId(), r.getCancellationId(), r.getPaymentId(),
                        r.getOriginalAmount(), r.getDeductionAmount(), r.getRefundAmount(),
                        csvSafe(r.getRefundStatus()), csvSafe(r.getProcessedAt())));
                bw.newLine();
            }
        }
        return refunds.size();
    }

    public int exportThreadLogsToCsv(List<ThreadLog> logs, String path) throws IOException {
        ensureDirectories();
        try (FileWriter fw = new FileWriter(path);
             BufferedWriter bw = new BufferedWriter(fw)) {
            bw.write("session_id,booking_id,thread_id,thread_name,priority,state,lock_target,action_detail,logged_at");
            bw.newLine();
            for (ThreadLog log : logs) {
                bw.write(String.format("%d,%s,%d,%s,%d,%s,%s,%s,%s",
                        log.getSessionId(), csvSafe(log.getBookingId()), log.getThreadIdNum(),
                        csvSafe(log.getThreadName()), log.getThreadPriority(),
                        csvSafe(log.getThreadState()), csvSafe(log.getLockTarget()),
                        csvSafe(log.getActionDetail()), csvSafe(log.getLoggedAt())));
                bw.newLine();
            }
        }
        return logs.size();
    }

    public int exportSimulationComparisonToCsv(List<SimulationResult> results, String path) throws IOException {
        ensureDirectories();
        try (FileWriter fw = new FileWriter(path);
             BufferedWriter bw = new BufferedWriter(fw)) {
            bw.write("session_id,universe_name,total_requests,confirmed,rac,wl,failed,duplicates,total_ms,avg_ms,max_ms,success_pct,utilization_pct,consistent");
            bw.newLine();
            for (SimulationResult r : results) {
                bw.write(String.format("%d,%s,%d,%d,%d,%d,%d,%d,%d,%.2f,%d,%.2f,%.2f,%s",
                        r.getSessionId(), csvSafe(r.getUniverseName()), r.getTotalRequests(),
                        r.getConfirmedCount(), r.getRacCount(), r.getWlCount(),
                        r.getFailedCount(), r.getDuplicateAttempts(), r.getTotalTimeMs(),
                        r.getAvgTimeMs(), r.getMaxTimeMs(), r.getSuccessRatePct(),
                        r.getSeatUtilizationPct(), r.isInventoryConsistent()));
                bw.newLine();
            }
        }
        return results.size();
    }

    private String csvSafe(String val) {
        if (val == null) {
            return "";
        }
        return val.replace(",", ";").replace("\n", " ").replace("\r", "");
    }
}
