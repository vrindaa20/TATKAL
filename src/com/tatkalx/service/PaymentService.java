package com.tatkalx.service;

import com.tatkalx.exception.BookingTimeoutException;
import com.tatkalx.exception.PaymentFailedException;
import com.tatkalx.model.Passenger;
import com.tatkalx.model.Payment;
import com.tatkalx.utility.IdGenerator;
import com.tatkalx.utility.TimeUtils;

/**
 * Simulates payment gateway authorization, latency, timeout detection, and wallet debit.
 */
public class PaymentService {

    private long simulatedLatencyMs = 5L;
    private long paymentTimeoutMs = 3000L;
    private boolean forceNextFailure = false;
    private boolean forceNextTimeout = false;

    public void setSimulatedLatencyMs(long simulatedLatencyMs) {
        this.simulatedLatencyMs = Math.max(0L, simulatedLatencyMs);
    }

    public void setPaymentTimeoutMs(long paymentTimeoutMs) {
        this.paymentTimeoutMs = Math.max(100L, paymentTimeoutMs);
    }

    public synchronized void setForceNextFailure(boolean forceNextFailure) {
        this.forceNextFailure = forceNextFailure;
    }

    public synchronized void setForceNextTimeout(boolean forceNextTimeout) {
        this.forceNextTimeout = forceNextTimeout;
    }

    /**
     * Authorizes payment for a booking request. Throws PaymentFailedException or BookingTimeoutException
     * if gateway fails, wallet balance is insufficient, or latency exceeds timeout.
     */
    public Payment processPayment(String bookingId, long sessionId, Passenger passenger,
                                  double amount, String method)
            throws PaymentFailedException, BookingTimeoutException {

        String initiatedAt = TimeUtils.nowTimestamp();
        long start = System.currentTimeMillis();
        String gatewayRef = IdGenerator.nextGatewayRef(method);

        boolean shouldFail;
        boolean shouldTimeout;
        synchronized (this) {
            shouldFail = this.forceNextFailure;
            shouldTimeout = this.forceNextTimeout;
            this.forceNextFailure = false;
            this.forceNextTimeout = false;
        }

        if (simulatedLatencyMs > 0) {
            try {
                Thread.sleep(simulatedLatencyMs);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new PaymentFailedException("Payment interrupted for booking " + bookingId);
            }
        }

        long elapsed = System.currentTimeMillis() - start;
        if (shouldTimeout || elapsed > paymentTimeoutMs) {
            throw new BookingTimeoutException(
                    "Payment gateway timeout for booking " + bookingId + " after " + Math.max(elapsed, paymentTimeoutMs) + " ms");
        }
        if (shouldFail) {
            throw new PaymentFailedException("Gateway declined transaction " + gatewayRef + " for booking " + bookingId);
        }

        synchronized (passenger) {
            if ("WALLET".equalsIgnoreCase(method) && passenger.getWalletBalance() < amount) {
                throw new PaymentFailedException(
                        String.format("Insufficient wallet balance (Rs %.2f < Rs %.2f)",
                                passenger.getWalletBalance(), amount));
            }
            if ("WALLET".equalsIgnoreCase(method)) {
                passenger.setWalletBalance(passenger.getWalletBalance() - amount);
            }
        }

        return new Payment(
                IdGenerator.nextPaymentId(),
                bookingId,
                sessionId,
                passenger.getPassengerId(),
                amount,
                method == null ? "UPI" : method.toUpperCase(),
                gatewayRef,
                "SUCCESS",
                elapsed,
                initiatedAt,
                TimeUtils.nowTimestamp()
        );
    }
}
