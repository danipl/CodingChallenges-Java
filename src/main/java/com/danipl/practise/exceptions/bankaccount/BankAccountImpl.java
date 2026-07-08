package com.danipl.practise.exceptions.bankaccount;

/**
 * Implementation of {@link BankAccount}.
 *
 * <p>
 * Implement all methods and make all tests pass. Pay close attention to:
 * <ul>
 * <li>The order of validation checks (frozen first, then amount, then
 * balance).</li>
 * <li>Using the correct exception type for each failure mode.</li>
 * <li>Defensive validation on construction.</li>
 * </ul>
 */
public final class BankAccountImpl implements BankAccount {

    private double balance;
    private boolean frozen;

    public BankAccountImpl(final double initialBalance) {
        if (initialBalance < 0) {
            throw new InvalidAmountException("Initial balance must be greater than zero");
        }
        balance = initialBalance;
        frozen = false;
    }

    @Override
    public void deposit(final double amount) {
        checkIfFrozen();
        checkPositiveAmount(amount);
        balance += amount;
    }

    @Override
    public void withdraw(final double amount) {
        checkIfFrozen();
        checkPositiveAmount(amount);
        if (balance < amount) {
            throw new InsufficientFundsException("There are not sufficient funds to withdraw " + amount);
        }
        balance -= amount;
    }

    @Override
    public void freeze() {
        frozen = true;
    }

    @Override
    public void unfreeze() {
        frozen = false;
    }

    @Override
    public double getBalance() {
        return balance;
    }

    @Override
    public boolean isFrozen() {
        return frozen;
    }

    private void checkPositiveAmount(final double amount) {
        if (amount <= 0) {
            throw new InvalidAmountException("Amount must be greater than zero");
        }
    }

    private void checkIfFrozen() {
        if (isFrozen()) {
            throw new AccountFrozenException("The account is frozen");
        }
    }

}
