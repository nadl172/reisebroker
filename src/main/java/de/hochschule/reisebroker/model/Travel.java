package de.hochschule.reisebroker.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Travel {
    private String id;
    private String customerId;
    private List<Booking> bookings;
    private TravelStatus status;

    public enum TravelStatus {
        PENDING,     // Reise wird gerade bearbeitet
        CONFIRMED,   // Alle Buchungen wurden bestätigt
        FAILED       // Mindestens eine Buchung konnte nicht durchgeführt werden
    }

    public Travel(String customerId) {
        this.id = UUID.randomUUID().toString();
        this.customerId = customerId;
        this.bookings = new ArrayList<>();
        this.status = TravelStatus.PENDING;
    }

    public String getId() {
        return id;
    }

    public String getCustomerId() {
        return customerId;
    }

    public List<Booking> getBookings() {
        return bookings;
    }

    public void addBooking(Booking booking) {
        this.bookings.add(booking);
    }

    public TravelStatus getStatus() {
        return status;
    }

    public void setStatus(TravelStatus status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "Travel{" +
                "id='" + id + '\'' +
                ", customerId='" + customerId + '\'' +
                ", bookings=" + bookings +
                ", status=" + status +
                '}';
    }
}