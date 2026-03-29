package com.bankapp.model;

import com.bankapp.enums.LoanStatus;

import java.time.LocalDateTime;

public class Loan {
    private final int id;
    private final int clientId;
    private final int accountId;
    private final double requestedAmount;
    private double outstandingBalance;
    private LoanStatus status;
    private final boolean topUp;
    private final Double topUpCharge;
    private final Integer previousLoanId;
    private final LocalDateTime createdAt;

    public Loan(int id, int clientId, int accountId, double requestedAmount, double outstandingBalance,
                LoanStatus status, boolean topUp, Double topUpCharge, Integer previousLoanId,
                LocalDateTime createdAt) {
        this.id = id;
        this.clientId = clientId;
        this.accountId = accountId;
        this.requestedAmount = requestedAmount;
        this.outstandingBalance = outstandingBalance;
        this.status = status;
        this.topUp = topUp;
        this.topUpCharge = topUpCharge;
        this.previousLoanId = previousLoanId;
        this.createdAt = createdAt;
    }

    public int getId() {
        return id;
    }

    public int getClientId() {
        return clientId;
    }

    public int getAccountId() {
        return accountId;
    }

    public double getRequestedAmount() {
        return requestedAmount;
    }

    public double getOutstandingBalance() {
        return outstandingBalance;
    }

    public LoanStatus getStatus() {
        return status;
    }

    public boolean isTopUp() {
        return topUp;
    }

    public Double getTopUpCharge() {
        return topUpCharge;
    }

    public Integer getPreviousLoanId() {
        return previousLoanId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setOutstandingBalance(double outstandingBalance) {
        this.outstandingBalance = outstandingBalance;
    }

    public void setStatus(LoanStatus status) {
        this.status = status;
    }
}
