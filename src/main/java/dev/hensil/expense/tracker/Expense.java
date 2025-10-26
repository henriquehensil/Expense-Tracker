package dev.hensil.expense.tracker;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDate;
import java.util.Objects;

public abstract class Expense {

    private final int id;

    private @NotNull String description;
    private @NotNull LocalDate date;

    private double amount;

    protected Expense(int id, @NotNull String description, double amount) {
        this(id, description, LocalDate.now(), amount);
    }

    protected Expense(int id, @NotNull String description, @NotNull LocalDate date, double amount) {
        if (id < 0) {
            throw new IllegalArgumentException("Illegal id value");
        } else if (amount < 0) {
            throw new IllegalArgumentException("Amount cannot be negative: " + amount);
        }

        this.id = id;
        this.description = description;
        this.amount = amount;
        this.date = date;
    }

    // Getters

    public final int getId() {
        return id;
    }

    public final @NotNull String getDescription() {
        return description;
    }

    public final @NotNull LocalDate getDate() {
        return date;
    }

    public final double getAmount() {
        return amount;
    }

    // Abstract

    public abstract void delete();

    // Setters

    public void setAmount(double amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Amount cannot be negative: " + amount);
        }

        this.amount = amount;
    }

    public void setDescription(@NotNull String description) {
        this.description = description;
    }

    public void setDate(@NotNull LocalDate date) {
        if (date.getYear() != this.date.getYear()) {
            throw new IllegalArgumentException("Its no possible to set new date with another years");
        }

        this.date = date;
    }

    @Override
    public @NotNull String toString() {
        @NotNull String column = "ID" + "  " + "DATE" + "       " + "DESCRIPTION" + "   " + "AMOUNT";
        @NotNull String content = this.id + "   " + this.date + "  " + this.description + "       " + "$" + this.amount;
        return column + "\r\n" + content;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (!(o instanceof Expense expense)) return false;
        return id == expense.id;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}