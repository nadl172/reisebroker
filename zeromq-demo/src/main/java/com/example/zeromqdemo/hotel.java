package com.example.zeromqdemo;

import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Socket;

import java.util.Map;
import java.util.HashMap;

public class hotel {
    private final String name;
    private final String identity;
    private final int preis;  // Preis pro Übernachtung

    public hotel(String name, String identity, int preis) {
        this.name = name;
        this.identity = identity;
        this.preis = preis;
    }

    public void start() {
        new Thread(() -> {
            try (ZContext context = new ZContext()) {
                Socket socket = context.createSocket(ZMQ.DEALER);
                socket.setIdentity(identity.getBytes(ZMQ.CHARSET));
                socket.connect("tcp://localhost:5555");
                System.out.println(name + " ist bereit und wartet auf Anfragen...");

                while (!Thread.currentThread().isInterrupted()) {
                    String message = socket.recvStr();
                    if (message != null) {
                        System.out.println(name + " empfangen: " + message);
                        
                        // Nachricht parsen
                        Map<String, String> data = parseMessage(message);
                        String type = data.getOrDefault("TYPE", "Unbekannt");
                        String anfrageName = data.getOrDefault("NAME", "Unbekannt");
                        String clientId = data.getOrDefault("CLIENTID", "");
                        String requestId = data.getOrDefault("ID", "0");
                        
                        // Auf Anfrage antworten
                        if ("Preisanfrage".equals(type)) {
                            System.out.println(name + " bearbeitet Preisanfrage von " + anfrageName);
                            // Simuliere etwas Bearbeitungszeit
                            try { Thread.sleep(500); } catch (InterruptedException ignored) {}
                            
                            // Anfrage beantworten mit Preis
                            String antwort = "TYPE=Antwort;NAME=" + name + ";PREIS=" + preis + ";CLIENTID=" + clientId + ";ID=" + requestId;
                            socket.sendMore("");
                            socket.send(antwort);
                            System.out.println(name + " sendet: " + antwort);
                        }
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