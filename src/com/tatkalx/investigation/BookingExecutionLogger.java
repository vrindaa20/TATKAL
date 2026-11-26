package com.tatkalx.investigation;

import com.tatkalx.dao.ForensicDao;
import com.tatkalx.filehandling.FileLogManager;
import com.tatkalx.model.BookingEvent;
import com.tatkalx.model.ThreadLog;

import java.io.IOException;
import java.util.List;

/**
 * Component #34: Booking Execution Logger.
 * Persists thread traces and booking lifecycle events to both SQLite and character-stream log files.
 */
public class BookingExecutionLogger {

    private final FileLogManager fileLogManager;
    private final ForensicDao forensicDao;

    public BookingExecutionLogger(FileLogManager fileLogManager, ForensicDao forensicDao) {
        this.fileLogManager = fileLogManager;
        this.forensicDao = forensicDao;
    }

    public void logBookingEventsToFile(List<BookingEvent> events) {
        try {
            fileLogManager.appendBookingEvents(events);
        } catch (IOException ex) {
            fileLogManager.appendErrorLog("BookingExecutionLogger", "Failed writing booking events: " + ex.getMessage());
        }
    }

    public void logThreadExecution(List<ThreadLog> logs, boolean persistToDb) {
        try {
            fileLogManager.appendThreadLogs(logs);
        } catch (IOException ex) {
            fileLogManager.appendErrorLog("BookingExecutionLogger", "Failed writing thread logs: " + ex.getMessage());
        }
        if (persistToDb && forensicDao != null && logs != null && !logs.isEmpty()) {
            forensicDao.insertThreadLogsBatch(logs);
        }
    }

    public void logError(String source, String errorMessage) {
        fileLogManager.appendErrorLog(source, errorMessage);
    }
}
