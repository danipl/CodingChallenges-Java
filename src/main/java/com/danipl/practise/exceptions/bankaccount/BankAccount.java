package com.danipl.practise.exceptions.bankaccount;

/**
 * A simple bank account with custom exception-driven error semantics.
 *
 * <p>Requirements:
 * <ul>
 *   <li>Deposits and withdrawals must reject non-positive amounts with {@link InvalidAmountException}.</li>
 *   <li>Withdrawals exceeding the current balance throw {@link InsufficientFundsException}.</li>
 *   <li>No operation is allowed on a frozen account — throws {@link AccountFrozenException}.</li>
 * </ul>
 */
public interface BankAccount {

    /**
     * Factory method to create a new account with the given initial balance.
     *
     * @param initialBalance the starting balance (must be non-negative)
     * @return a new {@link BankAccount} instance
     * @throws InvalidAmountException if {@code initialBalance} is negative
     */
    static BankAccount of(double initialBalance) {
        return new BankAccountImpl(initialBalance);
    }

    /**
     * Deposits the given amount into the account.
     *
     * @param amount the amount to deposit (must be positive)
     * @throws InvalidAmountException  if {@code amount} is not positive
     * @throws AccountFrozenException  if the account is frozen
     */
    void deposit(double amount);

    /**
     * Withdraws the given amount from the account.
     *
     * @param amount the amount to withdraw (must be positive)
     * @throws InvalidAmountException      if {@code amount} is not positive
     * @throws InsufficientFundsException  if {@code amount} exceeds the current balance
     * @throws AccountFrozenException      if the account is frozen
     */
    void withdraw(double amount);

    /**
     * Freezes the account, blocking all subsequent operations until unfrozen.
     */
    void freeze();

    /**
     * Unfreezes the account, re-enabling operations.
     */
    void unfreeze();

    /**
     * Returns the current balance.
     *
     * @return the balance
     */
    double getBalance();

    /**
     * Returns whether the account is currently frozen.
     *
     * @return {@code true} if frozen
     */
    boolean isFrozen();

    // === Exception Hierarchy ===

    /** Base exception for all bank account errors. */
    class BankAccountException extends RuntimeException {
        public BankAccountException(String message) {
            super(message);
        }
    }

    /** Thrown when an amount is zero or negative. */
    class InvalidAmountException extends BankAccountException {
        public InvalidAmountException(String message) {
            super(message);
        }
    }

    /** Thrown when a withdrawal exceeds the available balance. */
    class InsufficientFundsException extends BankAccountException {
        public InsufficientFundsException(String message) {
            super(message);
        }
    }

    /** Thrown when an operation is attempted on a frozen account. */
    class AccountFrozenException extends BankAccountException {
        public AccountFrozenException(String message) {
            super(message);
        }
    }
}
