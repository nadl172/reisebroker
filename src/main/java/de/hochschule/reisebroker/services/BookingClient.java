package de.hochschule.reisebroker.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import de.hochschule.reisebroker.config.Configuration;
import de.hochschule.reisebroker.messaging.Message;
import de.hochschule.reisebroker.messaging.ZeroMQHelper;
import de.hochschule.reisebroker.model.Booking;
import de.hochschule.reisebroker.model.Travel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class BookingClient {
    private static final Logger logger = LoggerFactory.getLogger(BookingClient.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    static {
        objectMapper.registerModule(new JavaTimeModule()); // <- HIER ist wichtig!
    }

    private final String clientId;
    private final String receiverEndpoint;
    private final String brokerEndpoint;
    private final ZeroMQHelper zmqHelper;
    private final Configuration config;
    private final Random random = new Random();
    private final List<String> hotelIds;

    public BookingClient(String clientId, String receiverEndpoint, String brokerEndpoint, List<String> hotelIds) {
        this.clientId = clientId;
        this.receiverEndpoint = receiverEndpoint;
        this.brokerEndpoint = brokerEndpoint;
        this.hotelIds = hotelIds;
        this.zmqHelper = new ZeroMQHelper();
        this.config = Configuration.getInstance();

        startReceiver();
    }

    private void startReceiver() {
        zmqHelper.startReceiver(receiverEndpoint, this::handleMessage);
    }

    private void handleMessage(Message message) {
        if (message.getType() == Message.MessageType.TRAVEL_SUMMARY) {
            try {
                Map<String, Object> summary = objectMapper.readValue(message.getPayload(), Map.class);

                String timestamp = LocalDateTime.now().format(formatter);
                String travelId = (String) summary.get("travelId");
                String status = summary.get("status").toString();

                StringBuilder log = new StringBuilder();
                log.append("\n=== REISEBUCHUNG ERGEBNIS ===\n");
                log.append("Zeitstempel: ").append(timestamp).append("\n");
                log.append("Reise-ID: ").append(travelId).append("\n");
                log.append("Status: ").append(status).append("\n");

                if (summary.containsKey("error")) {
                    log.append("Fehler: ").append(summary.get("error")).append("\n");
                }

                log.append("Buchungen:\n");
                List<Map<String, Object>> bookings = (List<Map<String, Object>>) summary.get("bookings");
                for (Map<String, Object> booking : bookings) {
                    log.append("  - Hotel: ").append(booking.get("hotelId"))
                            .append(", Status: ").append(booking.get("status"))
                            .append(", Zeitblock: ").append(booking.get("timeBlock"))
                            .append("\n");
                }
                log.append("===============================");

                logger.info(log.toString());

            } catch (IOException e) {
                logger.error("Fehler beim Deserialisieren der Reisezusammenfassung", e);
            }
        }
    }

    public Travel generateTravelRequest() {
        String customerId = "customer-" + UUID.randomUUID().toString().substring(0, 8);
        Travel travel = new Travel(customerId);

        int bookingCount = 1 + random.nextInt(5);

        List<String> usedHotels = new ArrayList<>();
        String lastHotelId = null;

        for (int i = 0; i < bookingCount; i++) {
            String hotelId;
            do {
                hotelId = hotelIds.get(random.nextInt(hotelIds.size()));
            } while (hotelId.equals(lastHotelId));

            lastHotelId = hotelId;
            usedHotels.add(hotelId);

            int timeBlock = 1 + random.nextInt(100);

            Booking booking = new Booking(hotelId, timeBlock, customerId);
            travel.addBooking(booking);
        }

        return travel;
    }

    public void sendTravelRequest(Travel travel) {
        try {
            String payload = objectMapper.writeValueAsString(travel);

            Message message = new Message(
                    Message.MessageType.BOOK_REQUEST,
                    clientId,
                    "ReiseBroker",
                    payload
            );

            zmqHelper.sendMessage(brokerEndpoint, message);
            logger.info("Reiseanfrage gesendet: {}", travel.getId());
        } catch (IOException e) {
            logger.error("Fehler beim Serialisieren der Reiseanfrage", e);
        }
    }

    public void startSimulation() {
        Thread simulationThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Travel travel = generateTravelRequest();
                    sendTravelRequest(travel);

                    Thread.sleep(config.getArrivalDelayMs());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });

        simulationThread.setDaemon(true);
        simulationThread.start();
        logger.info("Simulation gestartet");
    }

    public void shutdown() {
        zmqHelper.close();
    }
}
