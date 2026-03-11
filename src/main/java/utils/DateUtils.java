package utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class DateUtils {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static String getCurrentDate() {
        return LocalDate.now().format(DATE_FORMATTER);
    }

    public static String getCurrentDateTime() {
        return LocalDateTime.now().format(DATETIME_FORMATTER);
    }

    public static boolean isBefore(String date1, String date2) {
        if (date1 == null || date2 == null) return false;
        return date1.compareTo(date2) < 0;
    }

    public static boolean isAfter(String date1, String date2) {
        if (date1 == null || date2 == null) return false;
        return date1.compareTo(date2) > 0;
    }

    public static String addDays(String date, int days) {
        if (date == null) return null;
        try {
            if (date.length() == 10) { // только дата
                LocalDate d = LocalDate.parse(date, DATE_FORMATTER);
                return d.plusDays(days).format(DATE_FORMATTER);
            } else { // дата и время
                LocalDateTime dt = LocalDateTime.parse(date, DATETIME_FORMATTER);
                return dt.plusDays(days).format(DATETIME_FORMATTER);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid date format: " + date);
        }
    }

    public static String formatRelativeTime(String date) {
        if (date == null) return "";
        try {
            LocalDateTime target;
            LocalDateTime now = LocalDateTime.now();
            if (date.length() == 10) {
                target = LocalDate.parse(date, DATE_FORMATTER).atStartOfDay();
            } else {
                target = LocalDateTime.parse(date, DATETIME_FORMATTER);
            }

            long days = ChronoUnit.DAYS.between(now.toLocalDate(), target.toLocalDate());
            if (days == 0) {
                long hours = ChronoUnit.HOURS.between(now, target);
                if (hours == 0) {
                    long minutes = ChronoUnit.MINUTES.between(now, target);
                    if (minutes == 0) return "now";
                    if (minutes > 0) return "in " + minutes + " minutes";
                    else return Math.abs(minutes) + " minutes ago";
                }
                if (hours > 0) return "in " + hours + " hours";
                else return Math.abs(hours) + " hours ago";
            } else if (days > 0) {
                return "in " + days + " days";
            } else {
                return Math.abs(days) + " days ago";
            }
        } catch (Exception e) {
            return "unknown date";
        }
    }
}
