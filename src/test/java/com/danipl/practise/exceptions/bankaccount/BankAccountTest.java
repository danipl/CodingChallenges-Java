package com.danipl.practise.exceptions.bankaccount;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("BankAccount")
class BankAccountTest {

    @Nested
    @DisplayName("Construction")
    class Construction {

        @Test
        @DisplayName("should create account with valid initial balance")
        void validInitialBalance() {
            var account = BankAccount.of(100.0);
            assertEquals(100.0, account.getBalance());
            assertFalse(account.isFrozen());
        }

        @Test
        @DisplayName("should create account with zero balance")
        void zeroInitialBalance() {
            var account = BankAccount.of(0.0);
            assertEquals(0.0, account.getBalance());
        }

        @Test
        @DisplayName("should reject negative initial balance")
        void negativeInitialBalance() {
            assertThrows(BankAccount.InvalidAmountException.class, () -> BankAccount.of(-10.0));
        }
    }

    @Nested
    @DisplayName("Deposit")
    class Deposit {

        @Test
        @DisplayName("should increase balance on valid deposit")
        void validDeposit() {
            var account = BankAccount.of(50.0);
            account.deposit(25.0);
            assertEquals(75.0, account.getBalance());
        }

        @Test
        @DisplayName("should reject zero deposit")
        void zeroDeposit() {
            var account = BankAccount.of(50.0);
            assertThrows(BankAccount.InvalidAmountException.class, () -> account.deposit(0.0));
        }

        @Test
        @DisplayName("should reject negative deposit")
        void negativeDeposit() {
            var account = BankAccount.of(50.0);
            assertThrows(BankAccount.InvalidAmountException.class, () -> account.deposit(-5.0));
        }

        @Test
        @DisplayName("should reject deposit on frozen account")
        void depositOnFrozen() {
            var account = BankAccount.of(50.0);
            account.freeze();
            assertThrows(BankAccount.AccountFrozenException.class, () -> account.deposit(10.0));
        }
    }

    @Nested
    @DisplayName("Withdraw")
    class Withdraw {

        @Test
        @DisplayName("should decrease balance on valid withdrawal")
        void validWithdrawal() {
            var account = BankAccount.of(100.0);
            account.withdraw(40.0);
            assertEquals(60.0, account.getBalance());
        }

        @Test
        @DisplayName("should allow withdrawing the full balance")
        void withdrawFullBalance() {
            var account = BankAccount.of(100.0);
            account.withdraw(100.0);
            assertEquals(0.0, account.getBalance());
        }

        @Test
        @DisplayName("should reject zero withdrawal")
        void zeroWithdrawal() {
            var account = BankAccount.of(100.0);
            assertThrows(BankAccount.InvalidAmountException.class, () -> account.withdraw(0.0));
        }

        @Test
        @DisplayName("should reject negative withdrawal")
        void negativeWithdrawal() {
            var account = BankAccount.of(100.0);
            assertThrows(BankAccount.InvalidAmountException.class, () -> account.withdraw(-10.0));
        }

        @Test
        @DisplayName("should reject withdrawal exceeding balance")
        void insufficientFunds() {
            var account = BankAccount.of(50.0);
            var ex = assertThrows(BankAccount.InsufficientFundsException.class,
                    () -> account.withdraw(50.01));
            assertTrue(ex.getMessage().contains("50.0"));
        }

        @Test
        @DisplayName("should reject withdrawal on frozen account")
        void withdrawOnFrozen() {
            var account = BankAccount.of(100.0);
            account.freeze();
            assertThrows(BankAccount.AccountFrozenException.class, () -> account.withdraw(10.0));
        }
    }

    @Nested
    @DisplayName("Freeze / Unfreeze")
    class FreezeUnfreeze {

        @Test
        @DisplayName("should freeze and unfreeze account")
        void freezeAndUnfreeze() {
            var account = BankAccount.of(100.0);
            account.freeze();
            assertTrue(account.isFrozen());

            account.unfreeze();
            assertFalse(account.isFrozen());
        }

        @Test
        @DisplayName("should allow operations after unfreeze")
        void operationsAfterUnfreeze() {
            var account = BankAccount.of(100.0);
            account.freeze();
            account.unfreeze();
            assertDoesNotThrow(() -> account.deposit(10.0));
            assertDoesNotThrow(() -> account.withdraw(5.0));
        }
    }

    @Nested
    @DisplayName("Exception Hierarchy")
    class ExceptionHierarchy {

        @Test
        @DisplayName("all custom exceptions should extend BankAccountException")
        void hierarchyCheck() {
            assertAll(
                () -> assertTrue(
                    BankAccount.BankAccountException.class.isAssignableFrom(BankAccount.InvalidAmountException.class)),
                () -> assertTrue(
                    BankAccount.BankAccountException.class.isAssignableFrom(BankAccount.InsufficientFundsException.class)),
                () -> assertTrue(
                    BankAccount.BankAccountException.class.isAssignableFrom(BankAccount.AccountFrozenException.class))
            );
        }
    }
}
