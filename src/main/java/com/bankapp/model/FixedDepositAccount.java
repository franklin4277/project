package com.bankapp.model;

import com.bankapp.enums.AccountType;
import com.bankapp.util.BankConstants;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class FixedDepositAccount extends Account {
    private final LocalDate maturityDate;

    public FixedDepositAccount(int id, int clientId, double balance, LocalDateTime createdAt, LocalDate maturityDate) {
        super(id, clientId, AccountType.FIXED_DEPOSIT, balance, createdAt);
        this.maturityDate = maturityDate;
    }

    public LocalDate getMaturityDate() {
        return maturityDate;
    }

    public boolean isMatured(LocalDate today) {
        return !today.isBefore(maturityDate);
    }

    @Override
    public double calculateInterest() {
        return getBalance() * BankConstants.FIXED_DEPOSIT_INTEREST_RATE;
    }
}
