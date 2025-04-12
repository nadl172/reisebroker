package de.hochschule.reisebroker.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.Random;

public class Configuration {
    private static Configuration instance;
    private Properties properties;
    private Random random = new Random();

    // Standard-Konfigurationswerte
    private static final int DEFAULT_ARRIVAL_DELAY_MS = 1000;
    private static final int DEFAULT_PROCESSING_TIME_MS = 500;
    private static final double DEFAULT_CRASH_PROBABILITY = 0.05;
    private static final double DEFAULT_SILENT_FAILURE_PROBABILITY = 0.1;
    private static final double DEFAULT_BOOKING_SUCCESS_PROBABILITY = 0.8;

    private Configuration() {
        properties = new Properties();
        try {
            FileInputStream input = new FileInputStream("config.properties");
            properties.load(input);
            input.close();
        } catch (IOException e) {
            System.out.println("Keine Konfigurationsdatei gefunden, verwende Standardwerte");
        }
    }

    public static synchronized Configuration getInstance() {
        if (instance == null) {
            instance = new Configuration();
        }
        return instance;
    }

    public int getArrivalDelayMs() {
        return Integer.parseInt(properties.getProperty("arrival.delay.ms", 
                                String.valueOf(DEFAULT_ARRIVAL_DELAY_MS)));
    }

    public int getProcessingTimeMs() {
        return Integer.parseInt(properties.getProperty("processing.time.ms", 
                                String.valueOf(DEFAULT_PROCESSING_TIME_MS)));
    }

    public double getCrashProbability() {
        return Double.parseDouble(properties.getProperty("crash.probability", 
                                  String.valueOf(DEFAULT_CRASH_PROBABILITY)));
    }

    public double getSilentFailureProbability() {
        return Double.parseDouble(properties.getProperty("silent.failure.probability", 
                                  String.valueOf(DEFAULT_SILENT_FAILURE_PROBABILITY)));
    }

    public double getBookingSuccessProbability() {
        return Double.parseDouble(properties.getProperty("booking.success.probability", 
                                  String.valueOf(DEFAULT_BOOKING_SUCCESS_PROBABILITY)));
    }

    // Simuliert Verzögerungen für die Verarbeitung
    public void simulateProcessingDelay() {
        try {
            // Verwende Normalverteilung für realistische Verzögerungen
            int delay = (int) (getProcessingTimeMs() * (1 + 0.5 * random.nextGaussian()));
            Thread.sleep(Math.max(10, delay)); // Mindestens 10ms
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // Entscheidet, ob ein simulierter Absturz stattfinden soll
    public boolean shouldSimulateCrash() {
        return random.nextDouble() < getCrashProbability();
    }

    // Entscheidet, ob ein stiller Fehler simuliert werden soll
    public boolean shouldSimulateSilentFailure() {
        return random.nextDouble() < getSilentFailureProbability();
    }

    // Entscheidet, ob eine Buchung aus "fachlichen" Gründen erfolgreich sein soll
    public boolean shouldBookingSucceed() {
        return random.nextDouble() < getBookingSuccessProbability();
    }
}