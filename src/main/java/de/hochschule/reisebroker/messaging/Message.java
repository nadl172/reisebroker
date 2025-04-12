package de.hochschule.reisebroker.messaging;

import java.time.LocalDateTime;
import java.util.UUID;

public class Message {
    private String id;
    private MessageType type;
    private String senderID;
    private String receiverID;
    private LocalDateTime timestamp;
    private String payload;  // JSON-Daten für den Inhalt

    public enum MessageType {
        BOOK_REQUEST,        // Anfrage zum Buchen eines Zimmers
        BOOK_RESPONSE,       // Antwort zur Buchungsanfrage
        CANCEL_REQUEST,      // Anfrage zum Stornieren einer Buchung
        CANCEL_RESPONSE,     // Antwort zur Stornierungsanfrage
        TRAVEL_SUMMARY       // Zusammenfassung der gesamten Reisebuchung
    }

    public Message(MessageType type, String senderID, String receiverID, String payload) {
        this.id = UUID.randomUUID().toString();
        this.type = type;
        this.senderID = senderID;
        this.receiverID = receiverID;
        this.timestamp = LocalDateTime.now();
        this.payload = payload;
    }

    public String getId() {
        return id;
    }

    public MessageType getType() {
        return type;
    }

    public String getSenderID() {
        return senderID;
    }

    public String getReceiverID() {
        return receiverID;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getPayload() {
        return payload;
    }

    @Override
    public String toString() {
        return "Message{" +
                "id='" + id + '\'' +
                ", type=" + type +
                ", senderID='" + senderID + '\'' +
                ", receiverID='" + receiverID + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}