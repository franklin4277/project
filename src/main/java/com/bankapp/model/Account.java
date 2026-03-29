package com.bankapp.model;

import com.bankapp.enums.AccountType;

import java.time.LocalDateTime;

public abstract class Account {
    private final int id;
    private final int clientId;
    private final AccountType accountType;
    private double balance;
    private final LocalDateTime createdAt;

    protected Account(int id, int clientId, AccountType accountType, double balance, LocalDateTime createdAt) {
        this.id = id;
        this.clientId = clientId;
        this.accountType = accountType;
        this.balance = balance;
        this.createdAt = createdAt;
    }

    public int getId() {
        return id;
    }

    public int getClientId() {
        return clientId;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public double getBalance() {
        return balance;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void deposit(double amount) {
        balance += amount;
    }

    public void withdraw(double amount) {
        balance -= amount;
    }

    public abstract double calculateInterest();
}
