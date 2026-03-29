package com.bankapp.dao;

import com.bankapp.config.DatabaseManager;
import com.bankapp.enums.TransactionType;
import com.bankapp.model.Transaction;
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

public class TransactionDao {
    public int create(Connection connection, int accountId, int clientId, TransactionType transactionType,
                      double amount, String channel, String description, String referenceCode)
        throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
            "INSERT INTO transactions(account_id,client_id,type,amount,channel,description,reference_code,created_at) VALUES (?,?,?,?,?,?,?,?)",
            Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, accountId);
            statement.setInt(2, clientId);
            statement.setString(3, transactionType.name());
            statement.setDouble(4, amount);
            statement.setString(5, channel);
            statement.setString(6, description);
            statement.setString(7, referenceCode);
            statement.setString(8, DateTimeUtil.formatDateTime(LocalDateTime.now()));
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        throw new SQLException("Could not create transaction.");
    }

    public List<Transaction> findByAccountId(int accountId) throws SQLException {
        List<Transaction> transactions = new ArrayList<>();
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "SELECT id, account_id, client_id, type, amount, channel, description, reference_code, created_at FROM transactions WHERE account_id = ? ORDER BY id DESC")) {
            statement.setInt(1, accountId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    transactions.add(map(rs));
                }
            }
        }
        return transactions;
    }

    public double sumDailyWithdrawalsByClientAndChannel(int clientId, String channel, LocalDate date)
        throws SQLException {
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                 """
                 SELECT COALESCE(SUM(amount), 0)
                 FROM transactions
                 WHERE client_id = ?
                 AND type = ?
                 AND channel = ?
                 AND date(created_at) = date(?)
                 """)) {
            statement.setInt(1, clientId);
            statement.setString(2, TransactionType.WITHDRAWAL.name());
            statement.setString(3, channel);
            statement.setString(4, DateTimeUtil.formatDate(date));
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble(1);
                }
            }
        }
        return 0.0;
    }

    private Transaction map(ResultSet rs) throws SQLException {
        return new Transaction(
            rs.getInt("id"),
            rs.getInt("account_id"),
            rs.getInt("client_id"),
            TransactionType.valueOf(rs.getString("type")),
            rs.getDouble("amount"),
            rs.getString("channel"),
            rs.getString("description"),
            rs.getString("reference_code"),
            DateTimeUtil.parseDateTime(rs.getString("created_at"))
        );
    }
}
