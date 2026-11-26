package com.tatkalx.service;

import com.tatkalx.model.BookingEvent;
import com.tatkalx.model.BookingRequest;
import com.tatkalx.model.Payment;
import com.tatkalx.model.PnrRecord;
import com.tatkalx.model.Ticket;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Encapsulates the complete outcome of processing a single BookingRequest,
 * including issued Ticket, PNR, Payment, and recorded forensic events.
 */
public class BookingOutcome {

    private final boolean success;
    private final String outcomeStatus;
    private final String message;
    private final BookingRequest request;
    private final Ticket ticket;
    private final PnrRecord pnrRecord;
    private final Payment payment;
    private final List<BookingEvent> events;

    public BookingOutcome(boolean success, String outcomeStatus, String message,
                          BookingRequest request, Ticket ticket, PnrRecord pnrRecord,
                          Payment payment, List<BookingEvent> events) {
        this.success = success;
        this.outcomeStatus = outcomeStatus;
        this.message = message;
        this.request = request;
        this.ticket = ticket;
        this.pnrRecord = pnrRecord;
        this.payment = payment;
        this.events = events == null ? new ArrayList<>() : new ArrayList<>(events);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getOutcomeStatus() {
        return outcomeStatus;
    }

    public String getMessage() {
        return message;
    }

    public BookingRequest getRequest() {
        return request;
    }

    public Ticket getTicket() {
        return ticket;
    }

    public PnrRecord getPnrRecord() {
        return pnrRecord;
    }

    public Payment getPayment() {
        return payment;
    }

    public List<BookingEvent> getEvents() {
        return Collections.unmodifiableList(events);
    }
}
