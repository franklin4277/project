package com.bankapp.util;

import com.bankapp.enums.AccountType;
import com.bankapp.model.Account;
import com.bankapp.model.CurrentAccount;
import com.bankapp.model.FixedDepositAccount;
import com.bankapp.model.SavingsAccount;

import java.time.LocalDate;
import java.time.LocalDateTime;

public final class AccountFactory {
    private AccountFactory() {
    }

    public static Account createAccount(int id, int clientId, AccountType type, double balance,
                                        LocalDateTime createdAt, LocalDate maturityDate) {
        return switch (type) {
            case SAVINGS -> new SavingsAccount(id, clientId, balance, createdAt);
            case FIXED_DEPOSIT -> new FixedDepositAccount(id, clientId, balance, createdAt, maturityDate);
            case CURRENT -> new CurrentAccount(id, clientId, balance, createdAt);
        };
    }
}
