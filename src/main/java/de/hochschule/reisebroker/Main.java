package de.hochschule.reisebroker;

import de.hochschule.reisebroker.config.Configuration;
import de.hochschule.reisebroker.services.BookingClient;
import de.hochschule.reisebroker.services.HotelService;
import de.hochschule.reisebroker.services.TravelBroker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        logger.info("Starte Reisebroker-Anwendung");
        
        // Konfiguration laden
        Configuration config = Configuration.getInstance();
        
        // Endpoints definieren
        String brokerEndpoint = "tcp://localhost:5555";
        String clientEndpoint = "tcp://localhost:5556";
        
        // Hotel-Endpoints definieren
        Map<String, String> hotelEndpoints = new HashMap<>();
        hotelEndpoints.put("hotel1", "tcp://localhost:5557");
        hotelEndpoints.put("hotel2", "tcp://localhost:5558");
        hotelEndpoints.put("hotel3", "tcp://localhost:5559");
        
        // Dienste erstellen
        List<HotelService> hotelServices = Arrays.asList(
            new HotelService("hotel1", "UrlaubInDerHöhe", 10, "tcp://localhost:5557", "tcp://localhost:5560"),
            new HotelService("hotel2", "FluchtAnsMeer", 15, "tcp://localhost:5558", "tcp://localhost:5561"),
            new HotelService("hotel3", "FernDerHeimat", 8, "tcp://localhost:5559", "tcp://localhost:5562")
        );
        
        TravelBroker travelBroker = new TravelBroker(brokerEndpoint, clientEndpoint, hotelEndpoints);
        
        BookingClient bookingClient = new BookingClient(
            "client1", 
            clientEndpoint, 
            brokerEndpoint, 
            Arrays.asList("hotel1", "hotel2", "hotel3")
        );
        
        // Starte Simulation
        bookingClient.startSimulation();
        
        logger.info("Alle Dienste gestartet. Drücke ENTER zum Beenden.");
        
        // Warte auf Benutzereingabe zum Beenden
        Scanner scanner = new Scanner(System.in);
        scanner.nextLine();
        
        // Alle Dienste herunterfahren
        bookingClient.shutdown();
        travelBroker.shutdown();
        hotelServices.forEach(HotelService::shutdown);
        
        logger.info("Anwendung beendet");
    }
}