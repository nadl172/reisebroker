package de.hochschule.reisebroker.model;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Hotel {
    private String id;
    private String name;
    private int totalRooms;
    private Map<Integer, Integer> availableRoomsByTimeBlock; // Timeblock -> Anzahl verfügbarer Zimmer

    public Hotel(String name, int totalRooms) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.totalRooms = totalRooms;
        
        // Initialisiere 100 Zeitblöcke (Wochen über 2 Jahre)
        this.availableRoomsByTimeBlock = new HashMap<>();
        for (int i = 1; i <= 100; i++) {
            availableRoomsByTimeBlock.put(i, totalRooms);
        }
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getTotalRooms() {
        return totalRooms;
    }

    public boolean isRoomAvailable(int timeBlock) {
        return availableRoomsByTimeBlock.getOrDefault(timeBlock, 0) > 0;
    }

    public synchronized boolean bookRoom(int timeBlock) {
        int available = availableRoomsByTimeBlock.getOrDefault(timeBlock, 0);
        if (available > 0) {
            availableRoomsByTimeBlock.put(timeBlock, available - 1);
            return true;
        }
        return false;
    }

    public synchronized void cancelBooking(int timeBlock) {
        int available = availableRoomsByTimeBlock.getOrDefault(timeBlock, 0);
        availableRoomsByTimeBlock.put(timeBlock, available + 1);
    }

    @Override
    public String toString() {
        return "Hotel{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", totalRooms=" + totalRooms +
                '}';
    }
}