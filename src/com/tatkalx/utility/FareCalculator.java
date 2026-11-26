package com.tatkalx.utility;

import com.tatkalx.model.Coach;

/**
 * Calculates total Tatkal ticket fare including base distance fare,
 * 30% Tatkal dynamic surcharge, 5% GST (AC classes), and convenience fee.
 */
public final class FareCalculator {

    private static final double TATKAL_SURCHARGE_RATE = 0.30;
    private static final double AC_GST_RATE = 0.05;
    private static final double CONVENIENCE_FEE = 35.40;

    private FareCalculator() {
    }

    public static double calculateTotalFare(int distanceKm, Coach coach, int seatCount) {
        double baseFare = Math.max(150.0, distanceKm * coach.getBaseFarePerKm());
        double tatkalSurcharge = baseFare * TATKAL_SURCHARGE_RATE;
        double subTotalPerSeat = baseFare + tatkalSurcharge;

        boolean isAcClass = !"SL".equalsIgnoreCase(coach.getCoachClass());
        double gst = isAcClass ? (subTotalPerSeat * AC_GST_RATE) : 0.0;

        double total = ((subTotalPerSeat + gst) * seatCount) + CONVENIENCE_FEE;
        return Math.round(total * 100.0) / 100.0;
    }
}
