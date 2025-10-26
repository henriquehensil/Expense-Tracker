package dev.hensil.expense.tracker;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.io.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.time.Year;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

public final class Tracker {

    // Static initializers

    private static final @NotNull Tracker INSTANCE = new Tracker();

    public static @NotNull Tracker getInstance() {
        return INSTANCE;
    }

    // Objects

    private final @NotNull Map<@NotNull Integer, @NotNull ExpenseImpl> expenses;
    private final @NotNull Store store;
    private final @NotNull AtomicInteger autoIncrement;

    private Tracker() {
        try {
            this.expenses = new HashMap<>();
            this.store = new Store();
            this.autoIncrement = new AtomicInteger(expenses.size());

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                for (@NotNull ExpenseImpl expense : expenses.values()) try {
                    expense.close();
                } catch (IOException e) {
                    throw new RuntimeException("Cannot save", e);
                }
            }));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public @NotNull Optional<@NotNull Expense> get(int id) {
        return Optional.ofNullable(this.expenses.get(id));
    }

    public @Unmodifiable @NotNull Collection<@NotNull Expense> getAll() {
        return this.expenses.values().stream()
                .filter(expense -> !expense.isDeleted())
                .collect(Collectors.toUnmodifiableSet());
    }

    public double getSummary() {
        return getAll().stream()
                .flatMapToDouble(expense -> DoubleStream.of(expense.getAmount()))
                .sum();
    }

    public double getSummary(@NotNull Month month) {
        return getAll().stream()
                .filter(expense -> expense.getDate().getMonth() == month)
                .flatMapToDouble(expense -> DoubleStream.of(expense.getAmount()))
                .sum();
    }

    // Modules

    public @NotNull Expense create(@NotNull String description, double amount) {
        return create(description, LocalDate.now(), amount);
    }

    public @NotNull Expense create(@NotNull String description, @NotNull LocalDate date, double amount) {
        if (description.isBlank()) {
            throw new IllegalStateException("Description cannot be blank");
        }

        int year = Year.now().getValue();
        if (date.getYear() != Year.now().getValue()) {
            throw new IllegalArgumentException("Cannot use date before or after that " + year + " year");
        }

        amount = new BigDecimal(amount).doubleValue();

        int id = autoIncrement.getAndIncrement();
        @NotNull ExpenseImpl expense = new ExpenseImpl(id, description, date, amount);

        this.expenses.put(expense.getId(), expense);
        return expense;
    }

    // Classes

    private final class ExpenseImpl extends Expense implements Closeable {

        private boolean deleted = false;

        ExpenseImpl(int id, @NotNull String description, @NotNull LocalDate date, double amount) {
            super(id, description, date, amount);
        }

        ExpenseImpl(int id, @NotNull String description, double amount) {
            super(id, description, amount);
        }

        @Override
        public void setAmount(double amount) {
            if (isDeleted()) {
                throw new IllegalStateException("This expense doest not exists anymore");
            }

            super.setAmount(amount);
        }

        @Override
        public void setDescription(@NotNull String description) {
            if (isDeleted()) {
                throw new IllegalStateException("This expense doest not exists anymore");
            }

            super.setDescription(description);
        }

        @Override
        public void setDate(@NotNull LocalDate date) {
            if (isDeleted()) {
                throw new IllegalStateException("This expense doest not exists anymore");
            }

            super.setDate(date);
        }

        public boolean isDeleted() {
            return deleted;
        }

        @Override
        public void delete() {
            if (isDeleted()) {
                throw new IllegalStateException("This expense is already deleted");
            }

            this.deleted = true;
        }

        @Override
        public void close() throws IOException {
            if (isDeleted()) {
                Tracker.this.store.delete(this);
            } else {
                Tracker.this.store.save(this);
            }
        }
    }

    private final class Store {

        private final @NotNull File root = new File(System.getProperty("user.dir"), "expenses");
        private final @NotNull Set<@NotNull File> files = new HashSet<>();

        private Store() throws IOException {
            if (!root.exists() && !root.mkdirs()) {
                throw new RuntimeException("Cannot create the root expenses directory: " + root.getAbsolutePath());
            } else if (!root.isDirectory()) {
                throw new RuntimeException("Root must to be a directory: " + root.getAbsolutePath());
            }

            @NotNull File @Nullable [] files = root.listFiles(file -> file.getName().startsWith("expense") && file.getName().endsWith(".json"));
            if (files == null) {
                throw new IOException("Cannot load expenses file in the root directory: " + root.getAbsolutePath());
            } else for (@NotNull File file : files) {
                try (@NotNull FileReader reader = new FileReader(file)) {
                    @NotNull JsonObject object = JsonParser.parseReader(reader).getAsJsonObject();
                    @NotNull ExpenseImpl expense = deserialize(object);
                    Tracker.this.expenses.put(expense.getId(), expense);
                    this.files.add(file);
                }
            }
        }

        // Modules

        public void save(@NotNull Expense expense) throws IOException {
            @NotNull File file = toAbstractFile(expense);

            try (@NotNull FileWriter writer = new FileWriter(file, false)) {
                @NotNull JsonObject object = serialize(expense);
                writer.write(object.toString());
            }

            this.files.add(file);
        }

        public void delete(@NotNull Expense expense) {
            @NotNull File file = toAbstractFile(expense);
            file.deleteOnExit();
            this.files.remove(file);
        }

        private @NotNull File toAbstractFile(@NotNull Expense expense) {
            @NotNull String name = "expense-" + expense.getId() + ".json";
            return new File(root, name);
        }

        private @NotNull JsonObject serialize(@NotNull Expense expense) {
            @NotNull JsonObject object = new JsonObject();

            object.addProperty("id", expense.getId());
            object.addProperty("date", expense.getDate().toString());
            object.addProperty("description", expense.getDescription());
            object.addProperty("amount", expense.getAmount());

            return object;
        }

        private @NotNull ExpenseImpl deserialize(@NotNull JsonObject object) {
            try {
                int id = object.get("id").getAsInt();
                @NotNull String description = object.get("description").getAsString();
                @NotNull LocalDate date = LocalDate.parse(
                        object.get("date").getAsString()
                );
                double amount = object.get("amount").getAsDouble();

                return new ExpenseImpl(id, description, date, amount);
            } catch (Throwable e) {
                throw new RuntimeException("Cannot deserialize from json: " + object, e);
            }
        }
    }
}