package com.example.zeromqdemo;

import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Socket;

import java.util.Map;
import java.util.HashMap;

public class ClientInstance {
    private final String name;
    private final String identity;
    private final String message;

    public ClientInstance(String name, String identity, String message) {
        this.name = name;
        this.identity = identity;
        this.message = message;
    }

    public void start() {
        new Thread(() -> {
            try (ZContext context = new ZContext()) {
                Socket socket = context.createSocket(ZMQ.DEALER);
                socket.setIdentity(identity.getBytes(ZMQ.CHARSET));
                socket.connect("tcp://localhost:5555");
                
                System.out.println(name + " sendet: " + message);
                socket.sendMore("");
                socket.send(message);
                
                // Auf Antwort warten
                String reply = socket.recvStr();
                System.out.println(name + " erhält Antwort: " + reply);
                
                // Antwort auswerten
                Map<String, String> data = parseMessage(reply);
                String type = data.getOrDefault("TYPE", "Unbekannt");
                
                if ("Preisantwort".equals(type)) {
                    int gesamtpreis = Integer.parseInt(data.getOrDefault("GESAMTPREIS", "0"));
                    System.out.println(name + ": Der Gesamtpreis für meine Anfrage beträgt " + gesamtpreis + "€");
                    
                    // Hier könnte eine Buchungsanfrage folgen, wenn der Preis akzeptabel ist
                    if (gesamtpreis < 300) {
                        // Beispiel für eine Buchungsanfrage
                        System.out.println(name + ": Der Preis ist akzeptabel, ich buche!");
                        // Buchungslogik implementieren
                    } else {
                        System.out.println(name + ": Der Preis ist zu hoch, ich buche nicht.");
                    }
                }
            }
        }).start();
    }
    
    private Map<String, String> parseMessage(String message) {
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