package de.hochschule.reisebroker.model;

import java.util.UUID;

public class Booking {
    private String id;
    private String hotelId;
    private int timeBlock;
    private String customerId;
    private BookingStatus status;

    public enum BookingStatus {
        REQUESTED,    // Anfrage wurde gestellt
        CONFIRMED,    // Buchung wurde bestätigt
        FAILED,       // Buchung konnte nicht durchgeführt werden
        COMPENSATED   // Buchung wurde storniert (Kompensation)
    }

    public Booking(String hotelId, int timeBlock, String customerId) {
        this.id = UUID.randomUUID().toString();
        this.hotelId = hotelId;
        this.timeBlock = timeBlock;
        this.customerId = customerId;
        this.status = BookingStatus.REQUESTED;
    }

    public String getId() {
        return id;
    }

    public String getHotelId() {
        return hotelId;
    }

    public int getTimeBlock() {
        return timeBlock;
    }

    public String getCustomerId() {
        return customerId;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public void setStatus(BookingStatus status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "Booking{" +
                "id='" + id + '\'' +
                ", hotelId='" + hotelId + '\'' +
                ", timeBlock=" + timeBlock +
                ", customerId='" + customerId + '\'' +
                ", status=" + status +
                '}';
    }
}