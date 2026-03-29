package com.bankapp.service;

import com.bankapp.config.DatabaseManager;
import com.bankapp.dao.AccountDao;
import com.bankapp.dao.TransactionDao;
import com.bankapp.enums.AccountType;
import com.bankapp.enums.TransactionType;
import com.bankapp.enums.WithdrawalChannel;
import com.bankapp.exception.BankException;
import com.bankapp.model.Account;
import com.bankapp.model.FixedDepositAccount;
import com.bankapp.model.Transaction;
import com.bankapp.util.BankConstants;

import java.sql.Connection;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class AccountService {
    private static final DecimalFormat MONEY = new DecimalFormat("#,##0.00");
    private static final Set<String> DEPOSIT_CHANNELS = Set.of("MPESA", "COUNTER", "ONLINE");

    private final AccountDao accountDao;
    private final TransactionDao transactionDao;

    public AccountService(AccountDao accountDao, TransactionDao transactionDao) {
        this.accountDao = accountDao;
        this.transactionDao = transactionDao;
    }

    public int openAccount(int clientId, AccountType accountType) throws BankException {
        if (clientId <= 0) {
            throw new BankException("A valid client id is required.");
        }
        if (accountType == null) {
            throw new BankException("Account type is required.");
        }

        try (Connection connection = DatabaseManager.getConnection()) {
            connection.setAutoCommit(false);
            try {
                if (accountDao.countByClient(connection, clientId) >= 3) {
                    throw new BankException("A client can only operate up to 3 accounts.");
                }
                if (accountDao.findByClientAndType(connection, clientId, accountType) != null) {
                    throw new BankException("Client already has a " + accountType + " account.");
                }

                int accountId = accountDao.createAccount(
                    connection,
                    clientId,
                    accountType,
                    accountType == AccountType.FIXED_DEPOSIT
                        ? LocalDate.now().plusMonths(BankConstants.FIXED_DEPOSIT_PERIOD_MONTHS)
                        : null
                );
                connection.commit();
                return accountId;
            } catch (BankException e) {
                rollbackQuietly(connection);
                throw e;
            } catch (SQLException e) {
                rollbackQuietly(connection);
                throw new BankException("Could not open account: " + e.getMessage());
            }
        } catch (SQLException e) {
            throw new BankException("Could not open account: " + e.getMessage());
        }
    }

    public void deposit(int accountId, double amount, String channel) throws BankException {
        double normalizedAmount = validatePositiveAmount(amount);
        String normalizedChannel = normalizeDepositChannel(channel);

        try (Connection connection = DatabaseManager.getConnection()) {
            connection.setAutoCommit(false);
            try {
                Account account = accountDao.findById(connection, accountId);
                if (account == null) {
                    throw new BankException("Account not found.");
                }

                account.deposit(normalizedAmount);
                accountDao.updateBalance(connection, accountId, account.getBalance());

                transactionDao.create(
                    connection,
                    accountId,
                    account.getClientId(),
                    TransactionType.DEPOSIT,
                    normalizedAmount,
                    normalizedChannel,
                    "Deposit",
                    buildReference("DEP")
                );

                connection.commit();
            } catch (BankException e) {
                rollbackQuietly(connection);
                throw e;
            } catch (SQLException e) {
                rollbackQuietly(connection);
                throw new BankException("Deposit failed: " + e.getMessage());
            }
        } catch (SQLException e) {
            throw new BankException("Deposit failed: " + e.getMessage());
        }
    }

    public void withdraw(int accountId, double amount, WithdrawalChannel channel) throws BankException {
        double normalizedAmount = validatePositiveAmount(amount);
        if (channel == null) {
            throw new BankException("Withdrawal channel is required.");
        }

        if (normalizedAmount > BankConstants.COUNTER_WITHDRAWAL_MIN_FOR_LARGE && channel != WithdrawalChannel.COUNTER) {
            throw new BankException("Withdrawals above 50,000 must be done over the counter.");
        }

        try (Connection connection = DatabaseManager.getConnection()) {
            connection.setAutoCommit(false);
            try {
                Account account = accountDao.findById(connection, accountId);
                if (account == null) {
                    throw new BankException("Account not found.");
                }

                if (channel == WithdrawalChannel.ATM || channel == WithdrawalChannel.MPESA) {
                    double alreadyWithdrawn = transactionDao.sumDailyWithdrawalsByClientAndChannel(
                        connection, account.getClientId(), channel.name(), LocalDate.now());
                    if (alreadyWithdrawn + normalizedAmount > BankConstants.DIGITAL_WITHDRAWAL_DAILY_CAP) {
                        throw new BankException(channel + " withdrawals are capped at 20,000 per day.");
                    }
                }

                double totalDebit = normalizedAmount + BankConstants.WITHDRAWAL_CHARGE;
                if (account.getBalance() < totalDebit) {
                    throw new BankException("Insufficient funds. Required: " + MONEY.format(totalDebit));
                }

                account.withdraw(totalDebit);
                accountDao.updateBalance(connection, accountId, account.getBalance());

                String reference = buildReference("WDR");
                transactionDao.create(
                    connection,
                    accountId,
                    account.getClientId(),
                    TransactionType.WITHDRAWAL,
                    normalizedAmount,
                    channel.name(),
                    "Withdrawal",
                    reference
                );
                transactionDao.create(
                    connection,
                    accountId,
                    account.getClientId(),
                    TransactionType.CHARGE,
                    BankConstants.WITHDRAWAL_CHARGE,
                    channel.name(),
                    "Common withdrawal charge",
                    reference + "-CHG"
                );

                connection.commit();
            } catch (BankException e) {
                rollbackQuietly(connection);
                throw e;
            } catch (SQLException e) {
                rollbackQuietly(connection);
                throw new BankException("Withdrawal failed: " + e.getMessage());
            }
        } catch (SQLException e) {
            throw new BankException("Withdrawal failed: " + e.getMessage());
        }
    }

    public void transfer(int fromAccountId, int toAccountId, double amount) throws BankException {
        double normalizedAmount = validatePositiveAmount(amount);
        if (fromAccountId == toAccountId) {
            throw new BankException("Source and destination accounts must be different.");
        }

        try (Connection connection = DatabaseManager.getConnection()) {
            connection.setAutoCommit(false);
            try {
                Account fromAccount = accountDao.findById(connection, fromAccountId);
                Account toAccount = accountDao.findById(connection, toAccountId);

                if (fromAccount == null || toAccount == null) {
                    throw new BankException("Both source and destination accounts must exist.");
                }

                if (fromAccount.getBalance() < normalizedAmount) {
                    throw new BankException("Insufficient funds in source account.");
                }

                fromAccount.withdraw(normalizedAmount);
                toAccount.deposit(normalizedAmount);
                accountDao.updateBalance(connection, fromAccountId, fromAccount.getBalance());
                accountDao.updateBalance(connection, toAccountId, toAccount.getBalance());

                String reference = buildReference("TRF");
                transactionDao.create(
                    connection,
                    fromAccountId,
                    fromAccount.getClientId(),
                    TransactionType.TRANSFER_OUT,
                    normalizedAmount,
                    "ONLINE",
                    "Transfer to account " + toAccountId,
                    reference
                );
                transactionDao.create(
                    connection,
                    toAccountId,
                    toAccount.getClientId(),
                    TransactionType.TRANSFER_IN,
                    normalizedAmount,
                    "ONLINE",
                    "Transfer from account " + fromAccountId,
                    reference
                );

                connection.commit();
            } catch (BankException e) {
                rollbackQuietly(connection);
                throw e;
            } catch (SQLException e) {
                rollbackQuietly(connection);
                throw new BankException("Transfer failed: " + e.getMessage());
            }
        } catch (SQLException e) {
            throw new BankException("Transfer failed: " + e.getMessage());
        }
    }

    public int processMonthlySavingsInterest() throws BankException {
        try {
            List<Account> savingsAccounts = accountDao.findByType(AccountType.SAVINGS);
            int posted = 0;
            for (Account account : savingsAccounts) {
                double interest = account.calculateInterest();
                if (interest <= 0) {
                    continue;
                }
                try (Connection connection = DatabaseManager.getConnection()) {
                    connection.setAutoCommit(false);
                    try {
                        Account reloaded = accountDao.findById(connection, account.getId());
                        if (reloaded == null) {
                            rollbackQuietly(connection);
                            continue;
                        }
                        reloaded.deposit(interest);
                        accountDao.updateBalance(connection, reloaded.getId(), reloaded.getBalance());
                        transactionDao.create(
                            connection,
                            reloaded.getId(),
                            reloaded.getClientId(),
                            TransactionType.INTEREST_CREDIT,
                            interest,
                            "SYSTEM",
                            "Monthly savings interest credit (3%)",
                            buildReference("INT-SAV")
                        );
                        connection.commit();
                        posted++;
                    } catch (SQLException e) {
                        rollbackQuietly(connection);
                        throw e;
                    }
                }
            }
            return posted;
        } catch (SQLException e) {
            throw new BankException("Could not process savings interest: " + e.getMessage());
        }
    }

    public int processFixedDepositMaturityInterest() throws BankException {
        try {
            List<FixedDepositAccount> fixedAccounts = accountDao.findMaturedFixedDepositAccountsWithoutInterest();
            int posted = 0;
            for (FixedDepositAccount account : fixedAccounts) {
                double interest = account.calculateInterest();
                if (interest <= 0) {
                    continue;
                }
                try (Connection connection = DatabaseManager.getConnection()) {
                    connection.setAutoCommit(false);
                    try {
                        Account reloaded = accountDao.findById(connection, account.getId());
                        if (!(reloaded instanceof FixedDepositAccount)) {
                            rollbackQuietly(connection);
                            continue;
                        }
                        reloaded.deposit(interest);
                        accountDao.updateBalance(connection, reloaded.getId(), reloaded.getBalance());
                        accountDao.markFixedInterestPaid(connection, reloaded.getId());
                        transactionDao.create(
                            connection,
                            reloaded.getId(),
                            reloaded.getClientId(),
                            TransactionType.INTEREST_CREDIT,
                            interest,
                            "SYSTEM",
                            "Fixed deposit maturity interest credit (8%)",
                            buildReference("INT-FXD")
                        );
                        connection.commit();
                        posted++;
                    } catch (SQLException e) {
                        rollbackQuietly(connection);
                        throw e;
                    }
                }
            }
            return posted;
        } catch (SQLException e) {
            throw new BankException("Could not process fixed deposit interest: " + e.getMessage());
        }
    }

    public String generateAccountStatement(int accountId) throws BankException {
        try {
            Account account = accountDao.findById(accountId);
            if (account == null) {
                throw new BankException("Account not found.");
            }

            List<Transaction> transactions = transactionDao.findByAccountId(accountId);
            StringBuilder builder = new StringBuilder();
            builder.append("ACCOUNT STATEMENT\n");
            builder.append("Account ID: ").append(account.getId()).append("\n");
            builder.append("Client ID: ").append(account.getClientId()).append("\n");
            builder.append("Type: ").append(account.getAccountType()).append("\n");
            builder.append("Current Balance: ").append(MONEY.format(account.getBalance())).append("\n");
            builder.append("Transactions:\n");

            if (transactions.isEmpty()) {
                builder.append("  No transactions found.\n");
            } else {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                for (Transaction tx : transactions) {
                    builder.append(String.format(Locale.US,
                        "  %s | %-13s | %10.2f | %-7s | %s | Ref:%s%n",
                        tx.getCreatedAt().format(formatter),
                        tx.getTransactionType(),
                        tx.getAmount(),
                        tx.getChannel() == null ? "-" : tx.getChannel(),
                        tx.getDescription() == null ? "" : tx.getDescription(),
                        tx.getReference() == null ? "-" : tx.getReference()));
                }
            }

            return builder.toString();
        } catch (SQLException e) {
            throw new BankException("Could not generate account statement: " + e.getMessage());
        }
    }

    public List<Account> getAccountsForClient(int clientId) throws BankException {
        try {
            return accountDao.findByClient(clientId);
        } catch (SQLException e) {
            throw new BankException("Could not fetch client accounts: " + e.getMessage());
        }
    }

    public Account getAccount(int accountId) throws BankException {
        try {
            Account account = accountDao.findById(accountId);
            if (account == null) {
                throw new BankException("Account not found.");
            }
            return account;
        } catch (SQLException e) {
            throw new BankException("Could not fetch account: " + e.getMessage());
        }
    }

    private static double validatePositiveAmount(double amount) throws BankException {
        if (!Double.isFinite(amount) || amount <= 0) {
            throw new BankException("Amount must be greater than zero.");
        }
        return amount;
    }

    private static String normalizeDepositChannel(String channel) throws BankException {
        if (channel == null || channel.isBlank()) {
            throw new BankException("Deposit channel is required.");
        }
        String normalized = channel.trim().toUpperCase(Locale.ROOT);
        if (!DEPOSIT_CHANNELS.contains(normalized)) {
            throw new BankException("Invalid deposit channel.");
        }
        return normalized;
    }

    private static void rollbackQuietly(Connection connection) {
        try {
            connection.rollback();
        } catch (SQLException ignored) {
            // Intentionally ignored: original failure is more relevant.
        }
    }

    private static String buildReference(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }
}
