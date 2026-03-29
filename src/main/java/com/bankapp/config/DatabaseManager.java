package com.bankapp.config;

import com.bankapp.enums.AccountType;
import com.bankapp.enums.Role;
import com.bankapp.util.DateTimeUtil;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;

public final class DatabaseManager {
    private static final String DEFAULT_DB_URL = "jdbc:sqlite:bank.db";
    private static final String DB_URL = resolveDbUrl();

    private DatabaseManager() {
    }

    public static Connection getConnection() throws SQLException {
        Connection connection = DriverManager.getConnection(DB_URL);
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
        }
        return connection;
    }

    private static String resolveDbUrl() {
        String envUrl = System.getenv("BANK_DB_URL");
        if (envUrl != null && !envUrl.isBlank()) {
            return envUrl.trim();
        }

        String envPath = System.getenv("BANK_DB_PATH");
        if (envPath != null && !envPath.isBlank()) {
            return "jdbc:sqlite:" + envPath.trim();
        }

        String propUrl = System.getProperty("bank.db.url");
        if (propUrl != null && !propUrl.isBlank()) {
            return propUrl.trim();
        }

        String propPath = System.getProperty("bank.db.path");
        if (propPath != null && !propPath.isBlank()) {
            return "jdbc:sqlite:" + propPath.trim();
        }

        return DEFAULT_DB_URL;
    }

    public static void initializeDatabase() throws SQLException {
        try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("""
                CREATE TABLE IF NOT EXISTS clients (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    full_name TEXT NOT NULL
                )
                """);

            statement.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    username TEXT UNIQUE NOT NULL,
                    password TEXT NOT NULL,
                    full_name TEXT NOT NULL,
                    role TEXT NOT NULL,
                    client_id INTEGER,
                    FOREIGN KEY (client_id) REFERENCES clients(id)
                )
                """);

            statement.execute("""
                CREATE TABLE IF NOT EXISTS accounts (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    client_id INTEGER NOT NULL,
                    type TEXT NOT NULL,
                    balance REAL NOT NULL DEFAULT 0,
                    created_at TEXT NOT NULL,
                    maturity_date TEXT,
                    fixed_interest_paid INTEGER NOT NULL DEFAULT 0,
                    UNIQUE(client_id, type),
                    FOREIGN KEY (client_id) REFERENCES clients(id)
                )
                """);

            statement.execute("""
                CREATE TABLE IF NOT EXISTS transactions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    account_id INTEGER NOT NULL,
                    client_id INTEGER NOT NULL,
                    type TEXT NOT NULL,
                    amount REAL NOT NULL,
                    channel TEXT,
                    description TEXT,
                    reference_code TEXT,
                    created_at TEXT NOT NULL,
                    FOREIGN KEY (account_id) REFERENCES accounts(id),
                    FOREIGN KEY (client_id) REFERENCES clients(id)
                )
                """);

            statement.execute("""
                CREATE TABLE IF NOT EXISTS loans (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    client_id INTEGER NOT NULL,
                    account_id INTEGER NOT NULL,
                    requested_amount REAL NOT NULL,
                    outstanding_balance REAL NOT NULL,
                    status TEXT NOT NULL,
                    is_top_up INTEGER NOT NULL DEFAULT 0,
                    top_up_charge REAL,
                    previous_loan_id INTEGER,
                    approved_by INTEGER,
                    remarks TEXT,
                    created_at TEXT NOT NULL,
                    updated_at TEXT NOT NULL,
                    FOREIGN KEY (client_id) REFERENCES clients(id),
                    FOREIGN KEY (account_id) REFERENCES accounts(id),
                    FOREIGN KEY (approved_by) REFERENCES users(id),
                    FOREIGN KEY (previous_loan_id) REFERENCES loans(id)
                )
                """);

            statement.execute("""
                CREATE TABLE IF NOT EXISTS loan_payments (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    loan_id INTEGER NOT NULL,
                    account_id INTEGER NOT NULL,
                    amount REAL NOT NULL,
                    note TEXT,
                    created_at TEXT NOT NULL,
                    FOREIGN KEY (loan_id) REFERENCES loans(id),
                    FOREIGN KEY (account_id) REFERENCES accounts(id)
                )
                """);
        }

        seedDataIfRequired();
    }

    private static void seedDataIfRequired() throws SQLException {
        try (Connection connection = getConnection();
             PreparedStatement countUsers = connection.prepareStatement("SELECT COUNT(*) FROM users");
             ResultSet rs = countUsers.executeQuery()) {
            if (rs.next() && rs.getInt(1) > 0) {
                return;
            }
        }

        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false);

            int aliceClientId = insertClient(connection, "Alice Njeri");
            int bobClientId = insertClient(connection, "Bob Otieno");

            insertUser(connection, "alice", "client123", "Alice Njeri", Role.CLIENT, aliceClientId);
            insertUser(connection, "bob", "client123", "Bob Otieno", Role.CLIENT, bobClientId);
            insertUser(connection, "manager", "manager123", "Main Manager", Role.MANAGER, null);
            insertUser(connection, "operator", "operator123", "Main Operator", Role.OPERATOR, null);

            int aliceSavings = insertAccount(connection, aliceClientId, AccountType.SAVINGS,
                15_000.0, LocalDateTime.now().minusMonths(2), null);
            insertAccount(connection, aliceClientId, AccountType.CURRENT,
                22_000.0, LocalDateTime.now().minusMonths(1), null);

            insertAccount(connection, bobClientId, AccountType.SAVINGS,
                8_500.0, LocalDateTime.now().minusMonths(3), null);
            insertAccount(connection, bobClientId, AccountType.FIXED_DEPOSIT,
                50_000.0, LocalDateTime.now().minusMonths(11),
                LocalDate.now().plusMonths(1));

            insertTransaction(connection, aliceSavings, aliceClientId, "DEPOSIT", 15_000.0,
                "MPESA", "Initial seed deposit", "SEED-DEP-ALICE");

            connection.commit();
        }
    }

    private static int insertClient(Connection connection, String fullName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
            "INSERT INTO clients(full_name) VALUES (?)", Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, fullName);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        throw new SQLException("Failed to create client.");
    }

    private static int insertUser(Connection connection, String username, String password, String fullName,
                                  Role role, Integer clientId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
            "INSERT INTO users(username,password,full_name,role,client_id) VALUES (?,?,?,?,?)",
            Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, username);
            statement.setString(2, password);
            statement.setString(3, fullName);
            statement.setString(4, role.name());
            if (clientId == null) {
                statement.setNull(5, java.sql.Types.INTEGER);
            } else {
                statement.setInt(5, clientId);
            }
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        throw new SQLException("Failed to create user.");
    }

    private static int insertAccount(Connection connection, int clientId, AccountType accountType,
                                     double balance, LocalDateTime createdAt, LocalDate maturityDate)
        throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
            "INSERT INTO accounts(client_id,type,balance,created_at,maturity_date,fixed_interest_paid) VALUES (?,?,?,?,?,?)",
            Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, clientId);
            statement.setString(2, accountType.name());
            statement.setDouble(3, balance);
            statement.setString(4, DateTimeUtil.formatDateTime(createdAt));
            statement.setString(5, DateTimeUtil.formatDate(maturityDate));
            statement.setInt(6, 0);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        throw new SQLException("Failed to create account.");
    }

    private static void insertTransaction(Connection connection, int accountId, int clientId,
                                          String type, double amount, String channel,
                                          String description, String referenceCode)
        throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
            "INSERT INTO transactions(account_id,client_id,type,amount,channel,description,reference_code,created_at) VALUES (?,?,?,?,?,?,?,?)")) {
            statement.setInt(1, accountId);
            statement.setInt(2, clientId);
            statement.setString(3, type);
            statement.setDouble(4, amount);
            statement.setString(5, channel);
            statement.setString(6, description);
            statement.setString(7, referenceCode);
            statement.setString(8, DateTimeUtil.formatDateTime(LocalDateTime.now()));
            statement.executeUpdate();
        }
    }
}
