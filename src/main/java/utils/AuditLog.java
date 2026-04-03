package utils;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

public class AuditLog {
    private final BlockingQueue<AuditEntry> queue = new LinkedBlockingQueue<>();
    private final List<AuditEntry> entries = new CopyOnWriteArrayList<>();
    private Thread processorThread;
    private volatile boolean running = true;

    public record AuditEntry(
            String timestamp,
            String action,
            String performer,
            String target,
            String details
    ) {
        public String format() {
            return String.format("[%s] %s | Performer: %s | Target: %s | Details: %s",
                    timestamp, action, performer, target, details);
        }

        public String toCsvLine() {
            return String.format("%s,%s,%s,%s,%s",
                    timestamp, action, performer, target,
                    details.replace(",", ";") // Заменяем запятые в деталях
            );
        }
    }

    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public AuditLog() {
        // Запуск потока-обработчика
        processorThread = new Thread(() -> {
            while (running) {
                try {
                    AuditEntry entry = queue.take();
                    entries.add(entry);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        processorThread.setDaemon(true);
        processorThread.start();
    }

    public void log(String action, String performer, String target, String details) {
        ValidationUtils.requireNonEmpty(action, "Action");
        ValidationUtils.requireNonEmpty(performer, "Performer");

        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
        String normalizedTarget = target != null ? target : "N/A";
        String normalizedDetails = details != null ? details : "";

        AuditEntry entry = new AuditEntry(timestamp, action, performer,
                normalizedTarget, normalizedDetails);

        queue.offer(entry);
    }

    public List<AuditEntry> getAll() {
        return new ArrayList<>(entries);
    }

    public List<AuditEntry> getByPerformer(String performer) {
        ValidationUtils.requireNonEmpty(performer, "Performer");
        return entries.stream()
                .filter(entry -> entry.performer().equalsIgnoreCase(performer))
                .collect(Collectors.toList());
    }

    public List<AuditEntry> getByAction(String action) {
        ValidationUtils.requireNonEmpty(action, "Action");
        return entries.stream()
                .filter(entry -> entry.action().equalsIgnoreCase(action))
                .collect(Collectors.toList());
    }

    public void printLog() {
        if (entries.isEmpty()) {
            System.out.println("Audit log is empty.");
            return;
        }

        System.out.println("\n" + "=".repeat(100));
        System.out.println("AUDIT LOG");
        System.out.println("=".repeat(100));

        for (int i = 0; i < entries.size(); i++) {
            AuditEntry entry = entries.get(i);
            System.out.printf("%3d. %s\n", i + 1, entry.format());
            if (i < entries.size() - 1) {
                System.out.println("-".repeat(100));
            }
        }

        System.out.println("=".repeat(100));
        System.out.printf("Total entries: %d\n", entries.size());
    }

    public void saveToFile(String filename) {
        ValidationUtils.requireNonEmpty(filename, "Filename");

        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            // Заголовок CSV
            writer.println("Timestamp,Action,Performer,Target,Details");

            // Записи
            for (AuditEntry entry : entries) {
                writer.println(entry.toCsvLine());
            }

            System.out.println("Audit log saved to: " + filename);
        } catch (IOException e) {
            System.err.println("Error saving audit log: " + e.getMessage());
        }
    }

    public void shutdown() {
        running = false;
        processorThread.interrupt();
    }
}
