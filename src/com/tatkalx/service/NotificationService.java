package com.tatkalx.service;

import com.tatkalx.model.Notification;
import com.tatkalx.utility.IdGenerator;
import com.tatkalx.utility.TimeUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages passenger notifications for confirmations, RAC/WL promotions, failures, and refunds.
 */
public class NotificationService {

    private final Map<Integer, List<Notification>> notificationsByPassenger = new HashMap<>();

    public synchronized Notification sendNotification(long sessionId, int passengerId,
                                                      String bookingId, String title, String message) {
        Notification notification = new Notification(
                IdGenerator.nextNotificationId(),
                sessionId,
                passengerId,
                bookingId,
                title,
                message,
                "TERMINAL",
                false,
                TimeUtils.nowTimestamp()
        );
        List<Notification> list = notificationsByPassenger.get(passengerId);
        if (list == null) {
            list = new ArrayList<>();
            notificationsByPassenger.put(passengerId, list);
        }
        list.add(notification);
        return notification;
    }

    public synchronized List<Notification> getNotificationsForPassenger(int passengerId) {
        List<Notification> list = notificationsByPassenger.get(passengerId);
        if (list == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<>(list));
    }

    public synchronized void markAllRead(int passengerId) {
        List<Notification> list = notificationsByPassenger.get(passengerId);
        if (list != null) {
            for (Notification n : list) {
                n.setRead(true);
            }
        }
    }
}
