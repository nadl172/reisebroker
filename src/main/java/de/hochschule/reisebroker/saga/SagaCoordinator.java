package de.hochschule.reisebroker.saga;

import de.hochschule.reisebroker.messaging.Message;
import de.hochschule.reisebroker.messaging.ZeroMQHelper;
import de.hochschule.reisebroker.model.Booking;
import de.hochschule.reisebroker.model.Travel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class SagaCoordinator {
    private static final Logger logger = LoggerFactory.getLogger(SagaCoordinator.class);
    
    private final ZeroMQHelper zmqHelper;
    private final Map<String, String> hotelEndpoints;
    private final Map<String, SagaState> activeSagas = new ConcurrentHashMap<>();
    private final int TIMEOUT_SECONDS = 10;

    public SagaCoordinator(ZeroMQHelper zmqHelper, Map<String, String> hotelEndpoints) {
        this.zmqHelper = zmqHelper;
        this.hotelEndpoints = hotelEndpoints;
    }

    // Startet eine neue SAGA für eine Reisebuchung
    public void startSaga(Travel travel) {
        String sagaId = travel.getId();
        logger.info("Starte neue SAGA für Reise: {}", sagaId);
        
        // Erstelle den SAGA-Status
        SagaState sagaState = new SagaState(travel);
        activeSagas.put(sagaId, sagaState);
        
        // Versuche, jede Buchung durchzuführen
        for (Booking booking : travel.getBookings()) {
            String hotelId = booking.getHotelId();
            String endpoint = hotelEndpoints.get(hotelId);
            
            if (endpoint == null) {
                logger.error("Endpunkt für Hotel {} nicht gefunden", hotelId);
                handleFailureAndCompensate(sagaId, "Hotel-Endpunkt nicht gefunden");
                return;
            }
            
            // Sende Buchungsanfrage
            Message bookRequest = new Message(
                Message.MessageType.BOOK_REQUEST,
                "ReiseBroker",
                hotelId,
                booking.getId() // Als Payload senden wir die Buchungs-ID
            );
            
            zmqHelper.sendMessage(endpoint, bookRequest);
            
            // Warte auf Antwort mit Timeout
            CountDownLatch latch = new CountDownLatch(1);
            sagaState.pendingBookings.put(booking.getId(), latch);
            
            try {
                boolean received = latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                if (!received) {
                    logger.error("Timeout beim Warten auf Antwort für Buchung: {}", booking.getId());
                    handleFailureAndCompensate(sagaId, "Timeout bei Buchungsanfrage");
                    return;
                }
                
                // Prüfe, ob die Buchung erfolgreich war
                if (booking.getStatus() != Booking.BookingStatus.CONFIRMED) {
                    logger.error("Buchung fehlgeschlagen für: {}", booking.getId());
                    handleFailureAndCompensate(sagaId, "Buchung fehlgeschlagen");
                    return;
                }
                
                // Speichere die erfolgreiche Buchung
                sagaState.successfulBookings.add(booking);
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("Thread wurde unterbrochen", e);
                handleFailureAndCompensate(sagaId, "Thread-Unterbrechung");
                return;
            }
        }
        
        // Wenn wir hierher kommen, waren alle Buchungen erfolgreich
        travel.setStatus(Travel.TravelStatus.CONFIRMED);
        logger.info("SAGA erfolgreich abgeschlossen für Reise: {}", sagaId);
        
        // Entferne die SAGA aus den aktiven
        activeSagas.remove(sagaId);
    }

    // Behandelt eine Antwort von einem Hoteldienst
    public void handleHotelResponse(Message message) {
        // Extrahiere die Buchungs-ID aus der Nachricht
        String bookingId = message.getPayload();
        
        // Suche die entsprechende SAGA
        for (SagaState state : activeSagas.values()) {
            CountDownLatch latch = state.pendingBookings.get(bookingId);
            
            if (latch != null) {
                // Markiere die Buchung als bestätigt oder fehlgeschlagen
                for (Booking booking : state.travel.getBookings()) {
                    if (booking.getId().equals(bookingId)) {
                        if (message.getType() == Message.MessageType.BOOK_RESPONSE) {
                            booking.setStatus(Booking.BookingStatus.CONFIRMED);
                        } else {
                            booking.setStatus(Booking.BookingStatus.FAILED);
                        }
                        break;
                    }
                }
                
                // Countdown, um den Wartezustand zu beenden
                latch.countDown();
                return;
            }
        }
        
        logger.warn("Unbekannte Antwort erhalten: {}", message);
    }

    // Initiiert Kompensationstransaktionen für alle erfolgreichen Buchungen
    private void handleFailureAndCompensate(String sagaId, String reason) {
        logger.info("SAGA fehlgeschlagen für {}: {}", sagaId, reason);
        
        SagaState state = activeSagas.get(sagaId);
        if (state == null) {
            return;
        }
        
        // Markiere die Reise als fehlgeschlagen
        state.travel.setStatus(Travel.TravelStatus.FAILED);
        
        // Sende Stornierungsanfragen für alle erfolgreichen Buchungen
        for (Booking booking : state.successfulBookings) {
            String hotelId = booking.getHotelId();
            String endpoint = hotelEndpoints.get(hotelId);
            
            if (endpoint != null) {
                Message cancelRequest = new Message(
                    Message.MessageType.CANCEL_REQUEST,
                    "ReiseBroker",
                    hotelId,
                    booking.getId()
                );
                
                zmqHelper.sendMessage(endpoint, cancelRequest);
                
                // Markiere die Buchung als storniert
                booking.setStatus(Booking.BookingStatus.COMPENSATED);
            }
        }
        
        // Entferne die SAGA aus den aktiven
        activeSagas.remove(sagaId);
    }

    // Innere Klasse zur Verwaltung des SAGA-Zustands
    private static class SagaState {
        private final Travel travel;
        private final List<Booking> successfulBookings = new ArrayList<>();
        private final Map<String, CountDownLatch> pendingBookings = new HashMap<>();
        
        public SagaState(Travel travel) {
            this.travel = travel;
        }
    }
}