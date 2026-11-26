package com.tatkalx.reporting;

import com.tatkalx.model.BookingRequest;
import com.tatkalx.model.Payment;
import com.tatkalx.model.Refund;
import com.tatkalx.utility.ConsoleFormatter;

import java.util.List;
import java.util.Map;

/**
 * Formats Stream API analytical reports for clean terminal display.
 */
public class ReportFormatter {

    private final StreamAnalyticsService analytics;

    public ReportFormatter(StreamAnalyticsService analytics) {
        this.analytics = analytics;
    }

    public void printFullAnalyticsReport(List<BookingRequest> requests,
                                         List<Payment> payments,
                                         List<Refund> refunds) {
        ConsoleFormatter.printBanner("TATKAL-X | STREAM API ANALYTICAL INTELLIGENCE REPORT");

        List<BookingRequest> confirmed = analytics.filterConfirmedBookings(requests);
        List<String> wlPax = analytics.findWaitingListPassengers(requests);
        List<BookingRequest> failed = analytics.filterFailedBookings(requests);
        Map<String, Long> statusMap = analytics.getStatusBreakdown(requests);
        Map<Long, Long> scheduleMap = analytics.getTrainScheduleBookingCounts(requests);
        Map<Integer, List<String>> paxReport = analytics.getPassengerWiseBookingIds(requests);

        System.out.println("  Total Requests Analyzed   : " + requests.size());
        System.out.println("  Confirmed Bookings        : " + confirmed.size());
        System.out.println("  Waiting List Entries      : " + wlPax.size());
        System.out.println("  Failed / Timeout Attempts : " + failed.size());
        System.out.println("  Successful Payments Count : " + analytics.countSuccessfulPayments(payments));
        System.out.printf("  Total Revenue Collected   : Rs %.2f%n", analytics.calculateTotalSuccessfulRevenue(payments));
        System.out.printf("  Total Refunds Processed   : Rs %.2f%n", analytics.calculateTotalRefunds(refunds));
        System.out.println("  Status Breakdown          : " + statusMap);
        System.out.println("  Schedule-wise Counts      : " + scheduleMap);
        System.out.println("  Distinct Passengers Served: " + paxReport.size());

        if (!wlPax.isEmpty()) {
            ConsoleFormatter.printSubHeader("Waiting-List Passengers (via Stream filter + map)");
            wlPax.stream().limit(5).forEach(entry -> System.out.println("  - " + entry));
        }

        ConsoleFormatter.printSubHeader("Earliest 5 Requests Sorted by Timestamp (via Stream sorted + method ref)");
        analytics.sortBookingsByTime(requests).stream()
                .limit(5)
                .map(r -> String.format("  %s | %-10s | %-14s | Status=%s",
                        r.getRequestTimestamp(), r.getBookingId(), r.getPassengerName(), r.getStatus()))
                .forEach(System.out::println);

        ConsoleFormatter.printDivider();
    }
}
