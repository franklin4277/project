package com.bankapp.model;

import com.bankapp.enums.AccountType;

import java.time.LocalDateTime;

public class CurrentAccount extends Account {
    public CurrentAccount(int id, int clientId, double balance, LocalDateTime createdAt) {
        super(id, clientId, AccountType.CURRENT, balance, createdAt);
    }

    @Override
    public double calculateInterest() {
        return 0.0;
    }
}
