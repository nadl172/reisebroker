package de.hochschule.reisebroker.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.hochschule.reisebroker.messaging.Message;
import de.hochschule.reisebroker.messaging.ZeroMQHelper;
import de.hochschule.reisebroker.model.Booking;
import de.hochschule.reisebroker.model.Travel;
import de.hochschule.reisebroker.saga.SagaCoordinator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TravelBroker {
    private static final Logger logger = LoggerFactory.getLogger(TravelBroker.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    private final String receiverEndpoint;
    private final String clientEndpoint;
    private final ZeroMQHelper zmqHelper;
    private final SagaCoordinator sagaCoordinator;
    private final ExecutorService executorService;
    private final Map<String, String> hotelEndpoints;

    public TravelBroker(String receiverEndpoint, String clientEndpoint, Map<String, String> hotelEndpoints) {
        this.receiverEndpoint = receiverEndpoint;
        this.clientEndpoint = clientEndpoint;
        this.hotelEndpoints = hotelEndpoints;
        this.zmqHelper = new ZeroMQHelper();
        this.executorService = Executors.newCachedThreadPool();
        
        // SAGA-Koordinator initialisieren
        this.sagaCoordinator = new SagaCoordinator(zmqHelper, hotelEndpoints);
        
        // Starte Nachrichtenempfänger
        startReceiver();
    }

    private void startReceiver() {
        // Empfänger für Kundenanfragen
        zmqHelper.startReceiver(receiverEndpoint, this::handleMessage);
        
        // Empfänger für Antworten von Hotels
        for (String endpoint : hotelEndpoints.values()) {
            String responseEndpoint = endpoint.replace("request", "response");
            zmqHelper.startReceiver(responseEndpoint, this::handleHotelResponse);
        }
    }

    private void handleMessage(Message message) {
        if (message.getType() == Message.MessageType.BOOK_REQUEST) {
            // Verarbeite Buchungsanfrage vom Kunden
            executorService.submit(() -> {
                try {
                    Travel travel = objectMapper.readValue(message.getPayload(), Travel.class);
                    processTravelBooking(travel);
                } catch (IOException e) {
                    logger.error("Fehler beim Deserialisieren der Reisedaten", e);
                }
            });
        }
    }

    private void handleHotelResponse(Message message) {
        // Leite die Antwort an den SAGA-Koordinator weiter
        sagaCoordinator.handleHotelResponse(message);
    }

    private void processTravelBooking(Travel travel) {
        logger.info("Verarbeite Reisebuchung: {}", travel.getId());
        
        // Überprüfe die Gültigkeit der Reiseanfrage
        if (travel.getBookings().isEmpty() || travel.getBookings().size() > 5) {
            logger.error("Ungültige Anzahl an Buchungen: {}", travel.getBookings().size());
            sendTravelResponse(travel, "Ungültige Anzahl an Buchungen");
            return;
        }
        
        // Prüfe, ob zwei aufeinanderfolgende Buchungen im gleichen Hotel sind
        String lastHotelId = null;
        for (Booking booking : travel.getBookings()) {
            if (booking.getHotelId().equals(lastHotelId)) {
                logger.error("Zwei aufeinanderfolgende Buchungen im gleichen Hotel");
                sendTravelResponse(travel, "Zwei aufeinanderfolgende Buchungen im gleichen Hotel");
                return;
            }
            lastHotelId = booking.getHotelId();
        }
        
        // Starte die SAGA für die Reisebuchung
        sagaCoordinator.startSaga(travel);
        
        // Sende Ergebnis an den Kunden
        sendTravelResponse(travel, null);
    }

    private void sendTravelResponse(Travel travel, String errorMessage) {
        try {
            // Erstelle eine Zusammenfassung
            Map<String, Object> summary = new HashMap<>();
            summary.put("travelId", travel.getId());
            summary.put("status", travel.getStatus());
            summary.put("bookings", travel.getBookings());
            
            if (errorMessage != null) {
                summary.put("error", errorMessage);
            }
            
            String payload = objectMapper.writeValueAsString(summary);
            
            // Sende Nachricht an den Kunden
            Message response = new Message(
                    Message.MessageType.TRAVEL_SUMMARY,
                    "ReiseBroker",
                    travel.getCustomerId(),
                    payload
            );
            
            zmqHelper.sendMessage(clientEndpoint, response);
            logger.info("Reisezusammenfassung gesendet für: {}", travel.getId());
        } catch (IOException e) {
            logger.error("Fehler beim Serialisieren der Reisezusammenfassung", e);
        }
    }

    public void shutdown() {
        executorService.shutdownNow();
        zmqHelper.close();
    }
}