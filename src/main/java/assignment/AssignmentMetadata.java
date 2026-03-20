package assignment;

import utils.DateUtils;

import java.time.format.DateTimeFormatter;

public record AssignmentMetadata(String assignedBy, String assignedAt, String reason) {
    public static AssignmentMetadata now(String assignedBy, String reason) {
        String now = DateUtils.getCurrentDateTime();
        return new AssignmentMetadata(assignedBy, now, reason);
    }

    public String format() {
        if (reason == null || reason.isEmpty()) return String.format("Assigned by %s at %s", assignedBy, assignedAt);
        else return String.format("Assigned by: %s at %s, Reason: %s", assignedBy, assignedAt, reason);
    }
}
