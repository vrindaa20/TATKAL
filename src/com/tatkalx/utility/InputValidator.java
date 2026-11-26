package com.tatkalx.utility;

import com.tatkalx.exception.BookingValidationException;

/**
 * Centralized validation utility for passenger profiles, booking inputs, and PNR formats.
 */
public final class InputValidator {

    private InputValidator() {
    }

    public static void validatePassengerRegistration(String username, String fullName,
                                                     String email, String phone,
                                                     int age, String idProofNumber)
            throws BookingValidationException {
        if (isBlank(username) || username.trim().length() < 3) {
            throw new BookingValidationException("Username must be at least 3 characters.");
        }
        if (isBlank(fullName)) {
            throw new BookingValidationException("Full name cannot be empty.");
        }
        if (isBlank(email) || !email.contains("@") || !email.contains(".")) {
            throw new BookingValidationException("Invalid email address format.");
        }
        if (isBlank(phone) || phone.trim().length() != 10 || !isDigitsOnly(phone.trim())) {
            throw new BookingValidationException("Phone number must be exactly 10 digits.");
        }
        if (age < 1 || age > 120) {
            throw new BookingValidationException("Passenger age must be between 1 and 120.");
        }
        if (isBlank(idProofNumber) || idProofNumber.trim().length() < 5) {
            throw new BookingValidationException("Valid government ID proof number is required.");
        }
    }

    public static void validateBookingRequestFields(int passengerId, long scheduleId,
                                                    String coachClass, int requestedSeats)
            throws BookingValidationException {
        if (passengerId <= 0) {
            throw new BookingValidationException("Invalid passenger ID.");
        }
        if (scheduleId <= 0) {
            throw new BookingValidationException("Invalid train schedule ID.");
        }
        if (!isValidCoachClass(coachClass)) {
            throw new BookingValidationException("Unsupported coach class: " + coachClass);
        }
        if (requestedSeats < 1 || requestedSeats > 4) {
            throw new BookingValidationException("Tatkal allows 1 to 4 seats per booking request.");
        }
    }

    public static boolean isValidPnr(String pnr) {
        return pnr != null && pnr.trim().length() == 10 && isDigitsOnly(pnr.trim());
    }

    public static boolean isValidCoachClass(String coachClass) {
        if (coachClass == null) {
            return false;
        }
        String upper = coachClass.trim().toUpperCase();
        return "2A".equals(upper) || "3A".equals(upper) || "SL".equals(upper) || "CC".equals(upper);
    }

    public static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static boolean isDigitsOnly(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (!Character.isDigit(value.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}
