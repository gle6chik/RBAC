package core;

import managers.AssignmentManager;
import utils.AuditLog;
import assignment.TemporaryAssignment;
import assignment.RoleAssignment;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class ScheduledTasks {
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private ScheduledFuture<?> cleanupTask;
    private ScheduledFuture<?> statsTask;

    public void startExpiredAssignmentsCleaner(AssignmentManager assignmentManager, AuditLog auditLog, int intervalSeconds) {
        cleanupTask = scheduler.scheduleAtFixedRate(() -> {
            try {
                List<RoleAssignment> allAssignments = assignmentManager.findAll();

                List<TemporaryAssignment> expired = allAssignments.stream()
                        .filter(a -> a instanceof TemporaryAssignment)
                        .map(a -> (TemporaryAssignment) a)
                        .filter(TemporaryAssignment::isExpired)
                        .collect(Collectors.toList());

                if (!expired.isEmpty()) {
                    for (TemporaryAssignment temp : expired) {
                        auditLog.log(
                                "EXPIRED_CHECK",
                                "scheduler",
                                temp.user().username() + " -> " + temp.role().getName(),
                                "Temporary assignment expired at: " + temp.getExpiresAt()
                        );
                    }
                }

            } catch (Exception e) {
                System.err.println("[SCHEDULER ERROR] " + e.getMessage());
            }
        }, 0, intervalSeconds, TimeUnit.SECONDS);
    }

    public void startStatisticsLogger(AssignmentManager assignmentManager, AuditLog auditLog, int intervalSeconds) {
        statsTask = scheduler.scheduleAtFixedRate(() -> {
            try {
                List<RoleAssignment> allAssignments = assignmentManager.findAll();
                long totalAssignments = allAssignments.size();
                long activeAssignments = allAssignments.stream()
                        .filter(RoleAssignment::isActive)
                        .count();
                long expiredAssignments = totalAssignments - activeAssignments;

                long temporaryAssignments = allAssignments.stream()
                        .filter(a -> a instanceof TemporaryAssignment)
                        .count();
                long permanentAssignments = totalAssignments - temporaryAssignments;

                String stats = String.format(
                        "Total=%d, Active=%d, Expired=%d, Permanent=%d, Temporary=%d",
                        totalAssignments, activeAssignments, expiredAssignments,
                        permanentAssignments, temporaryAssignments
                );

                // Логирование
                auditLog.log("STATISTICS_REPORT", "scheduler", "system", stats);

            } catch (Exception e) {
                // Только в stderr, но без прерывания пользователя
                System.err.println("[SCHEDULER ERROR] " + e.getMessage());
            }
        }, intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
    }

    public void shutdown() {
        if (cleanupTask != null) {
            cleanupTask.cancel(false);
        }
        if (statsTask != null) {
            statsTask.cancel(false);
        }
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
