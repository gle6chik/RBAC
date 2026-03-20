package utils;

import java.util.List;

public class FormatUtils {
    public static String formatTable(String[] headers, List<String[]> rows) {
        if (headers == null || headers.length == 0) {
            return "";
        }

        int colCount = headers.length;
        int[] colWidths = new int[colCount];

        // Вычисляем ширину каждой колонки на основе заголовков
        for (int i = 0; i < colCount; i++) {
            colWidths[i] = headers[i].length();
        }

        // Учитываем данные строк
        for (String[] row : rows) {
            for (int i = 0; i < Math.min(row.length, colCount); i++) {
                if (row[i] != null && row[i].length() > colWidths[i]) {
                    colWidths[i] = row[i].length();
                }
            }
        }

        // Добавляем отступы по бокам
        for (int i = 0; i < colCount; i++) {
            colWidths[i] += 2;
        }

        StringBuilder sb = new StringBuilder();

        // Верхняя граница
        sb.append("+");
        for (int width : colWidths) {
            sb.append("-".repeat(width));
            sb.append("+");
        }
        sb.append("\n");

        // Заголовки
        sb.append("|");
        for (int i = 0; i < colCount; i++) {
            sb.append(padCenter(headers[i], colWidths[i]));
            sb.append("|");
        }
        sb.append("\n");

        // Разделитель после заголовков
        sb.append("+");
        for (int width : colWidths) {
            sb.append("-".repeat(width));
            sb.append("+");
        }
        sb.append("\n");

        // Строки данных
        for (String[] row : rows) {
            sb.append("|");
            for (int i = 0; i < colCount; i++) {
                String cell = (i < row.length && row[i] != null) ? row[i] : "";
                sb.append(padRight(cell, colWidths[i]));
                sb.append("|");
            }
            sb.append("\n");
        }

        // Нижняя граница
        sb.append("+");
        for (int width : colWidths) {
            sb.append("-".repeat(width));
            sb.append("+");
        }
        sb.append("\n");

        return sb.toString();
    }

    public static String formatBox(String text) {
        String[] lines = text.split("\n");
        int maxLength = 0;
        for (String line : lines) {
            maxLength = Math.max(maxLength, line.length());
        }
        int boxWidth = maxLength + 4; // по 2 пробела с каждой стороны

        StringBuilder sb = new StringBuilder();
        // Верхняя граница
        sb.append("┌").append("─".repeat(boxWidth - 2)).append("┐\n");
        // Строки текста
        for (String line : lines) {
            sb.append("│ ").append(padRight(line, maxLength)).append(" │\n");
        }
        // Нижняя граница
        sb.append("└").append("─".repeat(boxWidth - 2)).append("┘\n");
        return sb.toString();
    }

    public static String formatHeader(String text) {
        String line = "=".repeat(text.length() + 4);
        return String.format("%s\n= %s =\n%s\n", line, text, line);
    }

    public static String truncate(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        if (maxLength <= 3) return text.substring(0, maxLength);
        return text.substring(0, maxLength - 3) + "...";
    }

    public static String padRight(String text, int length) {
        if (text == null) text = "";
        if (text.length() > length) {
            return truncate(text, length);
        }
        return String.format("%-" + length + "s", text);
    }

    public static String padLeft(String text, int length) {
        if (text == null) text = "";
        if (text.length() > length) {
            return truncate(text, length);
        }
        return String.format("%" + length + "s", text);
    }

    private static String padCenter(String text, int width) {
        if (text == null) text = "";
        if (text.length() > width) {
            return truncate(text, width);
        }
        int spaces = width - text.length();
        int left = spaces / 2;
        int right = spaces - left;
        return " ".repeat(left) + text + " ".repeat(right);
    }
}
