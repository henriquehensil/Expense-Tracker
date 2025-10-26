package dev.hensil.expense.tracker.main;

import dev.hensil.expense.tracker.Expense;
import dev.hensil.expense.tracker.Tracker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.Locale;
import java.util.Scanner;

public final class Main {

    // Static initializers

    private static final @NotNull DateTimeFormatter FORMATTER = new DateTimeFormatterBuilder()
            .parseLenient()
            .appendOptional(DateTimeFormatter.ofPattern("dd-MM-uuuu"))
            .appendOptional(DateTimeFormatter.ofPattern("dd/MM/uuuu"))
            .appendOptional(DateTimeFormatter.ISO_LOCAL_DATE)
            .appendOptional(new DateTimeFormatterBuilder()
                    .appendPattern("dd-MM-uu")
                    .parseDefaulting(ChronoField.YEAR_OF_ERA, 2000)
                    .toFormatter())
            .toFormatter(Locale.of("pt", "BR"));

    public static void main(@NotNull String @NotNull [] args) {
        @NotNull Tracker tracker = Tracker.getInstance();
        @NotNull Scanner scanner = new Scanner(System.in);

        @NotNull String input;
        while (!(input = scanner.nextLine().toLowerCase()).equalsIgnoreCase("exit")) {
            @NotNull String @NotNull [] parts = input.split("--");
            try {
                if (input.startsWith("add")) {
                    if (parts.length < 3 || parts.length > 4) {
                        System.err.println("Invalid add command");
                    }

                    @NotNull String description;
                    @Nullable LocalDate date = null;
                    double amount;

                    if (parts.length == 3) {
                        description = parts[1];
                        amount = Double.parseDouble(parts[2]);
                    } else {
                        description = parts[2];
                        date = LocalDate.parse(parts[1].replace("/", "-"), FORMATTER);
                        amount = Double.parseDouble(parts[3]);
                    }

                    @NotNull Expense expense = date == null ? tracker.create(description, amount) : tracker.create(description, date, amount);
                    System.out.println("Added successfully (ID: " + expense.getId() + ")");
                } else if (input.startsWith("delete")) {
                    if (parts.length != 2) {
                        System.err.println("Invalid delete command");
                    }

                    int id = Integer.parseInt(parts[1]);
                    @Nullable Expense expense = tracker.get(id).orElse(null);
                    if (expense == null) {
                        System.err.println("No expense for id " + id);
                        continue;
                    }

                    expense.delete();
                    System.out.println("Successfully deleted: " + expense.getId());
                } else if (input.equalsIgnoreCase("summary")) {
                    System.out.println("$" + tracker.getSummary());
                } else if (input.startsWith("summary")) {
                    if (parts.length != 2) {
                        System.err.println("Invalid summary filter command");
                        continue;
                    }

                    int mouth = Integer.parseInt(parts[1]);
                    @NotNull Month month = Month.of(mouth);

                    System.out.println("$" + tracker.getSummary(month));
                } else if (input.equalsIgnoreCase("list")) {
                    tracker.getAll().forEach(System.out::println);
                } else {
                    System.err.println("Invalid command");
                }
            } catch (Throwable e) {
                System.err.println(e.getMessage());
            }
        }
    }

    // Objects

    private Main() {
        throw new UnsupportedOperationException();
    }
}