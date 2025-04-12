package de.hochschule.reisebroker.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ZeroMQHelper {
    private static final Logger logger = LoggerFactory.getLogger(ZeroMQHelper.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    static {
        // Registriere Modul für Java 8 Date/Time API
        objectMapper.registerModule(new JavaTimeModule());
    }

    private ZContext context;
    private ExecutorService executor;

    public ZeroMQHelper() {
        context = new ZContext();
        executor = Executors.newCachedThreadPool();
    }

    // Senden einer Nachricht an einen anderen Dienst
    public void sendMessage(String endpoint, Message message) {
        try {
            String jsonMessage = objectMapper.writeValueAsString(message);

            ZMQ.Socket socket = context.createSocket(SocketType.PUSH);
            socket.connect(endpoint);

            boolean sent = socket.send(jsonMessage);
            if (!sent) {
                logger.error("Fehler beim Senden der Nachricht an {}", endpoint);
            } else {
                logger.info("Nachricht gesendet an {}: {}", endpoint, message);
            }

            socket.close();
        } catch (IOException e) {
            logger.error("Fehler beim Serialisieren der Nachricht", e);
        }
    }

    // Starten eines Listeners für eingehende Nachrichten
    public void startReceiver(String endpoint, MessageHandler handler) {
        executor.submit(() -> {
            ZMQ.Socket socket = context.createSocket(SocketType.PULL);
            socket.bind(endpoint);

            logger.info("Empfänger gestartet auf {}", endpoint);

            while (!Thread.currentThread().isInterrupted()) {
                byte[] messageData = socket.recv();
                if (messageData != null) {
                    try {
                        String jsonMessage = new String(messageData);
                        Message message = objectMapper.readValue(jsonMessage, Message.class);

                        logger.info("Nachricht empfangen auf {}: {}", endpoint, message);

                        // Weiterleitung an den Handler
                        handler.handleMessage(message);
                    } catch (IOException e) {
                        logger.error("Fehler beim Deserialisieren der Nachricht", e);
                    }
                }
            }

            socket.close();
        });
    }

    // Interface für Message-Handler
    public interface MessageHandler {
        void handleMessage(Message message);
    }

    // Ressourcen freigeben
    public void close() {
        executor.shutdownNow();
        context.close();
    }
}
