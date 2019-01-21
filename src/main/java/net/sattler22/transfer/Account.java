package net.sattler22.transfer;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Revolut&copy; Account
 * 
 * @author Pete Sattler
 * @version January 2019
 * @implSpec This class is immutable and thread-safe
 */
public final class Account implements Serializable {

    private static final long serialVersionUID = -9088851442796213109L;
    private final int number;
    private final int customerId;
    private final BigDecimal balance;
    private final Object lockObject = new Object();

    /**
     * Constructs a new account
     */
    public Account(int number, int customerId, BigDecimal balance) {
        this.number = number;
        this.customerId = customerId;
        this.balance = (balance == null) ? BigDecimal.ZERO : balance;
    }

    /**
     * Credit funds to the account
     */
    public Account credit(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Amount must be greater than zero");
        synchronized (lockObject) {
            return new Account(number, customerId, balance.add(amount));
        }
    }

    /**
     * Debit funds from the account
     */
    public Account debit(BigDecimal amount) {
        synchronized (lockObject) {
            final BigDecimal newBalance = balance.add(amount);
            if (newBalance.compareTo(BigDecimal.ZERO) < 0)
                throw new IllegalStateException("Transaction would lead to an overdrawn account");
            return new Account(number, customerId, newBalance);
        }
    }

    public int getNumber() {
        return number;
    }

    public int getCustomerId() {
        return customerId;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    @Override
    public int hashCode() {
        return Objects.hash(number, customerId, balance);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other)
            return true;
        if (other == null)
            return false;
        if (this.getClass() != other.getClass())
            return false;
        final Account that = (Account) other;
        return this.number == that.number && this.customerId == that.customerId && Objects.equals(this.balance, that.balance);
    }

    @Override
    public String toString() {
        return String.format("Account [number=%s, customerId=%s, balance=%s]", number, customerId, balance);
    }
}