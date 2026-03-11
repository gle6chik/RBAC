package assignment;

import model.Role;
import model.User;
import utils.DateUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Date;

public class TemporaryAssignment extends AbstractRoleAssignment {
    String expiresAt;
    boolean autoRenew;

    public TemporaryAssignment(User user,
                               Role role,
                               AssignmentMetadata metadata,
                               String expiresAt,
                               boolean autoRenew) {
        super(user, role, metadata);
        this.expiresAt = expiresAt;
        this.autoRenew = autoRenew;
    }

    @Override
    public boolean isActive() {
        String now = DateUtils.getCurrentDateTime();
        return DateUtils.isBefore(now, expiresAt);
    }

    @Override
    public String assignmentType() {
        return "TEMPORARY";
    }

    public void extend(String newExpirationDate) {
        expiresAt = newExpirationDate;
    }

    public boolean isExpired() {
        return !isActive();
    }

    public String getTimeRemaining() {
        return "s";
//        if (!isActive()) {
//            return "Expired";
//        }
//
//        LocalDateTime now = LocalDateTime.now();
//        LocalDateTime expiry = LocalDateTime.parse(expiresAt, FORMATTER);
//
//        long days = ChronoUnit.DAYS.between(now, expiry);
//        long hours = ChronoUnit.HOURS.between(now, expiry) % 24;
//        long minutes = ChronoUnit.MINUTES.between(now, expiry) % 60;
//
//        return String.format("%d days, %d hours, %d minutes", days, hours, minutes);
    }

    public String getExpiresAt() {
        return expiresAt;
    }

    @Override
    public String summary() {
        String status = isActive() ? "ACTIVE" : "EXPIRED";
        String type = assignmentType();
        String reason = metadata().reason() != null ? metadata().reason() : "No reason";

        return String.format(
                "[%s] %s assigned to %s by %s at %s\n" +
                        "Expires: %s\nReason: %s\nStatus: %s",
                type,
                role().getName(),
                user().username(),
                metadata().assignedBy(),
                metadata().assignedAt(),
                expiresAt,
                reason,
                status
        );
    }
}
