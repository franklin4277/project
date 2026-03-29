package com.bankapp.model;

import com.bankapp.enums.TransactionType;

import java.time.LocalDateTime;

public class Transaction {
    private final int id;
    private final int accountId;
    private final int clientId;
    private final TransactionType transactionType;
    private final double amount;
    private final String channel;
    private final String description;
    private final String reference;
    private final LocalDateTime createdAt;

    public Transaction(int id, int accountId, int clientId, TransactionType transactionType, double amount,
                       String channel, String description, String reference, LocalDateTime createdAt) {
        this.id = id;
        this.accountId = accountId;
        this.clientId = clientId;
        this.transactionType = transactionType;
        this.amount = amount;
        this.channel = channel;
        this.description = description;
        this.reference = reference;
        this.createdAt = createdAt;
    }

    public int getId() {
        return id;
    }

    public int getAccountId() {
        return accountId;
    }

    public int getClientId() {
        return clientId;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public double getAmount() {
        return amount;
    }

    public String getChannel() {
        return channel;
    }

    public String getDescription() {
        return description;
    }

    public String getReference() {
        return reference;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
