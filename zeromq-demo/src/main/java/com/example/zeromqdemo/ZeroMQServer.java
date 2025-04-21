package com.example.zeromqdemo;

import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Socket;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ZeroMQServer {
    // Speichert Client-ID -> [AnzahlErwarteterAntworten, GesammeltePreise]
    private static Map<String, Object[]> pendingRequests = new ConcurrentHashMap<>();
    
    public static void main(String[] args) {
        try (ZContext context = new ZContext()) {
            Socket socket = context.createSocket(ZMQ.ROUTER);
            socket.bind("tcp://*:5555");
            System.out.println("ZeroMQ Server läuft auf Port 5555...");

            while (!Thread.currentThread().isInterrupted()) {
                // Zuerst die Identität des Senders empfangen
                String identity = socket.recvStr();
                // Leerer Rahmen als Delimiter bei ROUTER-Socket
                socket.recvStr(); 
                // Dann die eigentliche Nachricht
                String message = socket.recvStr();
                
                System.out.println("Empfangen von " + identity + ": " + message);
                
                // Nachricht in Key=Value-Paare aufschlüsseln
                Map<String, String> data = parseMessage(message);
                String type = data.getOrDefault("TYPE", "Unbekannt");
                
                if ("Preisanfrage".equals(type)) {
                    handlePriceRequest(socket, identity, data);
                } else if ("Antwort".equals(type)) {
                    handleHotelResponse(socket, identity, data);
                }
            }
        }
    }
    
    private static void handlePriceRequest(Socket socket, String clientId, Map<String, String> data) {
        String name = data.getOrDefault("NAME", "Unbekannt");
        String hotel1 = data.getOrDefault("Hotel1", "Nein");
        String hotel2 = data.getOrDefault("Hotel2", "Nein");
        String hotel3 = data.getOrDefault("Hotel3", "Nein");
        String requestId = data.getOrDefault("ID", "0");
        
        System.out.println("Preisanfrage von " + name + " (ID: " + clientId + ")");
        
        // Zähle, wie viele Hotels angefragt werden
        int hotelCount = 0;
        if ("Ja".equals(hotel1)) hotelCount++;
        if ("Ja".equals(hotel2)) hotelCount++;
        if ("Ja".equals(hotel3)) hotelCount++;
        
        // Speichere die Anfrage mit erwarteter Anzahl von Antworten und Anfrage-ID
        pendingRequests.put(clientId, new Object[]{hotelCount, 0, requestId});
        
        // Sende Anfragen an die gewünschten Hotels
        if ("Ja".equals(hotel1)) {
            sendToHotel(socket, "Meer", "ID=" + requestId + ";TYPE=Preisanfrage;NAME=" + name + ";CLIENTID=" + clientId);
        }
        if ("Ja".equals(hotel2)) {
            sendToHotel(socket, "City", "ID=" + requestId + ";TYPE=Preisanfrage;NAME=" + name + ";CLIENTID=" + clientId);
        }
        if ("Ja".equals(hotel3)) {
            sendToHotel(socket, "See", "ID=" + requestId + ";TYPE=Preisanfrage;NAME=" + name + ";CLIENTID=" + clientId);
        }
    }
    
    private static void handleHotelResponse(Socket socket, String hotelId, Map<String, String> data) {
        String hotelName = data.getOrDefault("NAME", "Unbekannt");
        int preis = Integer.parseInt(data.getOrDefault("PREIS", "0"));
        String clientId = data.getOrDefault("CLIENTID", "");
        
        System.out.println("Antwort von Hotel " + hotelName + " für Client " + clientId + ": " + preis + "€");
        
        // Prüfe, ob wir eine ausstehende Anfrage für diesen Client haben
        if (pendingRequests.containsKey(clientId)) {
            Object[] requestData = pendingRequests.get(clientId);
            int expectedResponses = (int) requestData[0];
            int currentTotal = (int) requestData[1];
            String requestId = (String) requestData[2];
            
            // Aktualisiere den Gesamtpreis
            currentTotal += preis;
            requestData[1] = currentTotal;
            
            // Reduziere die erwartete Anzahl von Antworten
            expectedResponses--;
            requestData[0] = expectedResponses;
            
            // Wenn alle Antworten eingetroffen sind, sende Endergebnis an Client
            if (expectedResponses == 0) {
                sendToClient(socket, clientId, "ID=" + requestId + ";TYPE=Preisantwort;GESAMTPREIS=" + currentTotal);
                pendingRequests.remove(clientId);
            }
        }
    }
    
    private static void sendToHotel(Socket socket, String hotelId, String message) {
        System.out.println("Sende an Hotel " + hotelId + ": " + message);
        socket.sendMore(hotelId);
        socket.sendMore("");
        socket.send(message);
    }
    
    private static void sendToClient(Socket socket, String clientId, String message) {
        System.out.println("Sende an Client " + clientId + ": " + message);
        socket.sendMore(clientId);
        socket.send(message);
    }

    private static Map<String, String> parseMessage(String message) {
        Map<String, String> data = new HashMap<>();
        for (String part : message.split(";")) {
            String[] kv = part.split("=", 2);
            if (kv.length == 2) {
                data.put(kv[0].trim(), kv[1].trim());
            }
        }
        return data;
    }
}