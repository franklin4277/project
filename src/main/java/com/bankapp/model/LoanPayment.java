package com.bankapp.model;

import java.time.LocalDateTime;

public class LoanPayment {
    private final int id;
    private final int loanId;
    private final int accountId;
    private final double amount;
    private final String note;
    private final LocalDateTime createdAt;

    public LoanPayment(int id, int loanId, int accountId, double amount, String note, LocalDateTime createdAt) {
        this.id = id;
        this.loanId = loanId;
        this.accountId = accountId;
        this.amount = amount;
        this.note = note;
        this.createdAt = createdAt;
    }

    public int getId() {
        return id;
    }

    public int getLoanId() {
        return loanId;
    }

    public int getAccountId() {
        return accountId;
    }

    public double getAmount() {
        return amount;
    }

    public String getNote() {
        return note;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
