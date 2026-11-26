package com.tatkalx.reporting;

import com.tatkalx.model.BookingRequest;
import com.tatkalx.model.Passenger;
import com.tatkalx.model.Payment;
import com.tatkalx.model.Refund;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Component #39: Booking Analytics powered by Lambda Expressions, Stream API
 * (filter, map, sorted, collect), and Method References.
 */
public class StreamAnalyticsService {

    public List<BookingRequest> filterConfirmedBookings(List<BookingRequest> requests) {
        return requests.stream()
                .filter(r -> "CONFIRMED".equalsIgnoreCase(r.getStatus()))
                .collect(Collectors.toList());
    }

    public List<String> findWaitingListPassengers(List<BookingRequest> requests) {
        return requests.stream()
                .filter(r -> "WL".equalsIgnoreCase(r.getStatus()))
                .map(r -> r.getPassengerName() + " (ID:" + r.getPassengerId() + ", Booking:" + r.getBookingId() + ")")
                .collect(Collectors.toList());
    }

    public List<BookingRequest> sortBookingsByTime(List<BookingRequest> requests) {
        return requests.stream()
                .sorted(Comparator.comparing(BookingRequest::getRequestTimestamp)
                        .thenComparing(BookingRequest::getBookingId))
                .collect(Collectors.toList());
    }

    public List<BookingRequest> filterFailedBookings(List<BookingRequest> requests) {
        return requests.stream()
                .filter(r -> "FAILED".equalsIgnoreCase(r.getStatus()) || "TIMEOUT".equalsIgnoreCase(r.getStatus()))
                .collect(Collectors.toList());
    }

    public long countSuccessfulPayments(List<Payment> payments) {
        return payments.stream()
                .filter(p -> "SUCCESS".equalsIgnoreCase(p.getPaymentStatus()))
                .count();
    }

    public double calculateTotalSuccessfulRevenue(List<Payment> payments) {
        return payments.stream()
                .filter(p -> "SUCCESS".equalsIgnoreCase(p.getPaymentStatus()))
                .map(Payment::getAmount)
                .mapToDouble(Double::doubleValue)
                .sum();
    }

    public double calculateTotalRefunds(List<Refund> refunds) {
        return refunds.stream()
                .filter(r -> "PROCESSED".equalsIgnoreCase(r.getRefundStatus()))
                .map(Refund::getRefundAmount)
                .mapToDouble(Double::doubleValue)
                .sum();
    }

    public Map<String, Long> getStatusBreakdown(List<BookingRequest> requests) {
        return requests.stream()
                .collect(Collectors.groupingBy(BookingRequest::getStatus, Collectors.counting()));
    }

    public Map<Long, Long> getTrainScheduleBookingCounts(List<BookingRequest> requests) {
        return requests.stream()
                .collect(Collectors.groupingBy(BookingRequest::getScheduleId, Collectors.counting()));
    }

    public Map<Integer, List<String>> getPassengerWiseBookingIds(List<BookingRequest> requests) {
        return requests.stream()
                .collect(Collectors.groupingBy(
                        BookingRequest::getPassengerId,
                        Collectors.mapping(BookingRequest::getBookingId, Collectors.toList())
                ));
    }

    public List<String> getVerifiedPassengerNamesSorted(List<Passenger> passengers) {
        return passengers.stream()
                .filter(Passenger::isVerified)
                .map(Passenger::getFullName)
                .sorted(String::compareToIgnoreCase)
                .collect(Collectors.toList());
    }
}
