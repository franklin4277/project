package com.bankapp.dao;

import com.bankapp.config.DatabaseManager;
import com.bankapp.enums.LoanStatus;
import com.bankapp.model.Loan;
import com.bankapp.model.LoanPayment;
import com.bankapp.util.DateTimeUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class LoanDao {
    public int createLoan(Connection connection, int clientId, int accountId, double requestedAmount,
                          double outstandingBalance, LoanStatus status, boolean topUp,
                          Double topUpCharge, Integer previousLoanId, String remarks)
        throws SQLException {
        String now = DateTimeUtil.formatDateTime(LocalDateTime.now());
        try (PreparedStatement statement = connection.prepareStatement(
            """
            INSERT INTO loans(client_id,account_id,requested_amount,outstanding_balance,status,is_top_up,top_up_charge,
                              previous_loan_id,approved_by,remarks,created_at,updated_at)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?)
            """,
            Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, clientId);
            statement.setInt(2, accountId);
            statement.setDouble(3, requestedAmount);
            statement.setDouble(4, outstandingBalance);
            statement.setString(5, status.name());
            statement.setInt(6, topUp ? 1 : 0);
            if (topUpCharge == null) {
                statement.setNull(7, java.sql.Types.REAL);
            } else {
                statement.setDouble(7, topUpCharge);
            }
            if (previousLoanId == null) {
                statement.setNull(8, java.sql.Types.INTEGER);
            } else {
                statement.setInt(8, previousLoanId);
            }
            statement.setNull(9, java.sql.Types.INTEGER);
            statement.setString(10, remarks);
            statement.setString(11, now);
            statement.setString(12, now);
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        throw new SQLException("Could not create loan.");
    }

    public Loan findById(int loanId) throws SQLException {
        try (Connection connection = DatabaseManager.getConnection()) {
            return findById(connection, loanId);
        }
    }

    public Loan findById(Connection connection, int loanId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
            "SELECT * FROM loans WHERE id = ?")) {
            statement.setInt(1, loanId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return map(rs);
                }
            }
        }
        return null;
    }

    public Loan findOpenLoanByClient(int clientId) throws SQLException {
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                 """
                 SELECT * FROM loans
                 WHERE client_id = ?
                 AND status IN (?, ?)
                 ORDER BY id DESC
                 LIMIT 1
                 """)) {
            statement.setInt(1, clientId);
            statement.setString(2, LoanStatus.PENDING.name());
            statement.setString(3, LoanStatus.ACTIVE.name());
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return map(rs);
                }
            }
        }
        return null;
    }

    public List<Loan> findPendingLoans() throws SQLException {
        List<Loan> loans = new ArrayList<>();
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "SELECT * FROM loans WHERE status = ? ORDER BY id")) {
            statement.setString(1, LoanStatus.PENDING.name());
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    loans.add(map(rs));
                }
            }
        }
        return loans;
    }

    public List<Loan> findByClient(int clientId) throws SQLException {
        List<Loan> loans = new ArrayList<>();
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "SELECT * FROM loans WHERE client_id = ? ORDER BY id DESC")) {
            statement.setInt(1, clientId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    loans.add(map(rs));
                }
            }
        }
        return loans;
    }

    public void updateStatus(Connection connection, int loanId, LoanStatus status, Integer managerId, String remarks)
        throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
            "UPDATE loans SET status = ?, approved_by = ?, remarks = ?, updated_at = ? WHERE id = ?")) {
            statement.setString(1, status.name());
            if (managerId == null) {
                statement.setNull(2, java.sql.Types.INTEGER);
            } else {
                statement.setInt(2, managerId);
            }
            statement.setString(3, remarks);
            statement.setString(4, DateTimeUtil.formatDateTime(LocalDateTime.now()));
            statement.setInt(5, loanId);
            statement.executeUpdate();
        }
    }

    public void updateBalanceAndStatus(Connection connection, int loanId, double balance, LoanStatus status,
                                       String remarks) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
            "UPDATE loans SET outstanding_balance = ?, status = ?, remarks = ?, updated_at = ? WHERE id = ?")) {
            statement.setDouble(1, balance);
            statement.setString(2, status.name());
            statement.setString(3, remarks);
            statement.setString(4, DateTimeUtil.formatDateTime(LocalDateTime.now()));
            statement.setInt(5, loanId);
            statement.executeUpdate();
        }
    }

    public void updateBalance(Connection connection, int loanId, double balance, String remarks) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
            "UPDATE loans SET outstanding_balance = ?, remarks = ?, updated_at = ? WHERE id = ?")) {
            statement.setDouble(1, balance);
            statement.setString(2, remarks);
            statement.setString(3, DateTimeUtil.formatDateTime(LocalDateTime.now()));
            statement.setInt(4, loanId);
            statement.executeUpdate();
        }
    }

    public int addPayment(Connection connection, int loanId, int accountId, double amount, String note)
        throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
            "INSERT INTO loan_payments(loan_id,account_id,amount,note,created_at) VALUES (?,?,?,?,?)",
            Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, loanId);
            statement.setInt(2, accountId);
            statement.setDouble(3, amount);
            statement.setString(4, note);
            statement.setString(5, DateTimeUtil.formatDateTime(LocalDateTime.now()));
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        throw new SQLException("Could not add loan payment.");
    }

    public List<LoanPayment> findPaymentsByLoan(int loanId) throws SQLException {
        List<LoanPayment> payments = new ArrayList<>();
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "SELECT id, loan_id, account_id, amount, note, created_at FROM loan_payments WHERE loan_id = ? ORDER BY id")) {
            statement.setInt(1, loanId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    payments.add(new LoanPayment(
                        rs.getInt("id"),
                        rs.getInt("loan_id"),
                        rs.getInt("account_id"),
                        rs.getDouble("amount"),
                        rs.getString("note"),
                        DateTimeUtil.parseDateTime(rs.getString("created_at"))
                    ));
                }
            }
        }
        return payments;
    }

    private Loan map(ResultSet rs) throws SQLException {
        return new Loan(
            rs.getInt("id"),
            rs.getInt("client_id"),
            rs.getInt("account_id"),
            rs.getDouble("requested_amount"),
            rs.getDouble("outstanding_balance"),
            LoanStatus.valueOf(rs.getString("status")),
            rs.getInt("is_top_up") == 1,
            (Double) rs.getObject("top_up_charge"),
            (Integer) rs.getObject("previous_loan_id"),
            DateTimeUtil.parseDateTime(rs.getString("created_at"))
        );
    }
}
