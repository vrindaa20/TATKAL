package com.tatkalx.model;

/**
 * Passenger status notification aligned with the Notifications table.
 */
public class Notification {

    private long notificationId;
    private long sessionId;
    private int passengerId;
    private String bookingId;
    private String title;
    private String message;
    private String channel;
    private boolean read;
    private String createdAt;

    public Notification() {
        this.channel = "TERMINAL";
    }

    public Notification(long notificationId, long sessionId, int passengerId, String bookingId,
                        String title, String message, String channel, boolean read, String createdAt) {
        this.notificationId = notificationId;
        this.sessionId = sessionId;
        this.passengerId = passengerId;
        this.bookingId = bookingId;
        this.title = title;
        this.message = message;
        this.channel = channel;
        this.read = read;
        this.createdAt = createdAt;
    }

    public long getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(long notificationId) {
        this.notificationId = notificationId;
    }

    public long getSessionId() {
        return sessionId;
    }

    public void setSessionId(long sessionId) {
        this.sessionId = sessionId;
    }

    public int getPassengerId() {
        return passengerId;
    }

    public void setPassengerId(int passengerId) {
        this.passengerId = passengerId;
    }

    public String getBookingId() {
        return bookingId;
    }

    public void setBookingId(String bookingId) {
        this.bookingId = bookingId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return String.format("[%s] %s: %s (%s)", createdAt, title, message, read ? "READ" : "NEW");
    }
}
