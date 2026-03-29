package com.bankapp.model;

import com.bankapp.enums.AccountType;
import com.bankapp.util.BankConstants;

import java.time.LocalDateTime;

public class SavingsAccount extends Account {
    public SavingsAccount(int id, int clientId, double balance, LocalDateTime createdAt) {
        super(id, clientId, AccountType.SAVINGS, balance, createdAt);
    }

    @Override
    public double calculateInterest() {
        return getBalance() * BankConstants.SAVINGS_MONTHLY_INTEREST_RATE;
    }
}
