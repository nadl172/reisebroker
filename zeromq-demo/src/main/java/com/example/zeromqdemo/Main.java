package com.example.zeromqdemo;

public class Main {
    public static void main(String[] args) {
        // Broker (Server) in einem eigenen Thread starten
        new Thread(() -> {
            ZeroMQServer.main(null);
        }).start();
        
        // Kurz warten, damit der Broker hochfährt
        try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
        
        // Hotels in Threads starten (mit verschiedenen Preisen)
        new hotel("Hotel zum Meerblick", "Meer", 120).start();
        new hotel("CityHotel69", "City", 89).start();
        new hotel("Seehotel Hornung", "See", 150).start();
        
        // Kurz warten, damit Hotels hochfahren
        try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
        
        // Dann Clients starten
        new ClientInstance("Felix", "Felix", "ID=1;TYPE=Preisanfrage;NAME=Felix;Hotel1=Ja;Hotel2=Ja;Hotel3=Ja").start();
        new ClientInstance("Ally", "Ally", "ID=2;TYPE=Preisanfrage;NAME=Ally;Hotel1=Nein;Hotel2=Ja;Hotel3=Ja").start();
        new ClientInstance("Nadia", "Nadia", "ID=3;TYPE=Preisanfrage;NAME=Nadia;Hotel1=Nein;Hotel2=Nein;Hotel3=Ja").start();
    }
}