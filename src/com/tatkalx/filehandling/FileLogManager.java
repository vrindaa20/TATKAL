package com.tatkalx.filehandling;

import com.tatkalx.model.BookingEvent;
import com.tatkalx.model.ThreadLog;
import com.tatkalx.utility.TimeUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Character-stream log manager using FileWriter, FileReader, and StringTokenizer
 * to persist and reconstruct booking execution events, thread traces, and error logs.
 */
public class FileLogManager {

    public static final String BOOKING_LOG_PATH = "logs/booking_events.log";
    public static final String THREAD_LOG_PATH = "logs/thread_execution.log";
    public static final String ERROR_LOG_PATH = "logs/error.log";

    public FileLogManager() {
        ensureLogDirectory();
    }

    private void ensureLogDirectory() {
        File dir = new File("logs");
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    public synchronized void appendBookingEvents(List<BookingEvent> events) throws IOException {
        if (events == null || events.isEmpty()) {
            return;
        }
        ensureLogDirectory();
        try (FileWriter fw = new FileWriter(BOOKING_LOG_PATH, true);
             BufferedWriter bw = new BufferedWriter(fw)) {
            for (BookingEvent ev : events) {
                // Pipe-delimited line; sanitize any '|' in descriptions
                String line = String.join("|",
                        safe(ev.getBookingId()),
                        String.valueOf(ev.getSessionId()),
                        String.valueOf(ev.getPassengerId()),
                        safe(ev.getThreadName()),
                        String.valueOf(ev.getEventSeq()),
                        safe(ev.getEventType()),
                        safe(ev.getEventDescription()),
                        safe(ev.getRequestStatus()),
                        ev.getAvailableSeatsSnapshot() == null ? "NA" : String.valueOf(ev.getAvailableSeatsSnapshot()),
                        ev.getQueuePositionSnapshot() == null ? "NA" : String.valueOf(ev.getQueuePositionSnapshot()),
                        safe(ev.getAllocationResult()),
                        safe(ev.getPaymentResult()),
                        safe(ev.getEventTimestamp())
                );
                bw.write(line);
                bw.newLine();
            }
        }
    }

    /**
     * Reconstructs BookingEvent records for a specific bookingId directly from FileReader + StringTokenizer.
     */
    public synchronized List<BookingEvent> readBookingEventsFromFile(String targetBookingId) throws IOException {
        List<BookingEvent> matched = new ArrayList<>();
        File file = new File(BOOKING_LOG_PATH);
        if (!file.exists()) {
            return matched;
        }
        try (FileReader fr = new FileReader(file);
             BufferedReader br = new BufferedReader(fr)) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                StringTokenizer tokenizer = new StringTokenizer(line, "|");
                if (tokenizer.countTokens() < 13) {
                    continue;
                }
                String bookingId = tokenizer.nextToken();
                if (!bookingId.equals(targetBookingId)) {
                    continue;
                }
                long sessionId = Long.parseLong(tokenizer.nextToken());
                int passengerId = Integer.parseInt(tokenizer.nextToken());
                String threadName = tokenizer.nextToken();
                int seq = Integer.parseInt(tokenizer.nextToken());
                String eventType = tokenizer.nextToken();
                String description = tokenizer.nextToken();
                String status = tokenizer.nextToken();
                String availStr = tokenizer.nextToken();
                String qposStr = tokenizer.nextToken();
                String allocRes = unsanitizeNull(tokenizer.nextToken());
                String payRes = unsanitizeNull(tokenizer.nextToken());
                String timestamp = tokenizer.nextToken();

                Integer avail = "NA".equals(availStr) ? null : Integer.parseInt(availStr);
                Integer qpos = "NA".equals(qposStr) ? null : Integer.parseInt(qposStr);

                matched.add(new BookingEvent(bookingId, sessionId, passengerId, threadName,
                        seq, eventType, description, status, avail, qpos, allocRes, payRes, timestamp));
            }
        }
        return matched;
    }

    public synchronized void appendThreadLogs(List<ThreadLog> logs) throws IOException {
        if (logs == null || logs.isEmpty()) {
            return;
        }
        ensureLogDirectory();
        try (FileWriter fw = new FileWriter(THREAD_LOG_PATH, true);
             BufferedWriter bw = new BufferedWriter(fw)) {
            for (ThreadLog log : logs) {
                bw.write(log.toString());
                bw.newLine();
            }
        }
    }

    public synchronized void appendErrorLog(String source, String message) {
        ensureLogDirectory();
        try (FileWriter fw = new FileWriter(ERROR_LOG_PATH, true);
             BufferedWriter bw = new BufferedWriter(fw)) {
            bw.write(TimeUtils.nowTimestamp() + " [" + source + "] " + message);
            bw.newLine();
        } catch (IOException ignored) {
            // Do not mask primary domain exception if error log fails
        }
    }

    private String safe(String value) {
        if (value == null || value.isEmpty()) {
            return "NONE";
        }
        return value.replace("|", "/").replace("\n", " ").replace("\r", "");
    }

    private String unsanitizeNull(String token) {
        return "NONE".equals(token) ? null : token;
    }
}
