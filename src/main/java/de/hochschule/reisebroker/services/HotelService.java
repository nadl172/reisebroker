package de.hochschule.reisebroker.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.hochschule.reisebroker.config.Configuration;
import de.hochschule.reisebroker.messaging.Message;
import de.hochschule.reisebroker.messaging.ZeroMQHelper;
import de.hochschule.reisebroker.model.Booking;
import de.hochschule.reisebroker.model.Hotel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class HotelService {
    private static final Logger logger = LoggerFactory.getLogger(HotelService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final String hotelId;
    private final String receiverEndpoint;
    private final String brokerEndpoint;
    private final Hotel hotel;
    private final ZeroMQHelper zmqHelper;
    private final Configuration config;

    // Speichert alle aktiven Buchungen
    private final Map<String, Booking> bookings = new ConcurrentHashMap<>();

    public HotelService(String hotelId, String name, int totalRooms,
                        String receiverEndpoint, String brokerEndpoint) {
        this.hotelId = hotelId;
        this.hotel = new Hotel(name, totalRooms);
        this.receiverEndpoint = receiverEndpoint;
        this.brokerEndpoint = brokerEndpoint;
        this.zmqHelper = new ZeroMQHelper();
        this.config = Configuration.getInstance();

        // Starte den Message-Receiver
        startReceiver();
    }

    private void startReceiver() {
        zmqHelper.startReceiver(receiverEndpoint, this::handleMessage);
    }

    private void handleMessage(Message message) {
        // Simuliere Verarbeitungszeit
        config.simulateProcessingDelay();

        // Simuliere Systemabsturz (keine Antwort)
        if (config.shouldSimulateCrash()) {
            logger.warn("Simuliere Systemabsturz für Nachricht: {}", message.getId());
            return; // Keine weitere Verarbeitung
        }

        try {
            switch (message.getType()) {
                case BOOK_REQUEST:
                    handleBookRequest(message);
                    break;

                case CANCEL_REQUEST:
                    handleCancelRequest(message);
                    break;

                default:
                    logger.warn("Unbekannter Nachrichtentyp: {}", message.getType());
            }
        } catch (Exception e) {
            logger.error("Fehler bei der Verarbeitung der Nachricht", e);
        }
    }

    private void handleBookRequest(Message message) throws IOException {
        String bookingId = message.getPayload();
        logger.info("Verarbeite Buchungsanfrage: {}", bookingId);

        // Erstelle eine neue Buchung aus den Daten
        Booking booking = new Booking(hotel.getId(), 1, "customer123");
        booking.setStatus(Booking.BookingStatus.REQUESTED);

        boolean success = false;
        Message.MessageType responseType;

        // Simuliere fachlichen Fehler (z.B. keine freien Zimmer)
        if (!config.shouldBookingSucceed()) {
            logger.info("Simuliere ausgebuchtes Hotel für Buchung: {}", bookingId);
            booking.setStatus(Booking.BookingStatus.FAILED);
            responseType = Message.MessageType.BOOK_RESPONSE;
        } else {
            // Versuche, das Zimmer zu buchen
            success = hotel.bookRoom(booking.getTimeBlock());

            if (success) {
                booking.setStatus(Booking.BookingStatus.CONFIRMED);
                bookings.put(bookingId, booking);
                responseType = Message.MessageType.BOOK_RESPONSE;
            } else {
                booking.setStatus(Booking.BookingStatus.FAILED);
                responseType = Message.MessageType.BOOK_RESPONSE;
            }
        }

        // Simuliere stillen Fehler (Verarbeitung erfolgt, aber keine Antwort)
        if (config.shouldSimulateSilentFailure()) {
            logger.warn("Simuliere stillen Fehler für Buchung: {}", bookingId);
            return;
        }

        // Sende Antwort an den Broker
        Message response = new Message(
                responseType,
                hotel.getId(),
                "ReiseBroker",
                bookingId
        );

        zmqHelper.sendMessage(brokerEndpoint, response);
        logger.info("Buchungsantwort gesendet: {} Status: {}", bookingId,
                success ? "BESTÄTIGT" : "ABGELEHNT");
    }

    private void handleCancelRequest(Message message) {
        String bookingId = message.getPayload();
        logger.info("Verarbeite Stornierungsanfrage: {}", bookingId);

        Booking booking = bookings.get(bookingId);
        if (booking != null) {
            // Storniere die Buchung
            hotel.cancelBooking(booking.getTimeBlock());
            booking.setStatus(Booking.BookingStatus.COMPENSATED);

            // Simuliere stillen Fehler (keine Antwort, aber Verarbeitung)
            if (config.shouldSimulateSilentFailure()) {
                logger.warn("Simuliere stillen Fehler für Stornierung: {}", bookingId);
                return;
            }

            // Sende Bestätigung
            Message response = new Message(
                    Message.MessageType.CANCEL_RESPONSE,
                    hotel.getId(),
                    "ReiseBroker",
                    bookingId
            );

            zmqHelper.sendMessage(brokerEndpoint, response);
            logger.info("Stornierungsbestätigung gesendet: {}", bookingId);
        } else {
            logger.warn("Buchung nicht gefunden: {}", bookingId);
        }
    }

    public void shutdown() {
        zmqHelper.close();
    }
}
