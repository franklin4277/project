package com.bankapp.dao;

import com.bankapp.config.DatabaseManager;
import com.bankapp.enums.AccountType;
import com.bankapp.model.Account;
import com.bankapp.model.FixedDepositAccount;
import com.bankapp.util.AccountFactory;
import com.bankapp.util.DateTimeUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AccountDao {
    public int createAccount(Connection connection, int clientId, AccountType accountType, LocalDate maturityDate)
        throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
            "INSERT INTO accounts(client_id,type,balance,created_at,maturity_date,fixed_interest_paid) VALUES (?,?,?,?,?,?)",
            Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, clientId);
            statement.setString(2, accountType.name());
            statement.setDouble(3, 0.0);
            statement.setString(4, DateTimeUtil.formatDateTime(LocalDateTime.now()));
            statement.setString(5, DateTimeUtil.formatDate(maturityDate));
            statement.setInt(6, 0);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        throw new SQLException("Could not create account.");
    }

    public Account findById(int accountId) throws SQLException {
        try (Connection connection = DatabaseManager.getConnection()) {
            return findById(connection, accountId);
        }
    }

    public Account findById(Connection connection, int accountId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
            "SELECT id, client_id, type, balance, created_at, maturity_date FROM accounts WHERE id = ?")) {
            statement.setInt(1, accountId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return map(rs);
                }
            }
        }
        return null;
    }

    public Account findByClientAndType(int clientId, AccountType accountType) throws SQLException {
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "SELECT id, client_id, type, balance, created_at, maturity_date FROM accounts WHERE client_id = ? AND type = ?")) {
            statement.setInt(1, clientId);
            statement.setString(2, accountType.name());
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return map(rs);
                }
            }
        }
        return null;
    }

    public int countByClient(int clientId) throws SQLException {
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "SELECT COUNT(*) FROM accounts WHERE client_id = ?")) {
            statement.setInt(1, clientId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    public List<Account> findByClient(int clientId) throws SQLException {
        List<Account> accounts = new ArrayList<>();
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "SELECT id, client_id, type, balance, created_at, maturity_date FROM accounts WHERE client_id = ? ORDER BY id")) {
            statement.setInt(1, clientId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    accounts.add(map(rs));
                }
            }
        }
        return accounts;
    }

    public List<Account> findByType(AccountType accountType) throws SQLException {
        List<Account> accounts = new ArrayList<>();
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "SELECT id, client_id, type, balance, created_at, maturity_date FROM accounts WHERE type = ? ORDER BY id")) {
            statement.setString(1, accountType.name());
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    accounts.add(map(rs));
                }
            }
        }
        return accounts;
    }

    public void updateBalance(Connection connection, int accountId, double newBalance) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
            "UPDATE accounts SET balance = ? WHERE id = ?")) {
            statement.setDouble(1, newBalance);
            statement.setInt(2, accountId);
            statement.executeUpdate();
        }
    }

    public List<FixedDepositAccount> findMaturedFixedDepositAccountsWithoutInterest() throws SQLException {
        List<FixedDepositAccount> accounts = new ArrayList<>();
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                 """
                 SELECT id, client_id, type, balance, created_at, maturity_date
                 FROM accounts
                 WHERE type = ?
                 AND fixed_interest_paid = 0
                 AND maturity_date IS NOT NULL
                 AND date(maturity_date) <= date('now')
                 """)) {
            statement.setString(1, AccountType.FIXED_DEPOSIT.name());
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    Account account = map(rs);
                    if (account instanceof FixedDepositAccount fixedDepositAccount) {
                        accounts.add(fixedDepositAccount);
                    }
                }
            }
        }
        return accounts;
    }

    public void markFixedInterestPaid(Connection connection, int accountId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
            "UPDATE accounts SET fixed_interest_paid = 1 WHERE id = ?")) {
            statement.setInt(1, accountId);
            statement.executeUpdate();
        }
    }

    private Account map(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        int clientId = rs.getInt("client_id");
        AccountType accountType = AccountType.valueOf(rs.getString("type"));
        double balance = rs.getDouble("balance");
        LocalDateTime createdAt = DateTimeUtil.parseDateTime(rs.getString("created_at"));
        LocalDate maturityDate = DateTimeUtil.parseDate(rs.getString("maturity_date"));
        return AccountFactory.createAccount(id, clientId, accountType, balance, createdAt, maturityDate);
    }
}
